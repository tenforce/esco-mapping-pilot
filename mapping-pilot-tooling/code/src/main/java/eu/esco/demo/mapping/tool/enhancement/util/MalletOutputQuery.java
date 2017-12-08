package eu.esco.demo.mapping.tool.enhancement.util;

import java.nio.file.Path;

public class MalletOutputQuery {
	
	public final Path file;
	public final String namedGraph;
	public final String conceptPrefix;
	
	public MalletOutputQuery(Path file, String namedGraph, String conceptPrefix) {
		this.file = file;
		this.namedGraph = namedGraph;
		this.conceptPrefix = conceptPrefix;
	}
	
}
