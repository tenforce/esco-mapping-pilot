package eu.esco.demo.mapping.tool.enhancement.util;

import java.nio.file.Path;

public class ContextQuery {

	public final String graphA;
	public final String graphB;
	public final Path outputFile;
	
	public ContextQuery(String graphA, String graphB, Path outputFile) {
		this.graphA = graphA;
		this.graphB = graphB;
		this.outputFile = outputFile;
	}

}
