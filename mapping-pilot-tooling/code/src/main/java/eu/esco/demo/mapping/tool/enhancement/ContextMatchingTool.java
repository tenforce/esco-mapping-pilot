package eu.esco.demo.mapping.tool.enhancement;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.tenforce.jena.JenaUtils;

import eu.esco.demo.mapping.main.Config;
import eu.esco.demo.mapping.tool.enhancement.util.ContextQuery;
import eu.esco.demo.mapping.tool.enhancement.util.SparseVector;

public class ContextMatchingTool implements Runnable {
	
	private final static Logger log = LoggerFactory.getLogger(ContextMatchingTool.class);
	
	private final String graphA, graphB;
	private final Path outputFile;
	
	public ContextMatchingTool(String graphA, String graphB, Path outputFile) {
		this.graphA = graphA;
		this.graphB = graphB;
		this.outputFile = outputFile;
	}
	
	public ContextMatchingTool(ContextQuery cq) {
		this.graphA = cq.graphA;
		this.graphB = cq.graphB;
		this.outputFile = cq.outputFile;
	}

	@Override
	public void run() {
		// OPEN VIRTUOSO CONNECTION
		VirtGraph graph = new VirtGraph("jdbc:virtuoso://" + Config.virtuoso + "/charset=UTF-8/","dba","dba");
		graph.setReadFromAllGraphs(true); // Why jena? WHY!?

		// SETUP VECTOR METADATA
		String query = 
				"PREFIX mt: <http://sem.tenforce.com/vocabularies/mapping-pilot/>\n"
						+ "SELECT DISTINCT ?context WHERE {\n"
						+ "  {\n"
						+ "    GRAPH <" + graphA + "> {\n"
						+ "      ?contextMatch mt:context ?context\n"
						+ "    }\n"
						+ "  } UNION {\n"
						+ "    GRAPH <" + graphB + "> {\n"
						+ "      ?contextMatch mt:context ?context\n"
						+ "    }\n"
						+ "  }\n"
						+ "}";
		VirtuosoQueryExecution qexec = new VirtuosoQueryExecution(query, graph);
		ResultSet rs = qexec.execSelect();
		
		Map<String, Integer> topicIndexMap = new HashMap<>();
		int index = 0;
		QuerySolution qs = null;
		do {
			qs = rs.next();
			topicIndexMap.put(qs.get("context").toString(), index++);
		} while(rs.hasNext());
		int vectorLength = topicIndexMap.size();
		log.info("Amount of unique topics: {}", vectorLength);

		// SETUP VECTORS
		Map<String, SparseVector> vectorMapA = setupVectors(vectorLength, topicIndexMap, graphA, graph);
		Map<String, SparseVector> vectorMapB = setupVectors(vectorLength, topicIndexMap, graphB, graph);
		
		// FOR EACH A MATCH FOR EACH B (CUTOFF AT 0.????)
		List<ContextMatch> matches = new ArrayList<>();
		double cutoff = Config.contextCutoff;
		log.info("Matching contexts...");
    index = 0;
    int accepted = 0;
    int rejected = 0;
    int max = vectorMapA.keySet().size();
		for(String url : vectorMapA.keySet()) {
      if(index % 1000 == 0){
          log.info("Matching {}/{} {} ...", index, max, url);
      }
      index++;

			for(String possibleMatch : vectorMapB.keySet()) {
				double score = vectorMapA.get(url).dot(vectorMapB.get(possibleMatch));
				if(score >= cutoff) {
					matches.add(new ContextMatch(url, possibleMatch, score));
          accepted++;
				}else {
          rejected++;
        }
			}
		}
		log.info("Accepted {} matches, rejected {} matches as they were below a score of {}", accepted, rejected, cutoff);
		
		// WRITE RESULTS TO TTL
		log.info("Writing results...");
		
		Map<String, String> prefixes = new HashMap<>();
		
    index = 0;
		// PREFIXES
		try {
        max = matches.size();
        Model model = JenaUtils.create();
        for(ContextMatch cm : matches) {
            cm.addTriplesToModel(model);
            if(index % 100000 == 0){
                log.info("Wrote {}/{} items", index, max);
                FileWriter fw = new FileWriter(outputFile.toFile(),index>0);
                model.write(fw, "TTL");
                model.removeAll();
            }
            index++;
        }

        FileWriter fw = new FileWriter(outputFile.toFile(),true);
        model.write(fw, "TTL");
        model.removeAll();
		} catch(IOException e) {
        e.printStackTrace();
		}
		
		// DONE
		log.info("Finished");
	}
	
	private Map<String, SparseVector> setupVectors(int N, Map<String, Integer> topicIndexMap, String graphName, VirtGraph virtuosoConnection) {
		log.info("Building Vectors for '{}'", graphName);
		String query = new StringBuilder() 
				.append("PREFIX mt: <http://sem.tenforce.com/vocabularies/mapping-pilot/>\n")
					.append("SELECT ?concept ?context ?score WHERE {\n")
					.append("  GRAPH <" + graphName + "> {\n")
					.append("    ?concept mt:contextMatch ?contextURI .\n")
					.append("    ?contextURI mt:context ?context .\n")
					.append("    ?contextURI mt:contextScore ?score .\n")
					.append("  }\n")
					.append("}").toString();
		VirtuosoQueryExecution qexec = new VirtuosoQueryExecution(query, virtuosoConnection);
		ResultSet rs = qexec.execSelect();
		
		Map<String, SparseVector> vectorMap = new HashMap<>();
		QuerySolution qs = null;
		if(rs.hasNext()) {
			do {
				qs = rs.next();
				if(vectorMap.containsKey(qs.get("concept").toString())) {
					vectorMap.get(qs.get("concept").toString())
						.put(topicIndexMap.get(qs.get("context").toString()), qs.get("score").asLiteral().getDouble());
				} else {
					vectorMap.put(qs.get("concept").toString(),
							new SparseVector(N)
								.put(topicIndexMap.get(qs.get("context").toString()), qs.get("score").asLiteral().getDouble()));
				}
			} while(rs.hasNext());
		}
    log.info("Done");
		return vectorMap;
	}
}
