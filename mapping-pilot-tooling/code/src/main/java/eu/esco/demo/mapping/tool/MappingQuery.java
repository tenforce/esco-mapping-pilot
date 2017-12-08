package eu.esco.demo.mapping.tool;

import eu.esco.demo.mapping.main.Config;

public class MappingQuery {
	
	public final String file;
	public final MappingConfiguration configuration;
	
	public MappingQuery(String file, String language, String searchType) {
		this.file = file;
		this.configuration = MappingConfiguration.fromTemplate(language, Config.solr_template, searchType);
	}
}