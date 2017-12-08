package eu.esco.demo.mapping.tool.enhancement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.openrdf.model.impl.NumericLiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import virtuoso.sesame2.driver.VirtuosoRepository;

import com.google.common.base.Preconditions;

import eu.esco.demo.mapping.main.Config;
import eu.esco.demo.mapping.tool.enhancement.util.MalletOutputQuery;
import eu.tenforce.commons.sem.sesame.SesameUtils;

public class MalletOutputFormatter implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(MalletOutputFormatter.class);

	private final Path file;
	private final String namedGraph;
	private final String conceptPrefix;
  private double minContextScore;

	public MalletOutputFormatter(Path file, String namedGraph, String conceptPrefix) {
		this.file = file;
		this.namedGraph = namedGraph;
		this.conceptPrefix = conceptPrefix;
	}
	
	public MalletOutputFormatter(MalletOutputQuery moq) {
		this.file = moq.file;
		this.namedGraph = moq.namedGraph;
		this.conceptPrefix = moq.conceptPrefix;
	}

	@Override
	public void run() {
    log.debug("processing {}",file);
		// OPEN FILE
		File contextFile = file.toFile();
		Preconditions.checkArgument(contextFile.exists()); // File must exist
		
		BufferedReader br = null;
		RepositoryConnection con = null;
		org.openrdf.model.URI mtContextMatch = null;
		org.openrdf.model.URI mtContext = null;
		org.openrdf.model.URI mtContextScore = null;
		
		try {
			// OPEN VIRTUOSO CONNECTION
			Repository rep = new VirtuosoRepository("jdbc:virtuoso://" + Config.virtuoso + "/charset=UTF-8/log_enable=2","dba","dba");
      minContextScore = Config.minContextScore;
			rep.initialize();
			con = rep.getConnection();
			con.setNamespace("mt", "http://sem.tenforce.com/vocabularies/mapping-pilot/");
			
			mtContextMatch = con.getValueFactory().createURI("http://sem.tenforce.com/vocabularies/mapping-pilot/", "contextMatch");
			mtContext = con.getValueFactory().createURI("http://sem.tenforce.com/vocabularies/mapping-pilot/", "context");
			mtContextScore = con.getValueFactory().createURI("http://sem.tenforce.com/vocabularies/mapping-pilot/", "contextScore");
			
		} catch(RepositoryException e) {
			e.printStackTrace();
		}
		
		try {
			br = new BufferedReader(new FileReader(contextFile));
			br.readLine(); // Discard Header
			
			// Setup wordmap
			Map<String, Map<Integer, Double>> wordMap = getWordMap(contextFile);
      if(wordMap.isEmpty()){
          log.debug("Empty wordmap");
      }

      int index =0;
			// FOR EACH CONCEPT ADD CONTEXT MATCH TO QUERY
			for(String line = br.readLine(); line != null; line = br.readLine()) {
				// PARSE FILE
				String[] lineParts = line.split("	");
				String concept = URLDecoder.decode(URLDecoder.decode(StringUtils.substringBeforeLast(StringUtils.substringAfterLast(lineParts[1], "/"),"."), "UTF-8"), "UTF-8");

				if(isURI(concept)) {
          if(index % 100 == 0){
              log.info("Wrote {} items", index);
          }
          index++;

					Map<Integer, Double> contextMap = getContextMap(lineParts);
					boostScores(contextMap, wordMap, concept, con);
					
					for(Integer context : contextMap.keySet()) {
						double score = contextMap.get(context);
            if(score < minContextScore) {
                score = 0;
            }
						
						String contextURI = conceptPrefix +"contextMatch/" + DigestUtils.md5Hex(context + "" + score);
						
						con.add(new URIImpl(concept), mtContextMatch, new URIImpl(contextURI), new URIImpl(namedGraph));
						con.add(new URIImpl(contextURI), mtContext, new URIImpl("http://url/to/" + "context/" + context), new URIImpl(namedGraph));
						con.add(new URIImpl(contextURI), mtContextScore, new NumericLiteralImpl(score), new URIImpl(namedGraph));
						
					}
				}
			}
		} catch (IOException | MalformedQueryException | QueryEvaluationException | RepositoryException e) {
			e.printStackTrace();
		} finally {
			SesameUtils.closeQuietly(con);
		}
	}

	private void boostScores(Map<Integer, Double> contextMap,
			Map<String, Map<Integer, Double>> wordMap, String concept, RepositoryConnection con) throws QueryEvaluationException, RepositoryException, MalformedQueryException {
		if(!wordMap.isEmpty()) {
			final double conceptCoefficient = 1.0;
			final double wordCoefficient = 1.0;
			
			// Get Labels
			String query = new StringBuilder()
			.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n")
			.append("PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n")
			.append("SELECT ?label WHERE {\n")
			.append("  GRAPH<" + namedGraph + "> {\n")
			.append("    {\n")
			.append("      <" + concept + "> skos:prefLabel ?label .\n")
			.append("    } UNION {\n")
			.append("      <" + concept + "> skos:altLabel ?label .\n")
			.append("    } UNION {\n")
			.append("      <" + concept + "> skosxl:prefLabel ?xlLabel .\n")
			.append("      ?xlLabel skosxl:literalForm ?label .\n")
			.append("    } UNION {\n")
			.append("      <" + concept + "> skosxl:altLabel ?xlLabel .\n")
			.append("      ?xlLabel skosxl:literalForm ?label .\n")
			.append("    }\n")
			.append("  }\n")
			.append("}").toString();
			
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
			TupleQueryResult rs = tupleQuery.evaluate();
			
			List<String> labels = new ArrayList<>();
			do {
				labels.add(rs.next().getValue("label").stringValue());
			} while(rs.hasNext());
			
			// For each label
			for(String label : labels) {
				// Split on Words
				String[] words = label.replaceAll("/", " ").split(" ");
				for(String word : words) {
					// Lookup words
					if(!word.isEmpty()) {
						Map<Integer, Double> wordContexts = wordMap.get(word);
						if(wordContexts != null) {
							for(Integer context : wordContexts.keySet()) {
								// BOOST
								contextMap.put(context, 
										contextMap.get(context) != null 
											? contextMap.get(context) * conceptCoefficient + wordContexts.get(context) * wordCoefficient
											: wordContexts.get(context) * wordCoefficient);
							}
						}
					}
				}
			}
		}
	}

	private Map<Integer, Double> getContextMap(String[] lineParts) {
		Map<Integer, Double> contextMap = new HashMap<>();
		
		for(int i = 2; i < lineParts.length; i+=2) {
			contextMap.put(Integer.parseInt(lineParts[i]), Double.parseDouble(lineParts[i+1]));
		}
		return contextMap;
	}

	private Map<String, Map<Integer, Double>> getWordMap(File contextFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(contextFile));
		br.readLine(); // discard header
		Map<String, Map<Integer, Double>> wordMap = new HashMap<>();
		
		for(String line = br.readLine(); line != null; line = br.readLine()) {
			String[] lineParts = line.split("	");
			String word = URLDecoder.decode(URLDecoder.decode(StringUtils.substringBeforeLast(StringUtils.substringAfterLast(lineParts[1], "/"),"."), "UTF-8"), "UTF-8");
			if(!isURI(word)) {
				Map<Integer, Double> contextMap = getContextMap(lineParts);
				wordMap.put(word.substring(1), contextMap);
			}
		}
		
		br.close();
		return wordMap;
	}

	private boolean isURI(String uri) {
	    URI u;
		try {
	        u = new URI(uri);
	    } catch (Exception e1) {
	    	log.debug("'{}' is not a uri", uri);
	        return false;
	    }
	    if(u.isAbsolute()) return true;
	    else return false;
	}
	
}
