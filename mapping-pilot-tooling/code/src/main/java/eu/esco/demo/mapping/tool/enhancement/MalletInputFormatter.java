package eu.esco.demo.mapping.tool.enhancement;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openrdf.model.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.esco.demo.mapping.main.Config;
import eu.tenforce.commons.sem.jena.JenaUtils;

public class MalletInputFormatter implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(MalletInputFormatter.class);

	public static void main(String[] args) {
		Path[] dirs = new Path[] {
				//				Paths.get("F:\\Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/corpus/corpus_dutch/"),
				//				Paths.get("F:\\Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/corpus/corpus_french/"),
				Paths.get("F:\\Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/v1-translations/hosp-occupations/occ-fr_final.ttl"),
				Paths.get("F:\\Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/pes-v1/fr/hosp-occupations.ttl")
		};
		for(Path dir : dirs) {
			new MalletInputFormatter(dir).run();
		}
	}

	private final Path dir;
	private final static String SKOS_XL = "http://www.w3.org/2008/05/skos-xl#";

	public MalletInputFormatter(Path dir) {
		this.dir = dir;
	}

	public void run() {
		try {
			File file = dir.toFile();
			// GIVEN DIR IS A DIRECTORY,
			// VISIT ALL FILES IN DIRECTORY > EXTRACT BODY FROM CORPUS.XML FILES
			if(file.exists() && file.isDirectory()) {		
				Files.walkFileTree(dir, new SimpleFileVisitor<Path>(){

					@Override
					public FileVisitResult postVisitDirectory(Path dir,
							IOException arg1) throws IOException {
						log.debug("Visited '{}'", dir);
						return FileVisitResult.TERMINATE;
					}

					@Override
					public FileVisitResult preVisitDirectory(Path dir,
							BasicFileAttributes arg1) throws IOException {
						log.debug("Visiting '{}'", dir);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes arg1) throws IOException {
						log.debug("Visiting file '{}'", file);
						File output = new File(dir.toString() + "_mallet" + File.separator + file.getFileName() + ".txt");
						if(!output.exists()) {
							Document doc;
							try {
								doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
							} catch (SAXException | ParserConfigurationException e) {
								throw new IOException(e);
							}

							Element body = (Element) doc.getElementsByTagName("body").item(0);

							try {

								TransformerFactory tf = TransformerFactory.newInstance();
								Transformer transformer = tf.newTransformer();
								transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
								StringWriter writer = new StringWriter();
								transformer.transform(new DOMSource(body), new StreamResult(writer));
								body = (Element) DocumentBuilderFactory
										.newInstance()
										.newDocumentBuilder()
										.parse(new ByteArrayInputStream(writer.getBuffer().toString().replaceAll("\\>", "> ").getBytes()))
										.getDocumentElement();

							} catch(Exception e) { log.error("Fail"); e.printStackTrace(); }

							FileWriter fw = new FileWriter(output);
							fw.write(body.getTextContent());
							fw.flush();
							fw.close();
						} else {
							log.debug("'{}' Already exists, skipping", output.toPath());
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file,
							IOException arg1) throws IOException {
						log.debug("Failed visiting file '{}'", file);
						return FileVisitResult.TERMINATE;
					}
				});
			// GIVEN DIR IS A FILE,
			// VISIT RDF > OUTPUT ALL CONCEPTS AS SEPERATE PLAIN TEXT FILES
			} else if(file.exists() && file.isFile()) {
				final Model model = JenaUtils.createMemoryModel();
				model.read(dir.toString(), "TTL");
				
				// Consider all resources with a pref_label (or with a skos_xl:prefLabel -> esco)
				List<Resource> resList = model.listResourcesWithProperty(model.getProperty(SKOS.PREF_LABEL.stringValue())).toList();
				resList.addAll(model.listResourcesWithProperty(model.getProperty(SKOS_XL + "prefLabel")).toList());
				
				// Create folder structure
				String outputDirString = file.getParent() + File.separator + "mallet_" + file.getName() + File.separator;
				File outputDir = new File(outputDirString);
				if(!outputDir.exists()) outputDir.mkdir();
				
				for(Resource resource : resList) {
					// Define interesting data
					String prefLabel = null, xlPrefLabel = null;
					List<RDFNode> altLabels, xlAltLabels;
					StringBuilder resourceData = new StringBuilder();
					
					if(resource.hasProperty(model.getProperty(SKOS.PREF_LABEL.stringValue()))) {
						prefLabel = model.getProperty(resource, model.getProperty(SKOS.PREF_LABEL.stringValue())).getString();
						resourceData.append(prefLabel);
					} else if(resource.hasProperty(model.getProperty(SKOS_XL + "prefLabel"))) {
						xlPrefLabel = model.getProperty(resource, model.getProperty(SKOS_XL + "prefLabel")).getResource().getProperty(model.getProperty(SKOS_XL + "literalForm")).getString();
						resourceData.append(xlPrefLabel);
					}
					
					altLabels = model.listObjectsOfProperty(resource, model.getProperty(SKOS.ALT_LABEL.stringValue())).toList();
					for(RDFNode node : altLabels) {
						resourceData.append("\n" + node.toString());
					}
					
					xlAltLabels = model.listObjectsOfProperty(resource, model.getProperty(SKOS_XL + "altLabel")).toList();
					for(RDFNode node : xlAltLabels) {
						resourceData.append("\n" + node.asResource().getProperty(model.getProperty(SKOS_XL + "literalForm")).getString());
					}
					
					if(resource.hasProperty(model.getProperty(SKOS.DEFINITION.stringValue()))) {
						resourceData.append("\n" + model.getProperty(resource, model.getProperty(SKOS.DEFINITION.stringValue())).getString());
					}
					
					// Output
					FileWriter fw = new FileWriter(new File(outputDirString + URLEncoder.encode(resource.getURI(),"UTF-8") + ".txt"));
					fw.write(resourceData.toString());
					fw.flush();
					
					// Word based output
					if(Config.mallet_split_on_words) {
						// Gather all data
						List<String> labels = new ArrayList<>();
						if(prefLabel != null) labels.add(prefLabel);
						if(xlPrefLabel != null) labels.add(xlPrefLabel);
						for(RDFNode node : altLabels) { labels.add(node.toString()); }
						for(RDFNode node : xlAltLabels) { labels.add(node.asResource().getProperty(model.getProperty(SKOS_XL + "literalForm")).getString()); }
						
						// Parse/Split/Check/Write for each label
						for(String label : labels) {
							label = label.replaceAll("/", " ").trim();
							for(String word : label.split(" ")) {
								if(!word.isEmpty()) {
									File wordFile = new File(outputDirString + URLEncoder.encode("_" + word, "UTF-8") + ".txt");
									if(!wordFile.exists()) {
										wordFile.createNewFile();
										FileWriter ww = new FileWriter(wordFile);
										ww.write(word);
										ww.flush();
									}
								}
							}
						}
					}
				}
			} else { throw new FileNotFoundException(dir + " does not exist"); }
		} catch (IOException e) {
			log.error("Fail");
			e.printStackTrace();
		}
	}
}