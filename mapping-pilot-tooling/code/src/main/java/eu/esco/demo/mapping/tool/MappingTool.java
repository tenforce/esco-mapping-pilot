package eu.esco.demo.mapping.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openrdf.model.vocabulary.SKOS;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import eu.esco.demo.mapping.html.HtmlReport;
import eu.esco.demo.mapping.tool.model.MatchingResult;

public class MappingTool implements Runnable {

	private static void doMapping(String incomingFile, MappingConfiguration mappingConfiguration) throws IOException {

		String outgoingFile = incomingFile + "-mapping";

		System.out.println("Mapping => " + incomingFile + " " + outgoingFile + " " + mappingConfiguration);
		Model model = ModelFactory.createDefaultModel();
		model.read(new FileInputStream(incomingFile + ".ttl"), "http://ignore.me", "TTL");

		MappingInputProducer mappingInputProducer = new RdfMappingInputProducer(model, mappingConfiguration.getLanguage(),
				Arrays.asList(SKOS.PREF_LABEL.stringValue(), SKOS.ALT_LABEL.stringValue()));
		MatchingEngine matchingEngine = new MatchingEngine(mappingInputProducer, mappingConfiguration);

		List<MatchingResult> matchingResults = matchingEngine.call();

		MatchingOutputWriter writer = new MatchingOutputWriter();
		writer.writeJson(new File(outgoingFile + ".json"), matchingResults);
		writer.writeTriples(outgoingFile, matchingResults, "TTL", mappingConfiguration.getSearchType());
		writer.writeDump(new File(outgoingFile + ".txt"), matchingResults, "UTF-8");
		HtmlReport.generateHtmlReport(outgoingFile);
	}
	
	private List<MappingQuery> queries;
	
	public MappingTool(List<MappingQuery> queries) {
		this.queries = queries;
	}

	@Override
	public void run() {
//		String rootFolderV1 = "F:\\Program Files\\Dropbox\\UNI\\Tenforce\\workspace\\mapping-pilot\\data\\pes-v1\\";
//	    Object[][] mappings = { // CLEAN put all into config.xml
//	            {
//	                    rootFolderV1 + "cs/hosp-occupations",
//	                    MappingConfiguration.fromTemplate("cs", templateUri, "Occupation")
//	            },
//	            {
//	                    rootFolderV1 + "es/hosp-occupations",
//	                    MappingConfiguration.fromTemplate("es", templateUri, "Occupation")
//	            },
//	            {
//	                	rootFolderV1 + "es/hosp-skills",
//	                	MappingConfiguration.fromTemplate("es", templateUri, "Skill")
//	            },
//	            {
//	                    rootFolderV1 + "nl/hosp-occupations",
//	                    MappingConfiguration.fromTemplate("nl", templateUri, "Occupation")
//	            },
//	            {
//	                    rootFolderV1 + "nl/hosp-skills",
//	                    MappingConfiguration.fromTemplate("nl", templateUri, "Skill")
//	            }
//	            },
//	            {
//	                    rootFolderV1 + "fr/hosp-occupations",
//	                    MappingConfiguration.fromTemplate("fr", Config.solr_template, "Occupation")
//	            },
//	            {
//	                    rootFolderV1 + "fr/hosp-skills",
//	                    MappingConfiguration.fromTemplate("fr", Config.solr_template, "Skill")
//	            }
//
//
//	            {
//	                    rootFolder + "dutch/dutch-hospi-occ-small.ttl",
//	                    rootFolder + "dutch/dutch-hospi-occ-small-mapping",
//	                    new MappingConfiguration("nl", "http://tfvirt-esco-02-rdf.tenforce2.be:7001/esco-solr/data_nl_20150304_070839", "Occupation")
//	            }//,
//	            {
//	                    rootFolder + "dutch/dutch-hospi-occ.ttl",
//	                    rootFolder + "dutch/dutch-hospi-occ-mapping",
//	                    new MappingConfiguration("nl", "http://tfvirt-esco-02-rdf.tenforce2.be:7001/esco-solr/data_nl_20150304_070839", "Occupation")
//	            },
//	            {
//	                    rootFolder + "dutch/dutch-hospi-skill.ttl",
//	                    rootFolder + "dutch/dutch-hospi-skill-mapping",
//	                    new MappingConfiguration("nl", "http://tfvirt-esco-02-rdf.tenforce2.be:7001/esco-solr/data_nl_20150304_070839", "Skill")
//	            },
//	            {
//	                    rootFolder + "czech/hospi-occ-czech.ttl",
//	                    rootFolder + "czech/hospi-occ-czech-mapping",
//	                    new MappingConfiguration("cs", rootUri, "Occupation")
//	            }//,
//	            {
//	                    rootFolder + "spain/spain-hospi-occ.ttl",
//	                    rootFolder + "spain/spain-hospi-occ-mapping",
//	                    new MappingConfiguration("es", "http://tfvirt-esco-02-rdf.tenforce2.be:7001/esco-solr/data_es_20150304_070839", "Occupation")
//	            },
//	            {
//	                    rootFolder + "french/hospi-rome-fr.ttl",
//	                    rootFolder + "french/hospi-rome-fr-mapping",
//	                    new MappingConfiguration("fr", "http://tfvirt-esco-02-rdf.tenforce2.be:7001/esco-solr/data_fr_20150304_070839", "Occupation")
//	            },
//	            {
//	                    rootFolder + "french/hospi-rome-fr-skills.ttl",
//	                    rootFolder + "french/hospi-rome-fr-skills-mapping",
//	                    new MappingConfiguration("fr", "http://tfvirt-esco-02-rdf.tenforce2.be:7001/esco-solr/data_fr_20150304_070839", "Skill")
//	            }
//	    };

	    for (MappingQuery query : queries) {
	    	long start = System.currentTimeMillis();
	    	try {
	    		doMapping(query.file, query.configuration);
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    	System.out.println("timing = " + (System.currentTimeMillis() - start));
	    }
  }
}