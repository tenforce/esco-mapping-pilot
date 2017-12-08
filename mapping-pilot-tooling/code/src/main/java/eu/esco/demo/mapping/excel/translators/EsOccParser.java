package eu.esco.demo.mapping.excel.translators;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.springframework.core.io.FileSystemResource;

import com.google.common.base.Preconditions;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import eu.esco.demo.mapping.excel.ExcelParser;
import eu.esco.operations.excel.ExcelMapProcessor;
import eu.esco.operations.excel.ExcelProcessor;
import eu.tenforce.commons.sem.jena.JenaUtils;

public class EsOccParser extends ExcelParser {

	public static void main(String[] args) {
		Path mappingFolderPath = Paths.get("F:/Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/mapping/");
		Path excelLocation = Paths.get(mappingFolderPath.toString() + File.separator + "es_occ.xls");
		
		Runnable esOccParser = new EsOccParser(excelLocation);
		esOccParser.run();
	}
	
	public EsOccParser(Path excelLocation) {
		super(excelLocation.toString());
	}
	
	@Override
	public void run() {
		final Model model = JenaUtils.createMemoryModel();
		model.read("F:/Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/mapping/hosp-occupations.ttl", "TTL");
		
		File excelFile = new File(excelLocation);
		Preconditions.checkState(excelFile.exists(), "File '" + excelLocation + "' does not exist.");
		
		log.info("Processing file '{}'.", excelLocation);
		
		ExcelProcessor excelProcessor = new ExcelProcessor();
		HSSFSheet sheet = excelProcessor.getSheet(new FileSystemResource(excelFile), 0);
		
		excelProcessor.processRows(sheet,
				new String[] { "CODIGO",
				"CODIGO (ALTAS)", "OPERACION A REALIZAR", "OBSERVACIONES",
				"DENOMINACION PRINCIPAL", "DENOMINACION PRINCIPAL PROPUESTA",
				"NIV.PROFES. PROPUESTO", "INF.COM. PROPUESTA",
				"SEC.PR. PROPUESTO", "SEC.2 PROPUESTO", "SEC.3 PROPUESTO",
				"SEC.4 PROPUESTO", "MUJ.SUB", "MUJ.SUB PROPUESTO",
				"DEN.ALT.1 PROPUESTA", "DEN.ALT.2 PROPUESTA",
				"DEN.ALT.3  PROPUESTA" },
				new ExcelMapProcessor() {

			@Override
			// For each row
			public void process(int rowNumber, Map<String, String> rowMap) {
				// If notation is subgroup of existing notation
				if(rowMap.get("CODIGO").length() == 8 &&
						model.containsLiteral(
								model.wrapAsResource(Node.ANY),
								model.getProperty("http://www.w3.org/2004/02/skos/core#notation"),
								rowMap.get("CODIGO").substring(0, 4))) {
					// Add to model
					log.info("Found match '{}'.", rowMap.get("DENOMINACION PRINCIPAL"));
					addConcept(model, rowMap.get("CODIGO"), rowMap.get("DENOMINACION PRINCIPAL"));
				}
			}
		});
		
		saveModel2File(model, Paths.get(excelLocation + ".ttl"), "TTL");
		
	}
	
	/**
	 * Add concept to the occupation model
	 * 
	 * @param model
	 *            The model to add to
	 * @param identifier
	 *            The identifier of the concept
	 * @param label
	 *            The label of the concept
	 */
	private void addConcept(Model model, String identifier, String label) {
		// Create resource
		Resource conceptResource = model.createResource("http://pes.es/cno/Concept/Tree/iC." + identifier);
		
		// Add type concept
		model.add(conceptResource, RDF.type, model.createResource("http://www.w3.org/2004/02/skos/core#Concept"));
		// Add identifier
		model.add(conceptResource, model.createProperty("http://purl.org/dc/terms/identifier"), model.createLiteral("es CNO:" + identifier));
		// Add broader
		model.add(conceptResource, model.createProperty("http://www.w3.org/2004/02/skos/core#broader"), model.getResource("http://pes.es/cno/Concept/Tree/iC." + identifier.substring(0, 4)));
		// Add scheme
		model.add(conceptResource, model.createProperty("http://www.w3.org/2004/02/skos/core#inScheme"), model.getResource("http://pes.es/cno/ConceptScheme"));
		// Add notation
		model.add(conceptResource, model.createProperty("http://www.w3.org/2004/02/skos/core#notation"), model.createLiteral(identifier));
		// Add prefLabel
		model.add(conceptResource, model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel"), label,"es");
	}	
}
