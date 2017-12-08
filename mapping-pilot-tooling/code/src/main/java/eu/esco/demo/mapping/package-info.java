/**
 * <h2>Overview of tools and how to use them</h2>
 * <p/>
 * We have 4 tools here that do the mapping.
 * <p/>
 * <ol>
 * <li>ExcelParser : takes an excel with translations and outputs rdf</li>
 * <li>MappingTool : takes rdf of PES and outputs txt and json file reports</li>
 * <li>HtmlReport  : takes json file output of MappingTool and generates nice HTML report</li>
 * <li>EscoReport  : takes json file output of MappingTool and generates JSON and HTML report but from ESCO point of view</li>
 * </ol>
 * <p/>
 * Note: for all tools all things are configured with
 * <ul>
 * <li>root folder: which you need to point to your root folder</li>
 * <li>set of configurations: typically same process will happen for each of languages but if you only want to run one
 * you can do it without any problem</li>
 * </ul>
 * <p/>
 * <p/>
 * <h3>ExcelParser</h3>
 * <p/>
 * <p>
 * First step of process is get excels and process them into RDF. This is exaclty what this tool does.
 * Just point to excel translation file in format as reported and it will generate terms for existing concepts.
 * </p>
 * <p>
 * Logic: PT column will be a PT label; both NPT and NPT-NS columns will be NPT labels. Some simple validations will done and
 * if they dont pass they will be logged as warnings. Also some splitting based on semi-colon and stripping of gender will happen.
 * </p>
 * <p>
 * <b>Next step</b> now is to actually takes these files and <b>run an ingest</b> with files in a location
 * where they will be picked up by ingest process. First copy RDF files in './work/ext/ingest/taxonomy/esco/support' and then
 * configure an ingest process in admin tool. Create an ingest profile: 'suggesters' select all, languages select 'en', 'fr',
 * 'nl', 'es' and 'cs', concept schemes select '*' and views is probably not needed but my config has 'list', 'detail',
 * 'tree', 'hierarchy' and 'translation'.
 * </p>
 * <p>
 * Pick a file, select correct ingest profile and run the ingest. After that you can run the other tools.
 * </p>
 * <p/>
 * <h3>MappingTool</h3>
 * <p/>
 * <p>
 * Does major work and outputs results as json. Input is a .ttl file which was already created last year based on some output
 * of NOCs.
 * </p>
 * <p>
 * Logic: take all concepts in file; for each concept test each term (PT, NPT) with solr database in ESCO; for each ESCO concept
 * returned take the highest score (if multiple terms return same concept); sort and save.
 * </p>
 * <p>
 * This JSON output can then be used as input for next 2 tools.
 * </p>
 * <p/>
 * <h3>HtmlReport</h3>
 * <p/>
 * <p>
 * Loads json output of mapping tool and generates a nice report. Per NOC concept.
 * </p>
 * <p>
 * Note: there is a summary but output is hidden for now because they do not want to show it when doing the demo.
 * </p>
 * <h3>EscoReport</h3>
 * <p>
 * Loads json output of mapping tool and generates a json and html report. Per ESCO concept.
 * </p>
 * <p>
 * Currently only valid for occupations (not skills) because we filter also on a list of occupation concepts!
 * </p>
 * <h3>Website</h3>
 * <p>
 * Currently website is hosted at mapping-pilot.escoportal.eu. Homepage is located at 'resources/index.html'.
 * </p>
 * <p>
 * All generated files should be copied to the server. Main thing to keep in mind there is that each language has its own subfolder
 * under the rootfolder of the project.
 * </p>
 */
package eu.esco.demo.mapping;