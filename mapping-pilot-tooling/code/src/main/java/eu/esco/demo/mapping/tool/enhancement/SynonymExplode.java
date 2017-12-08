package eu.esco.demo.mapping.tool.enhancement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.tenforce.jena.JenaUtils;

public class SynonymExplode implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(SynonymExplode.class);

    private static String rootV1 = "F:/Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/pes-v1/";
	
	public static void main(String[] args) {
		new SynonymExplode(rootV1 + "fr/hosp-occupations.ttl").run();
	}
	
	private String modelPath;
	
	public SynonymExplode(String modelPath) {
		this.modelPath = modelPath;
	}
	
	public void run() {
		Model model = JenaUtils.create();
		model.read(modelPath, "TTL");
		
		List<Resource> resources = model.listResourcesWithProperty(model.createProperty(SKOS.PREF_LABEL.stringValue())).toList();
		
		int done = 1;
		for(Resource resource : resources) {
			String label = resource.getProperty(model.getProperty(SKOS.PREF_LABEL.stringValue())).getString();
			String[] parts = label.split(" ");
			
			List<String[]> synList = new ArrayList<>();
			for(String part : parts) {
				try {
					String[] synonyms = searchSynonyms(part);
					if(synonyms != null) {
						synList.add(synonyms);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			List<String> combinations = combine("", synList);
			for(String combination : combinations) {
				resource.addProperty(model.getProperty(SKOS.ALT_LABEL.stringValue()), combination, "fr");
				log.debug("Added value '{}' to '{}'",combination,resource.getURI());
			}
			log.info("Found synonyms for '{}' out of '{}'",done,resources.size());
			done++;
 		}
		try {
			model.write(new FileWriter(new File(modelPath + ".new")), "TTL");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String[] searchSynonyms(String part) throws IOException {
		if(part.equals("") || part.equals("/")) { return null; }
		
		BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("thes_fr-encoded.txt"), "UTF-8"));
		
		for(String line = br.readLine(); line != null; line = br.readLine()) {
			String[] lineParts = line.split(",");
			for(String linePart : lineParts) {
				if(part.equalsIgnoreCase(linePart)) { return lineParts; }
			}
		}
		return null;
	}

	private List<String> combine(String start, List<String[]> rest) {
		if(rest.isEmpty()) { // Nill case
			return new ArrayList<String>();
		}
		
		String[] synonyms = rest.remove(0);
		ArrayList<String> combinations = new ArrayList<String>();
		for(String synonym : synonyms) {
			if(start.equals("")) {
				combinations.add(synonym);
			} else {
				combinations.add(start + " " + synonym);
			}
		}
		
		ArrayList<String> newCombinations = new ArrayList<>();
		for(String combination : combinations) {
			newCombinations.addAll(combine(combination, rest));
		}
		
		combinations.addAll(newCombinations);
		
		return combinations;
	}
}
