package eu.esco.demo.mapping.html;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import eu.esco.demo.mapping.tool.model.MatchingResult;
import eu.esco.demo.mapping.tool.model.MatchingResultSummary;
import org.apache.commons.io.FileUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class HtmlReport implements Runnable {

  public static void main(String[] args) throws IOException {

//    String rootV0 = "F:\\Program Files\\Dropbox\\UNI\\Tenforce\\workspace\\mapping-pilot\\data\\v0-pes\\";
    String rootV1 = "F:\\Program Files\\Dropbox\\UNI\\Tenforce\\workspace\\mapping-pilot\\data\\pes-v1\\";

    for (String file : new String[]{
//            rootV1 + "cs/hosp-occupations-mapping",
//            rootV1 + "es/hosp-occupations-mapping",
//            rootV1 + "es/hosp-skills-mapping",
//            rootV1 + "nl/hosp-occupations-mapping",
//            rootV1 + "nl/hosp-skills-mapping",
            rootV1 + "fr/hosp-occupations-mapping",
            rootV1 + "fr/hosp-skills-mapping"
    }) {
      generateHtmlReport(file);
    }
  }

  public static void generateHtmlReport(String file) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    List<MatchingResult> matchingResults = objectMapper.readValue(new File(file + ".json"),
                                                                  objectMapper.getTypeFactory().constructCollectionType(List.class, MatchingResult.class));

    new HtmlReport(matchingResults, file).run();
  }

  private final List<MatchingResult> matchingResults;
  private final String outputFile;

  public HtmlReport(List<MatchingResult> matchingResults, String outputFile) {
    this.matchingResults = matchingResults;
    this.outputFile = outputFile;
  }

  @Override
  public void run() {
    writeHtmlReport();
  }

  private void writeHtmlReport() {
    try {
      StringWriter writer = new StringWriter();
      Context context = new Context();
      context.setVariable("matchingResults", matchingResults);
      context.setVariable("matchingResultSummary", new MatchingResultSummary(matchingResults));
      getTemplateEngine().process("templates/mapping-report", context, writer);

      FileUtils.write(new File(outputFile + ".html"), writer.toString(), "utf-8");
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static TemplateEngine getTemplateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setTemplateMode("XHTML");
    resolver.setSuffix(".html");
    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }

}
