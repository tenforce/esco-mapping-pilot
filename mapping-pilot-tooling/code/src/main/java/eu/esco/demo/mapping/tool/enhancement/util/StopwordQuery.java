package eu.esco.demo.mapping.tool.enhancement.util;

public class StopwordQuery {
	
	public final String stopwordGraph;
	public final String targetFile;
	public final String conceptScheme;
	
	public StopwordQuery(String graph, String file, String conceptScheme) {
		this.stopwordGraph = graph;
		this.targetFile = file;
		this.conceptScheme = conceptScheme;
	}
}
