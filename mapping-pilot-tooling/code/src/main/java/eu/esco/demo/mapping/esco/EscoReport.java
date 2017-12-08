package eu.esco.demo.mapping.esco;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Multimaps;
import eu.esco.demo.mapping.tool.model.MatchCandidate;
import eu.esco.demo.mapping.tool.model.MatchingResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class EscoReport implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(EscoReport.class);

  private static final Set<String> uris = new HashSet<>();

//  static { // groups
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.2");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.1");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.3");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.2");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.1π
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.2");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.1");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3π
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1π
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.2");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1");
//  uris.add("http://data.europa.eu/esco/occupation/CTC_14348");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.2");
//  uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3");
//  }

  static { // members
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.1.m.3");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.2.m.10");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.1.m.8");
    uris.add("http://data.europa.eu/esco/occupation/CTC_15850_20140807");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.2.m.20");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.2.m.26");
    uris.add("http://data.europa.eu/esco/occupation/CTC_16019_20140807");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.2.1.m.2");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.3.2.m.2");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14333_20140807");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.3.1.m.7");
    uris.add("http://data.europa.eu/esco/occupation/CTC_15860_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14294_20140807");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.2.2.m.12");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.2.2.m.13");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14580_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14485_20140807");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.2.m.6");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.2.m.5");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14369_20140807");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.2.m.1");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.2.m.8");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.2.m.9");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.3.2.m.3");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.2.m.20");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.2.m.11");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.2.m.17");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.1.2.m.10");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.21");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.24");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.31");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.32");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.35");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.11");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.3");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.9");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.1.3.m.6");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14455_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14453");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14582_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_15830_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14459_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_43257");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14457_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14621_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14335_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14610_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14571_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_15892_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14310_20140807");
    uris.add("http://data.europa.eu/esco/occupation/CTC_14606_20140807");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.1.m.7");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.1.2.1.m.9");
    uris.add("http://data.europa.eu/esco/occupation/HOSPI.2.1.1.m.14");
  }

  public static void main(String[] args) throws IOException {
    String rootV1 = "/Users/natan/projects/esco/esco-mapping/data/pes-v1/";

    for (String file : new String[]{
            rootV1 + "nl/hosp-occupations-mapping",
            rootV1 + "fr/hosp-occupations-mapping",
            rootV1 + "cs/hosp-occupations-mapping",
            rootV1 + "es/hosp-occupations-mapping"
    }) {
      generateEscoReport(file);
    }
  }

  private static void generateEscoReport(String file) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    List<MatchingResult> matchingResults = objectMapper.readValue(new File(file + ".json"),
                                                                  objectMapper.getTypeFactory().constructCollectionType(List.class, MatchingResult.class));

    new EscoReport(matchingResults, file).run();

  }

  private final List<MatchingResult> matchingResults;
  private final String file;

  private final Map<String, EscoMatch> escoMatchMap = new HashMap<>();

  public EscoReport(List<MatchingResult> matchingResults, String file) {
    this.matchingResults = matchingResults;
    this.file = file;
  }

  @Override
  public void run() {
    fillEscoMatchMap();
    generateEscoJson();
    writeHtmlReport();

//    for (EscoMatch escoMatch : escoMatchMap.values()) {
//      System.out.println(escoMatch.getUri());
//      List<EscoMatchPair> escoMatchPairs = escoMatch.getEscoMatchPairs();
//      if (escoMatchPairs.isEmpty()) {
//        System.out.println("\t\tNOTHING FOUND!!!!!");
//      }
//      else {
//        EscoMatchPair escoMatchPair = escoMatchPairs.get(0);
//        System.out.println("\tScore : " + MatchCandidate.Rank.getRank(escoMatchPair.getScore()).getDisplay());
//
//        System.out.println("\tESCO  : " + escoMatchPair.getEsco().getPrefLabel());
//        System.out.println("\tPES   : " + escoMatchPair.getPes().getPrefLabel());
//      }
//    }
  }

  private void generateEscoJson() {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      objectMapper.writeValue(new File(file + "-esco.json"), escoMatchMap);
    }
    catch (IOException e) {
      log.error("Failed to save esco report.", e);
    }
  }

  private void fillEscoMatchMap() {
    for (String uri : uris) {
      escoMatchMap.put(uri, new EscoMatch(uri));
    }

    for (MatchingResult matchingResult : matchingResults) {
      for (MatchCandidate matchCandidate : matchingResult.getMatchingCandidates()) {
        if (!uris.contains(matchCandidate.getUri())) continue;

        EscoMatch escoMatch = escoMatchMap.get(matchCandidate.getUri());
        escoMatch.addMatchingResult(matchingResult, matchCandidate);
      }
    }
  }

  private void writeHtmlReport() {
    try {
      StringWriter writer = new StringWriter();
      Context context = new Context();
      context.setVariable("escoMatches", escoMatchMap.values());
      context.setVariable("escoSummary", getEscoSummary());
      context.setVariable("average", getAverage());
      getTemplateEngine().process("templates/esco-mapping-report", context, writer);

      FileUtils.write(new File(file + "-esco.html"), writer.toString());
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private Map<MatchCandidate.Rank, Collection<EscoMatch>> getEscoSummary() {
    return
            new TreeMap<>(Multimaps.index(escoMatchMap.values(), new Function<EscoMatch, MatchCandidate.Rank>() {
              @Nullable
              @Override
              public MatchCandidate.Rank apply(EscoMatch input) {
                return input.getRank();
              }
            }).asMap());
  }

  private double getAverage() {
    Integer sum = 0;
    Integer count = 0;
    for (Map.Entry<MatchCandidate.Rank, Collection<EscoMatch>> entry : getEscoSummary().entrySet()) {
      count += entry.getValue().size();
      sum += (entry.getKey().ordinal() + 1) * entry.getValue().size();
    }

    return (double) sum / count;
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
