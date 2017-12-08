package eu.esco.demo.mapping.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.esco.demo.mapping.tool.MappingQuery;
import eu.esco.demo.mapping.tool.enhancement.util.ContextQuery;
import eu.esco.demo.mapping.tool.enhancement.util.MalletOutputQuery;
import eu.esco.demo.mapping.tool.enhancement.util.StopwordQuery;


public class Config {
	
	private static final Logger log = LoggerFactory.getLogger(Config.class);
	private static final String defaultConfigFile = "config/config.xml";
	
	// CONFIGURATION ELEMENTS
	public static String virtuoso;
	public static double minContextScore;
	public static String solr_template;
	public static List<StopwordQuery> stopwordQueries = new ArrayList<>();
	
	public static double contextCutoff;
	public static List<ContextQuery> contextQueries = new ArrayList<>();
	public static List<MappingQuery> mappingQueries = new ArrayList<>();
	
	public static boolean mallet_split_on_words;
	public static List<Path> malletInputPaths = new ArrayList<>();
	public static List<MalletOutputQuery> malletOutputQueries = new ArrayList<>();
	
	
	public static void readConfig() {
		readConfig(defaultConfigFile);
	}
	
	public static void readConfig(String configFile) {
		try {
			// LOAD CONFIG FILE
			XMLConfiguration config = new XMLConfiguration(configFile);
			
			// Read virtuoso location
			virtuoso = config.getString("virtuoso") != null ? config.getString("virtuoso") : "localhost";
			
			// Read solr template
			solr_template = config.getString("solr_template");
			
			// Read stopword queries
			for(int query = 0 ; query < config.getList("stopwordQueries.stopwordQuery.graph").size() ; query++) {
				String key = "stopwordQueries.stopwordQuery(" + query + ")";
				stopwordQueries.add(new StopwordQuery(config.getString(key + ".graph"),
						config.getString(key + ".file"),
						config.getString(key + ".conceptScheme")));
			}
			
			// Read mapping queries
			for(int query = 0 ; query < config.getList("mappingQueries.mappingQuery.file").size() ; query++) {
				String key = "mappingQueries.mappingQuery(" + query + ")";
				mappingQueries.add(new MappingQuery(config.getString(key + ".file"),
						config.getString(key + ".language"),
						config.getString(key + ".searchType")));
			}
			
			// Read context cutoff
			contextCutoff = config.getDouble("contextCutoff");
			// Read score cutoff
			minContextScore = config.getDouble("minContextScore");
			
			// Read context queries
			for(int query = 0 ; query < config.getList("contextQueries.contextQuery.graphA").size() ; query++) {
				String key = "contextQueries.contextQuery(" + query + ")";
				contextQueries.add(new ContextQuery(config.getString(key + ".graphA"),
						config.getString(key + ".graphB"),
						Paths.get(config.getString(key + ".outputFile"))));
			}
			
			// Read mallet input paths
			mallet_split_on_words = config.getBoolean("mallet_split_on_words",false);
			for(String path : config.getStringArray("malletInputPaths.path")) {
				malletInputPaths.add(Paths.get(path));
			}
			
			// Read mallet output queries
			for(int query = 0 ; query < config.getList("malletOutputQueries.malletOutputQuery.file").size() ; query++) {
				String key = "malletOutputQueries.malletOutputQuery(" + query + ")";
				malletOutputQueries.add(new MalletOutputQuery(Paths.get(config.getString(key + ".file")),
						config.getString(key + ".namedGraph"),
						config.getString(key + ".conceptPrefix")));
			}
			
		} catch (ConfigurationException e) {
			log.error("Could not read config file");
			e.printStackTrace();
			log.debug(e.getMessage());
		}
	}
	
}
