/**
 * <h2>Mapping Tool v0.10</h2>
 * <p/>
 * Currently the Mapping Tool (Java application) consists of 4 parts
 * <p/>
 * <ol>
 * <li>Solr Mapping		: Generate output used by the front end. Output is based upon mapping between Concepts (in a triple file) and Solr data</li>
 * <li>Context Mapping	: Add context mappings to a virtuoso graph, based upon context information already in the graph</li>
 * <li>Mallet Input		: Generate the input for mallet from files</li>
 * <li>Mallet Output	: Use the mallet output to add context information to a TS</li>
 * </ol>
 * <p/>
 * Note: Configuration is done using a configuration file (example can be found in resources/config/config.xml
 * <p/>
 * <p/>
 * 
 */
package eu.esco.demo.mapping.main;

import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.esco.demo.mapping.tool.MappingTool;
import eu.esco.demo.mapping.tool.enhancement.ContextMatchingTool;
import eu.esco.demo.mapping.tool.enhancement.MalletInputFormatter;
import eu.esco.demo.mapping.tool.enhancement.MalletOutputFormatter;
import eu.esco.demo.mapping.tool.enhancement.StopwordProducer;
import eu.esco.demo.mapping.tool.enhancement.util.ContextQuery;
import eu.esco.demo.mapping.tool.enhancement.util.MalletOutputQuery;
import eu.esco.demo.mapping.tool.enhancement.util.StopwordQuery;

/**
 * A Main class to execute the entire mapping pilot. A config.properties file
 * will override default configuration elements (like directories to read from).
 * The default config.properties file can be found at
 * resources/config/config.xml. A different config file can be provided
 * with the argument --config. Different parts of the mapping-pilot can be
 * executed with their respective arguments.
 */
public class Main {

	private final static Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
		//args = new String[] {"--config", "src/main/resources/config/config.xml", "--solr"}; // CLEAN remove at deployment
    log.debug("Started!");
		// SETUP A COMMAND LINE PARSER
		CommandLineParser parser = new DefaultParser();
		
		// DEFINE OPTIONS
		Options options = new Options();
		Option help = new Option("h", "help", false, "print this message" );
		
		Option config = Option.builder("c").argName( "file" )
				.hasArg()
				.desc("define new config location")
				.longOpt("config")
				.build();
		
		Option solr = Option.builder()
				.desc("execute a solr matching")
				.longOpt("solr")
				.build();
		
		Option context = Option.builder()
				.desc("execute a contextual matching")
				.longOpt("context")
				.build();
		
		Option malletI = Option.builder("mi")
				.desc("build mallet input files from turtle files")
				.longOpt("malletInput")
				.build();
		
		Option malletO = Option.builder("mo")
				.desc("add contextual knowledge to a triple store")
				.longOpt("malletOutput")
				.build();
		
		Option stopword = Option.builder("s")
				.desc("read stopwords from a triple store and feed them to solr")
				.longOpt("stopword")
				.build();
		
		// ADD OPTIONS
		options.addOption(help);
		options.addOption(config);
		options.addOption(solr);
		options.addOption(context);
		options.addOption(malletI);
		options.addOption(malletO);
		options.addOption(stopword);
		
		// READ CLI
		try {
			CommandLine line = parser.parse(options, args);
			
			if(line.hasOption("h")) {
				// automatically generate the help statement
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "Mapping Tool", options );
			}
			
			// ------------------------------- //
			// READ CONFIG FILE
			if(line.hasOption("c")) {
				// User submitted config file location
				Config.readConfig(line.getOptionValue("c"));
			} else {
				// Read default config file
				Config.readConfig();
			}
			
			// ------------------------------- //
			// EXECUTE MAPPING TOOL
			if(line.hasOption("s")) {
				for(StopwordQuery stopwordQuery : Config.stopwordQueries) {
					new StopwordProducer(stopwordQuery).run();
				}
			}
			if(line.hasOption("solr")) {
				new MappingTool(Config.mappingQueries).run();
			}
			
			if(line.hasOption("mi")) {
				for(Path path : Config.malletInputPaths) {
					new MalletInputFormatter(path).run();
				}
			}
			
			if(line.hasOption("mo")) {
				for(MalletOutputQuery moq : Config.malletOutputQueries) {
					new MalletOutputFormatter(moq).run();
				}
			}

			if(line.hasOption("context")) {
				for(ContextQuery contextQuery : Config.contextQueries) {
					new ContextMatchingTool(contextQuery).run();
				}
			}
			
		} catch( ParseException e) {
			e.printStackTrace();
		}
	}

}
