package eu.esco.demo.mapping.tool.enhancement.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.tenforce.jena.JenaUtils;

public class ScoreBucketGenerator {
	
	public static void main(String[] args) throws IOException {
		Model m = JenaUtils.create();
		
		m.setNsPrefix("mt", "http://sem.tenforce.com/vocabularies/mapping-pilot/");
		
		final int NUM_BUCKETS = 50;
		final int MAX = 100;
		final int MIN = 0;
		
		int startScore = MIN;
		int bump = MAX / NUM_BUCKETS;
		for(int bucket = 0 ; bucket < NUM_BUCKETS ; bucket++) {
			Resource bucketRes = m.createResource("http://url/to/bucket/" + bucket);
			bucketRes.addProperty(RDF.type, m.getResource("http://sem.tenforce.com/vocabularies/mapping-pilot/ScoreBucket"));
			bucketRes.addLiteral(m.getProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/scoreStart"), startScore);
			bucketRes.addLiteral(m.getProperty("http://sem.tenforce.com/vocabularies/mapping-pilot/scoreEnd"), startScore + bump);
			startScore += bump;
		}
		
		m.write(new FileWriter(new File("src/main/resources/bucketGraph.ttl")), "TTL");
	}
	
}
