package eu.esco.demo.mapping.tool.enhancement;

import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class ContextMatch {
	
	private String url;
	private String match;
	private double score;
	
	private final String MT = "http://sem.tenforce.com/vocabularies/mapping-pilot/";
	
	public ContextMatch(String url, String match, double score) {
		this.url = url;
		this.match = match;
		this.score = score;
	}
	
	public String getTriples() {
		return new StringBuilder()
			.append("<" + url + "> ")
			.append("<" + match + "> ")
			.append(score)
			.toString();
	}

	public void addTriplesToModel(Model model) {
		Resource mapping = model.createResource(MT + "mapping/" + UUID.randomUUID().toString());
		mapping.addProperty(RDF.type, model.createResource(MT + "ContextMapping"));
		mapping.addProperty(model.getProperty(MT + "mapsFrom"), url);
		mapping.addProperty(model.getProperty(MT + "mapsTo"), match);
		mapping.addLiteral(model.getProperty(MT + "contextScore"), score);
	}
}
