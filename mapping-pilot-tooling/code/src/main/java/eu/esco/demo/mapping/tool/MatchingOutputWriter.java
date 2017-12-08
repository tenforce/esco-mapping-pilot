package eu.esco.demo.mapping.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.tenforce.jena.JenaUtils;
import eu.esco.demo.mapping.tool.model.MatchCandidate;
import eu.esco.demo.mapping.tool.model.MatchingResult;
import eu.esco.demo.mapping.tool.model.MatchingResultSummary;
import eu.esco.demo.mapping.tool.model.ModelConcept;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openrdf.model.vocabulary.SKOS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchingOutputWriter {
	
	public void writeJson(File outputFile, List<? extends MatchingResult> matchingResults) throws IOException {
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	    objectMapper.writeValue(outputFile, matchingResults);
	}
	
	public void writeTriples(String path, List<? extends MatchingResult> matchingResults, String lang, String searchType) throws IOException {
		Model model = JenaUtils.create();
		
		model.setNsPrefix(SKOS.PREFIX, SKOS.NAMESPACE);
		model.setNsPrefix("mt", "http://sem.tenforce.com/vocabularies/mapping-pilot/");
		model.setNsPrefix("mu", "http://mu.semte.ch/vocabularies/");
		
		String taxonomyPath = path.substring(0, StringUtils.lastIndexOf(path, "/")) + "/pes-taxonomy-" + searchType + ".ttl";
		
		Resource pesTax = readTaxonomy(model, taxonomyPath);
		
		taxonomyPath = path.substring(0, StringUtils.lastIndexOf(path,"/")) + "/esco-taxonomy-" + searchType + ".ttl";		
		
		Resource escoTax = readTaxonomy(model, taxonomyPath);
		
		for(MatchingResult mr : matchingResults) {
			Resource concept = buildConcept(model, mr, pesTax);
			// For each matching candidate
			for(MatchCandidate mc : mr.getMatchingCandidates()) {
				// Add concept
				Resource mcConcept = buildConcept(model, mc, escoTax);
				// Add match mapping
				Resource mapping = model.createResource("http://sem.tenforce.com/vocabularies/mapping-pilot/mapping/" + UUID.randomUUID().toString()); // TODO de echte url
				mapping.addProperty(RDF.type, model.createResource("http://sem.tenforce.com/vocabularies/mapping-pilot/Mapping"));
				mapping.addLiteral(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/score"), mc.getScore());
				mapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/maps"), concept);
				mapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/maps"), mcConcept);
				mapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/mapsFrom"), concept);
				mapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/mapsTo"), mcConcept);
				mapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/encodedUri"), mapping.getURI());
				
				Resource reverseMapping = model.createResource("http://sem.tenforce.com/vocabularies/mapping-pilot/mapping/" + UUID.randomUUID().toString());
				reverseMapping.addProperty(RDF.type, model.createResource("http://sem.tenforce.com/vocabularies/mapping-pilot/Mapping"));
				reverseMapping.addLiteral(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/score"), mc.getScore());
				reverseMapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/maps"), concept);
				reverseMapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/maps"), mcConcept);
				reverseMapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/mapsFrom"), mcConcept);
				reverseMapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/mapsTo"), concept);
				reverseMapping.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/encodedUri"), reverseMapping.getURI());
			}
		}
		
		// Add UUID's
		for(Resource res : model.listResourcesWithProperty(RDF.type).toList()) {
			res.addProperty(model.createProperty("http://mu.semte.ch/vocabularies/uuid"), UUID.randomUUID().toString());
		}
		
		// Add altLabels pipe seperated
		for(Resource res : model.listResourcesWithProperty(model.getProperty(SKOS.ALT_LABEL.stringValue())).toList()) {
			List<String> altLabels = new ArrayList<>();
			for(Statement s : res.listProperties(model.getProperty(SKOS.ALT_LABEL.stringValue())).toList()) {
				altLabels.add(s.getString());
			}
			res.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/altLabels"),
					StringUtils.join("|", altLabels));
		}

		model.write(new OutputStreamWriter(new FileOutputStream(path + "." + lang.toLowerCase()), "UTF-8"), lang);
	}
	
	private Resource readTaxonomy(Model model, String taxonomyPath) { 
		
		model.read(taxonomyPath, "TTL");
		
		List<Resource> topConceptList = model.listResourcesWithProperty(RDF.type, model.getResource(SKOS.CONCEPT_SCHEME.stringValue())).toList();
		
		return topConceptList.isEmpty() ? 
				model.createResource().addProperty(RDF.type, model.getResource(SKOS.CONCEPT_SCHEME.stringValue())) :
						topConceptList.get(0).addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/encodedUri"), topConceptList.get(0).getURI());
	}
	
	private Resource buildConcept(Model model, ModelConcept mc, Resource taxonomy) {
		Preconditions.checkArgument(model.containsResource(taxonomy));
		
		// Define resource
		Resource concept = model.createResource(mc.getUri());
		concept.addProperty(RDF.type, model.createResource(SKOS.CONCEPT.stringValue()));
		Property inScheme = model.createProperty(SKOS.IN_SCHEME.stringValue());
		concept.addProperty(inScheme, taxonomy);
		
		// Add labels
		concept.addProperty(model.createProperty(SKOS.PREF_LABEL.stringValue()), mc.getPrefLabel());
		Property altLabelProperty = model.createProperty(SKOS.ALT_LABEL.stringValue()); 
		for(String altLabel : mc.getAltLabel()) {
			concept.addProperty(altLabelProperty, altLabel);
		}
		
		// Add definition
		if(mc.hasDescription()) {
			concept.addProperty(model.getProperty(SKOS.DEFINITION.stringValue()), mc.getDescription());
		}
		
		// Add broader/topConceptOf
		if(mc.getBroader().isEmpty()) {
			concept.addProperty(model.createProperty(SKOS.TOP_CONCEPT_OF.stringValue()), taxonomy);
		} else {
			for(String broader : mc.getBroader()) {
				concept.addProperty(model.createProperty(SKOS.BROADER.stringValue()), model.getResource(broader));
			}
		}
		
		// Encoded Uri
		concept.addProperty(model.createProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/encodedUri"), concept.getURI());
		
		return concept;
	}
	
	public void writeDump(File outputFile, List<MatchingResult> matchingResults, String encoding) throws IOException {

		MatchingResultSummary matchingResultSummary = new MatchingResultSummary();
		matchingResultSummary.setMatchingResults(matchingResults);

		String output = "";
		output += matchingResultSummary.getMatchingQualityReport();
		output += "\n\n";

		for (MatchingResult matchingResult : matchingResults) {
			output += matchingResult;
		}

		FileUtils.write(outputFile, output, encoding);
	}

}
