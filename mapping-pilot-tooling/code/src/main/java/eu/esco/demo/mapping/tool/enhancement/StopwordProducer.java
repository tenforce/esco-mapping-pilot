package eu.esco.demo.mapping.tool.enhancement;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import eu.esco.demo.mapping.main.Config;
import eu.esco.demo.mapping.tool.enhancement.util.StopwordQuery;

public class StopwordProducer implements Runnable {

	public StopwordProducer(StopwordQuery query) {
		graph = query.stopwordGraph;
		file = query.targetFile;
		conceptScheme = query.conceptScheme;
	}

	public StopwordProducer(String graph, String file, String conceptScheme) {
		this.graph = graph;
		this.file = file;
		this.conceptScheme = conceptScheme;
	}

	private final String graph;
	private final String file;
	private final String conceptScheme;

	@Override
	public void run() {
		// OPEN VIRTUOSO CONNECTION
		VirtGraph virtGraph = new VirtGraph("jdbc:virtuoso://" + Config.virtuoso + "/charset=UTF-8/","dba","dba");
		virtGraph.setReadFromAllGraphs(true);

		String query = "PREFIX mt: <http://sem.tenforce.com/vocabularies/mapping-pilot/>\n"
				+ "SELECT ?stopword WHERE {\n"
				+ "{\n"
				+ "GRAPH <" + graph + "> {"
				+ "?settings a mt:MappingSettings .\n"
				+ "?settings mt:settingsFor <" + conceptScheme + "> .\n"
				+ "?settings mt:stopword ?stopword .\n"
				+ "}} UNION {\n"
				+ "GRAPH <" + graph + "> {"
				+ "?settings a mt:MappingSettings .\n"
				+ "?settings mt:settingsFor <" + conceptScheme + "> .\n"
				+ "?settings mt:hiddenStopword ?stopword .\n"
				+ "}}}";
		VirtuosoQueryExecution qexec = new VirtuosoQueryExecution(query, virtGraph);
		ResultSet rs = qexec.execSelect();
		
		StringBuilder stopwords = new StringBuilder();
		
		QuerySolution qs = null;
		if(rs.hasNext()) {
			do {
				qs = rs.next();
				stopwords.append(qs.get("stopword").toString() + "\n");
			} while(rs.hasNext());
		}
		
		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			out.write(stopwords.toString());
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
