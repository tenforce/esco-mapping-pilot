package eu.esco.demo.mapping.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import eu.esco.demo.mapping.main.Config;
import eu.esco.demo.mapping.tool.model.MatchCandidate;
import eu.esco.demo.mapping.tool.model.MatchingResult;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.openrdf.model.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class MappingOutputProducer implements Callable<MatchingResult> {

  private static final Logger log = LoggerFactory.getLogger(MappingOutputProducer.class);

  private final HttpClient httpClient;
  private final MappingInputProducer mappingInputProducer;
  private final Resource resource;
  private final MappingConfiguration mappingConfiguration;

  public MappingOutputProducer(HttpClient httpClient, MappingInputProducer mappingInputProducer, Resource resource, MappingConfiguration mappingConfiguration) {
    Preconditions.checkNotNull(httpClient);
    Preconditions.checkNotNull(mappingInputProducer);
    Preconditions.checkNotNull(resource);
    Preconditions.checkNotNull(mappingConfiguration);

    this.httpClient = httpClient;
    this.mappingInputProducer = mappingInputProducer;
    this.resource = resource;
    this.mappingConfiguration = mappingConfiguration;
  }

  @Override
  public MatchingResult call() {
    log.info("Matching {}", resource.getURI());
//    System.out.println("resource = " + resource);

    Map<String, MatchCandidate> matchInfoMap = new HashMap<>();

    String englishPreferredTerm = mappingInputProducer.getEnglishPreferredTerm(resource);
    if (englishPreferredTerm != null) {
      String solrRequest = getSolrRequest(MappingConfiguration.getSearchServer("en", Config.solr_template),
                                          englishPreferredTerm,
                                          mappingConfiguration.getSearchType(),
                                          getIscoCodes());
      System.out.println("solrRequest = " + solrRequest);
      GetMethod get = new GetMethod(solrRequest);

      List<MatchCandidate> matchCandidates = getMatchInfo(getJsonRootNode(getJson(get)));
      for (MatchCandidate candidate : matchCandidates) {
        addLanguageSpecificInformation(candidate);
      }

      addCandidatesToMap(matchInfoMap, matchCandidates);

      get.releaseConnection();
    }

    for (String term : mappingInputProducer.getMatchingTerms(resource)) {
      String solrRequest = getSolrRequest(term, mappingConfiguration.getSearchType(), getIscoCodes());
      System.out.println("solrRequest = " + solrRequest);
      GetMethod get = new GetMethod(solrRequest);

      List<MatchCandidate> matchCandidate = getMatchInfo(getJsonRootNode(getJson(get)));
      addCandidatesToMap(matchInfoMap, matchCandidate);

      get.releaseConnection();
    }


    MatchingResult matchingResult = new MatchingResult();
    matchingResult.setIncomingMatchUri(resource.getURI());
    matchingResult.setIscoCodes(getIscoCodes());
    matchingResult.setPrefLabel(getPrefLabel());
    matchingResult.setAltLabel(getAltLabels());
    matchingResult.setDefinition(getDefinition());
    matchingResult.setBroader(getBroader());
    matchingResult.setMatchingCandidates(getFirstTen(getSortedCandidates(matchInfoMap)));

    return matchingResult;
  }

  private void addCandidatesToMap(Map<String, MatchCandidate> matchInfoMap, List<MatchCandidate> matchCandidate) {
    for (MatchCandidate info : matchCandidate) {
      if (!matchInfoMap.containsKey(info.getPrefLabel())) {
        matchInfoMap.put(info.getPrefLabel(), info);
      }
      if (matchInfoMap.containsKey(info.getPrefLabel())) {
        int compare = matchInfoMap.get(info.getPrefLabel()).getScore().compareTo(info.getScore());
        if (compare < 0) {
          matchInfoMap.put(info.getPrefLabel(), info);
        }
      }
    }
  }

  private List<String> getIscoCodes() {
    List<String> result = getValues(resource, SKOS.BROAD_MATCH.stringValue());
    result.addAll(getValues(resource, SKOS.EXACT_MATCH.stringValue()));

    return Lists.newArrayList(Iterables.filter(result, new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return input != null && (
                input.startsWith("http://ec.europa.eu/esco/isco2008/Concept/")
                        || input.startsWith("http://data.europa.eu/esco/isco2008/Concept/")
        );
      }
    }));
  }

  private String getPrefLabel() {
    List<String> values = getValues(resource, mappingConfiguration.getLanguage(), SKOS.PREF_LABEL.stringValue());
    return values.isEmpty() ? null : values.get(0);
  }

  private List<String> getAltLabels() {
    return getValues(resource, mappingConfiguration.getLanguage(), SKOS.ALT_LABEL.stringValue());
  }
  
  private String getDefinition() {
	  List<String> values = getValues(resource, mappingConfiguration.getLanguage(), SKOS.DEFINITION.stringValue());
	  return values.isEmpty() ? null : values.get(0);
  }
  
  private List<String> getBroader() {
	  List<String> values = getValues(resource, SKOS.BROADER.stringValue());
	  return values;
  }

  private List<String> getValues(Resource incomingMatch, String property) {
    List<String> result = new ArrayList<>();

    List<RDFNode> nodes = incomingMatch.getModel().listObjectsOfProperty(incomingMatch, ResourceFactory.createProperty(property)).toList();
    for (RDFNode node : nodes) {
      if (!node.isURIResource()) continue;

      result.add(node.asResource().getURI());
    }

    return result;
  }

  private List<String> getValues(Resource incomingMatch, String language, String property) {
    List<String> result = new ArrayList<>();

    List<RDFNode> nodes = incomingMatch.getModel().listObjectsOfProperty(incomingMatch, ResourceFactory.createProperty(property)).toList();
    for (RDFNode node : nodes) {
      if (!node.isLiteral() || !language.equals(node.asLiteral().getLanguage())) continue;

      result.add(node.asLiteral().getString());
    }

    return result;
  }

  private List<MatchCandidate> getSortedCandidates(Map<String, MatchCandidate> matchInfoMap) {
    List<MatchCandidate> matchCandidateList = new ArrayList<>(matchInfoMap.values());
    Collections.sort(matchCandidateList, new Comparator<MatchCandidate>() {
      @Override
      public int compare(MatchCandidate o1, MatchCandidate o2) {
        return -o1.getScore().compareTo(o2.getScore());
      }
    });
    return matchCandidateList;
  }

  private List<MatchCandidate> getFirstTen(List<MatchCandidate> matchCandidateList) {
    List<MatchCandidate> result = new ArrayList<>();
    for (int i = 0; i < matchCandidateList.size() && i <= 10; i++) {
      MatchCandidate info = matchCandidateList.get(i);
      result.add(info);
    }
    return result;
  }

  private String getJson(GetMethod get) {
    try {
      int result = httpClient.executeMethod(get);

      Preconditions.checkState(200 <= result && result < 300, "Failed Solr call: " + get.getResponseBodyAsString());
      return get.getResponseBodyAsString();
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private String getSolrRequest(String name, String type, List<String> iscoCodes) {
    return getSolrRequest(mappingConfiguration.getSearchServer(), name, type, iscoCodes);
  }

  private String getSolrRequest(String searchServer, String name, String type, List<String> iscoCodes) {
    try {
      String iscoCodePart = type.equals("Occupation") ? getIscoCodesQuery(iscoCodes) : "";
      return searchServer + "/select?q=" +
              "longText" + UriUtils.encodeHost(":", "UTF-8") + UriUtils.encodeQueryParam(name, "UTF-8") +
              iscoCodePart +
              "&fq=type%3Ahttp%5C%3A%2F%2Fdata.europa.eu%2Fesco%2Fmodel%23" + type
              + "&fq=type%3Ahttp%5C%3A%2F%2Fdata.europa.eu%2Fesco%2Fmodel%23MemberConcept"
              + "&fl=*%2Cscore&wt=json&indent=true";
    }
    catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

  private String getIscoCodesQuery(List<String> iscoCodes) {
    if (iscoCodes.isEmpty()) return "";

    try {
      Collection<String> encodedIscoCodes = new ArrayList<>();
      for (String iscoCode : iscoCodes) {
        // fixes wrong data in some input !!!
        iscoCode = "http\\://data.europa.eu/esco/isco2008/Concept/" + StringUtils.substringAfterLast(iscoCode, "/");
        String encodedIscoCode = "relation_to_memberOfISCOGroup"
                + UriUtils.encodeHost(":", "UTF-8")
                + UriUtils.encodeHost(iscoCode, "UTF-8");
        encodedIscoCodes.add(encodedIscoCode);
      }

      return UriUtils.encodeQueryParam("\n", "UTF-8")
              + Joiner.on(UriUtils.encodeQueryParam(" ", "UTF-8")).join(encodedIscoCodes);
    }
    catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

  private static JsonNode getJsonRootNode(String json) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(json);
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private List<MatchCandidate> getMatchInfo(JsonNode root) {
    List<MatchCandidate> result = new ArrayList<>();


    JsonNode docs = root.findValue("response").findValue("docs");
    try {
      for (JsonNode doc : docs) {
        if (doc.findValue("prefLabel") == null) {
          log.warn("Ignoring match candidate URI {} because of missing pref label.", doc.findValue("URI").asText());
          continue;
        }

        MatchCandidate matchCandidate = new MatchCandidate();
        matchCandidate.setUri(doc.findValue("URI").asText());
        log.info("Match candidate : {}", matchCandidate.getUri());

        List<String> iscoCodes = getPropertyAsStringList(doc, "relation_to_memberOfISCOGroup");
        if (iscoCodes.size() > 1) log.warn("Invalid set of isco codes : {}", iscoCodes);
        matchCandidate.setIscoCode(iscoCodes.isEmpty() ? "" : iscoCodes.size() == 1 ? iscoCodes.get(0) : iscoCodes.toString());

        matchCandidate.setPrefLabel(doc.findValue("prefLabel") == null ? "# NOT SET IN CURRENT LANGUAGE #" : doc.findValue("prefLabel").asText());
        matchCandidate.setScore(doc.findValue("score").asText());
        matchCandidate.setShortText(getPropertyAsStringList(doc, "shortText"));
        matchCandidate.setBroader(getPropertyAsStringList(doc, "broader"));
        matchCandidate.setAltLabel(getPropertyAsStringList(doc, "altLabel"));
        
        addEnglishInformation(matchCandidate);

        result.add(matchCandidate);
      }
    }
    catch (NullPointerException e) {
      log.error("Fail!");
      throw e;
    }

    return result;
  }

  private List<String> getPropertyAsStringList(JsonNode doc, String property) {
    List<String> shortTexts = new ArrayList<>();
    if (doc.findValue(property) == null) return shortTexts;

    for (JsonNode shortText : doc.findValue(property)) {
      shortTexts.add(shortText.asText());
    }
    return shortTexts;
  }

  private void addLanguageSpecificInformation(MatchCandidate matchCandidate) {
    JsonNode doc = getDocJsonNode(matchCandidate.getUri(), mappingConfiguration.getLanguage());

    matchCandidate.setPrefLabel(doc.findValue("prefLabel") == null ? "" : doc.findValue("prefLabel").asText());
    matchCandidate.setDescription(doc.findValue("description") == null ? "" : Joiner.on(' ').join(getPropertyAsStringList(doc, "description")));
    matchCandidate.setShortText(getPropertyAsStringList(doc, "shortText"));
  }

  private void addEnglishInformation(MatchCandidate matchCandidate) {
    JsonNode doc = getDocJsonNode(matchCandidate.getUri(), "en");

    matchCandidate.setPrefLabelEn(doc.findValue("prefLabel") == null ? "" : doc.findValue("prefLabel").asText());
    matchCandidate.setDescriptionEn(doc.findValue("description") == null ? "" : Joiner.on(' ').join(getPropertyAsStringList(doc, "description")));
  }

  private JsonNode getDocJsonNode(String uri, String language) {
    String solrRequest = getSolrRequestForUri(uri, language);
    System.out.println("solrRequest en = " + solrRequest);
    GetMethod get = new GetMethod(solrRequest);

    JsonNode rootNode = getJsonRootNode(getJson(get));

    JsonNode docs = rootNode.findValue("response").findValue("docs");
    JsonNode doc = docs.iterator().next();

    get.releaseConnection();

    return doc;
  }

  public String getSolrRequestForUri(String uri, String language) {
    try {
      return MappingConfiguration.getSearchServer(language, Config.solr_template)
              + "/select?q=*%3A*" +
              "&fq=URI%3A" + URLEncoder.encode(StringUtils.replace(uri, ":", "\\:"), "UTF-8")
              + "&wt=json&indent=true";
    }
    catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

}
