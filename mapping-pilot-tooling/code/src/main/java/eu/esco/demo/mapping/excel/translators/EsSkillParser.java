package eu.esco.demo.mapping.excel.translators;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.springframework.core.io.FileSystemResource;

import com.google.common.base.Preconditions;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import eu.esco.demo.mapping.excel.ExcelParser;
import eu.esco.operations.excel.ExcelMapProcessor;
import eu.esco.operations.excel.ExcelProcessor;
import eu.tenforce.commons.sem.jena.JenaUtils;

public class EsSkillParser extends ExcelParser {

	public static void main(String[] args) {
		Path mappingFolderPath = Paths.get("F:/Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/mapping/");
		Path excelLocation = Paths.get(mappingFolderPath.toString() + File.separator + "es_skills.xls");
		
		Runnable esSkillParser = new EsSkillParser(excelLocation);
		esSkillParser.run();
	}
	
	public EsSkillParser(Path excelLocation) {
		super(excelLocation.toString());
	}
	
	@Override
	public void run() {
		final Model model = JenaUtils.createMemoryModel();
		
		// Setup prefixes
		Map<String,String> prefixes = new HashMap<>();
		prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
		prefixes.put("ns2", "http://purl.org/dc/terms/");
		prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
		model.setNsPrefixes(prefixes);
		
		File excelFile = new File(excelLocation);
		Preconditions.checkState(excelFile.exists(), "File '" + excelLocation + "' does not exist.");
		
		log.info("Processing file '{}'.", excelLocation);
		
		ExcelProcessor excelProcessor = new ExcelProcessor();
		HSSFSheet sheet = excelProcessor.getSheet(new FileSystemResource(excelFile), 0);
		
		excelProcessor.processRows(sheet,
				new String[] { "Professional Family (sector branch)",
				"Qualification Code",
				"Qualification",
				"Skills Unit Code",
				"Skills Unit descrption"
				},
				new ExcelMapProcessor() {

			@Override
			// For each row
			public void process(int rowNumber, Map<String, String> rowMap) {
				// If part of HOSP
				if(rowMap.get("Professional Family (sector branch)").equals("HOT")) {
					// Add to model
					log.info("Found match '{}'.", rowMap.get("Skills Unit descrption"));
					addConcept(model, rowMap.get("Skills Unit Code"), rowMap.get("Skills Unit descrption"));
				}
			}
		});
		
		saveModel2File(model, Paths.get(excelLocation + ".ttl"), "TTL");
	}
	
	/**
	 * Add concept to the skill model
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
		Resource conceptResource = model.createResource("http://pes.es/cno/Concept/Tree/Skill/iC." + identifier);
		
		// Add type concept
		model.add(conceptResource, RDF.type, model.createResource("http://www.w3.org/2004/02/skos/core#Concept"));
		// Add identifier
		model.add(conceptResource, model.createProperty("http://purl.org/dc/terms/identifier"), model.createLiteral(identifier));
		// Add notation
		//model.add(conceptResource, model.createProperty("http://www.w3.org/2004/02/skos/core#notation"), model.createLiteral(identifier));
		// Add prefLabel
		model.add(conceptResource, model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel"), label,"es");
		// Add Skill type
		model.add(conceptResource, model.createProperty("http://purl.org/dc/elements/1.1/type"), "Skill");
	}

}
