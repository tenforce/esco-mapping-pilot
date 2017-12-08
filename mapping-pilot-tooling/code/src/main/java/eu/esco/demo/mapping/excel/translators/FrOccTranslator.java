package eu.esco.demo.mapping.excel.translators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetImpl;

import eu.tenforce.commons.sem.jena.JenaUtils;

public class FrOccTranslator implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(FrOccTranslator.class);

	private final String originalFilePath;

	public static void main(String[] args) {
		FrOccTranslator t = new FrOccTranslator(
				"F:/Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/pes-v1/fr/hosp-occupations.ttl");
		t.run();
	}

	public FrOccTranslator(String originalFilePath) {
		this.originalFilePath = originalFilePath;
	}

	@Override
	public void run() {
		final Dataset ds = new DatasetImpl(JenaUtils.createMemoryModel());
		final Model model = JenaUtils.createMemoryModel();
		model.read(originalFilePath, "TTL");
		ds.addNamedModel("nen/uri", model);
		
		String constructQueryString =
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
						+ "CONSTRUCT {\n"
						+ "    ?SUB rdf:type skos:Concept .\n"
						+ "    ?SUB skos:prefLabel ?X .\n"
						+ "    ?SUB skos:broader ?TOP .\n"
						+ "    ?SUB skos:broadMatch ?ISCO"
						+ "} WHERE {\n"
						+ "    SELECT (IRI(concat(\"http://pes.fr/rome/Concept/Tree/Appellation/\",md5(concat(str(?X),str(?TOP))))) AS ?SUB) ?X ?A ?TOP ?ISCO WHERE {\n"
						+ "        ?TOP skos:prefLabel ?A .\n"
						+ "        ?TOP skos:altLabel ?X .\n"
						+ "        ?TOP skos:broadMatch ?ISCO"
						+ "    }"
						+ "}";
		
		Query constructQuery = QueryFactory.create(constructQueryString);
//		QueryExecution qexec = QueryExecutionFactory.create(constructQuery, model);
		QueryExecution qexec = QueryExecutionFactory.create(constructQuery, ds);
		
		Model constructModel = qexec.execConstruct();
		qexec.close();
		log.info("Constructed new model from '{}'", originalFilePath);


		model.removeAll(model.wrapAsResource(Node.ANY), model.getProperty("http://www.w3.org/2004/02/skos/core#altLabel"), model.wrapAsResource(Node.ANY));
		log.info("Removed from old model '{}'", originalFilePath);

		model.add(constructModel);
		log.info("Added constructed model to old model");
		
		saveModel2File(model, Paths.get("F:/Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/pes-v1/fr/hosp-occupations-translated.ttl"), "TTL");
	}

	/**
	 * Write a model to a file with a given language, on a given path
	 * 
	 * @param model
	 *            The model to write
	 * @param path
	 *            The path for the file
	 * @param lang
	 *            The language for the model to output
	 */
	protected void saveModel2File(Model model, Path path, String lang) {
		log.info("Writing file '{}'", path.toString());
		File output = path.toFile();

		if(!output.exists()) {
			try {
				output.createNewFile();
			} catch (IOException e) {
				log.error("Failed writing output file '{}'", path.toString());
			}
		}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(output);
			model.write(out, lang);
		} catch (FileNotFoundException ignore) {}
		finally {
			IOUtils.closeQuietly(out);
		}

	}

}
