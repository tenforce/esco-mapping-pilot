package eu.esco.demo.mapping.excel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import eu.esco.operations.excel.ExcelMapProcessor;
import eu.esco.operations.excel.ExcelProcessor;
import eu.tenforce.commons.sem.jena.JenaUtils;

public class ExcelParser implements Runnable {

  protected static final Logger log = LoggerFactory.getLogger(ExcelParser.class);

  public static void main(String[] args) {

//    translationtemplate-occ-nl_Final.xls
	
	  String root = "F://Program Files/Dropbox/UNI/Tenforce/workspace/mapping-pilot/data/v1-translations/";
    //String root = "/Users/natan/projects/esco/esco-mapping/data/v1-translations/";
    String[] paths = {
//            root + "hosp-occupations/translationtemplate-occ-cs_Final.xls",
//            root + "hosp-occupations/translationtemplate-occ-es_Final.xls",
//            root + "hosp-occupations/translationtemplate-occ-fr_Final.xls",
//            root + "hosp-occupations/translationtemplate-occ-nl_Final.xls",
//            root + "hosp-skills/translationtemplate-sk-cs_Final.xls",
//            root + "hosp-skills/translationtemplate-sk-es_Final.xls",
            root + "hosp-skills/translationtemplate-sk-fr_Final.xls",
//            root + "hosp-skills/translationtemplate-sk-nl_Final.xls"
    };
//    String excelLocation = "/Users/natan/projects/esco/esco-mapping/data/v1-translations/hosp-occupations/translationtemplate-occ-nl_55terms_v2.xls";

    for (String path : paths) {
      Runnable excelParser = new ExcelParser(path);
      excelParser.run();
    }

  }

  protected final String excelLocation;

  public ExcelParser(String excelLocation) {
    this.excelLocation = excelLocation;
  }


  @Override
  public void run() {
    final Model model = JenaUtils.createMemoryModel();

    File excelFile = new File(excelLocation);
    Preconditions.checkState(excelFile.exists(), "File '" + excelLocation + "' does not exist.");

    log.info("Processing file '{}'.", excelFile);

    ExcelProcessor excelProcessor = new ExcelProcessor();
    HSSFSheet sheet = excelProcessor.getSheet(new FileSystemResource(excelFile), 0);

    excelProcessor.processRows(sheet,
                               new String[]{"type", "language", null, "conceptUri", null, null, "pt", null, "npt", "npt-ns"}, new ExcelMapProcessor() {
              @Override
              public void process(int rowNumber, Map<String, String> row) {
                String language = row.get("language");
                if (StringUtils.isEmpty(language) || language.length() != 2) return;

//                log.debug("{} : {}", rowNumber, row);
                String conceptUri = row.get("conceptUri");

                addTerm(model, language, conceptUri, "prefLabel").apply(row.get("pt"));

                Lists.newArrayList(Iterables.transform(splitTerms(row.get("npt")), addTerm(model, language, conceptUri, "altLabel")));
                Lists.newArrayList(Iterables.transform(splitTerms(row.get("npt-ns")), addTerm(model, language, conceptUri, "altLabel")));
              }
            });


    saveModel2File(model, excelFile);
  }

	/**
	 * Write a model to an rdf file. The file can be found at the same location as
	 * the given file, with the same name (different extension).
	 * 
	 * @param model
	 *            The model to write
	 * @param excelFile
	 *            The File which name and location should be used
	 */
  protected void saveModel2File(Model model, File excelFile) {
    File output = new File(excelFile.getParent(), StringUtils.substringBeforeLast(excelFile.getName(), ".xls") + ".rdf");
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(output);
      JenaUtils.write(model, out);
    }
    catch (FileNotFoundException ignore) {
    }
    finally {
      IOUtils.closeQuietly(out);
    }
  }
  
	/**
	 * Write a model to a file with a given language, on a given path
	 * 
	 * @param model
	 *            The model to write
	 * @param path
	 *            The path for the file
	 * @param lang
	 *            The language for the model to output
	 */
  protected void saveModel2File(Model model, Path path, String lang) {
		log.info("Writing file '{}'", path.toString());
		File output = path.toFile();
		
		if(!output.exists()) {
			try {
				output.createNewFile();
			} catch (IOException e) {
				log.error("Failed writing output file '{}'", path.toString());
			}
		}
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(output);
			model.write(out, lang);
		} catch (FileNotFoundException ignore) {}
		finally {
			IOUtils.closeQuietly(out);
		}
		
	}

  private Function<String, Void> addTerm(final Model model, final String language, final String conceptUri, final String labelType) {
    return new Function<String, Void>() {
      @Nullable
      @Override
      public Void apply(String term) {
        if ("altLabel".equals(labelType) && term.contains("/")) log.warn("Invalid NPT '{}'", term.trim());
        // link concept to label uri
        Resource conceptResource = model.createResource(conceptUri);
        Property labelProperty = model.createProperty("http://www.w3.org/2008/05/skos-xl#", labelType);
        Resource labelResource = model.createResource("http://data.europa.eu/esco/label/" + UUID.randomUUID());

        model.add(conceptResource, labelProperty, labelResource);

        // add literal to label
        model.add(labelResource,
                  model.createProperty("http://www.w3.org/2008/05/skos-xl#", "literalForm"),
                  model.createLiteral(stripGender(term), language));

        // add types to label
        List<Property> types = Lists.newArrayList(model.createProperty("http://www.w3.org/2008/05/skos-xl#", "Label"),
                                                  model.createProperty("http://data.europa.eu/esco/model#", "Label"),
                                                  model.createProperty("http://purl.org/iso25964/skos-thes#", labelType.equals("prefLabel") ? "PreferredTerm" : "SimpleNonPreferredTerm"));
        for (Property type : types) {
          model.add(labelResource, RDF.type, type);
        }

        return null;
      }
    };
  }

  private List<String> splitTerms(String terms) {
    if (Strings.isNullOrEmpty(terms)) return Collections.emptyList();

    return Arrays.asList(terms.split(";"));
  }

  private String stripGender(String term) {
    if (Strings.isNullOrEmpty(term)) return null;

    String result = term;
    if (term.contains("(") && term.contains(")") && StringUtils.substringAfterLast(term, "(").contains(")")) {
      result = StringUtils.substringBeforeLast(term, "(").trim();
    }

    if (result.contains("(")) log.warn("Term : {}", term);
    return result.trim();
  }
}
