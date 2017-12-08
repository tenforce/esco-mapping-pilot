package eu.esco.demo.mapping.tool;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openrdf.model.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class RdfMappingInputProducer implements MappingInputProducer {

  private static final Logger log = LoggerFactory.getLogger(RdfMappingInputProducer.class);


  private final Model model;
  private final String language;
  private final List<String> searchFields;

  public RdfMappingInputProducer(Model model, String language, List<String> searchFields) {
    this.model = model;
    this.language = language;
    this.searchFields = searchFields;
  }

  @Override
  public List<Resource> getResourcesToMatch() {
    return model.listResourcesWithProperty(RDF.type, ResourceFactory.createResource(SKOS.CONCEPT.stringValue())).toList();
  }

  @Override
  public List<String> getMatchingTerms(Resource resourceToMatch) {
    List<String> result = new ArrayList<>();
    for (String searchField : searchFields) {
      List<RDFNode> terms = model.listObjectsOfProperty(resourceToMatch, ResourceFactory.createProperty(searchField)).toList();
      for (RDFNode term : terms) {
        if (!term.isLiteral() || !language.equals(term.asLiteral().getLanguage())) continue;

        String termString = term.asLiteral().getString();
        result.add(cleanUpString(termString));
      }
    }
    return result;
  }

  @Override
  public String getEnglishPreferredTerm(Resource resourceToMatch) {
    List<RDFNode> terms = model.listObjectsOfProperty(resourceToMatch, ResourceFactory.createProperty(SKOS.PREF_LABEL.stringValue())).toList();
    for (RDFNode term : terms) {
      if (!term.isLiteral() || !"en".equals(term.asLiteral().getLanguage())) continue;

      return cleanUpString(term.asLiteral().getString());
    }
    return null;
  }


  private String cleanUpString(String incomingString) {
    return solrEscapeString(
//            withoutPartRemoval(
                    incomingString
//            )
    );
  }

  private String withoutPartRemoval(String incomingString) {
    return incomingString.replaceAll("\\(kromě.*\\)", "");
  }

  private String solrEscapeString(String incomingString) {
    String[] toEscape = {"\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":",
                         "/"};

    for (String s : toEscape) {
//      incomingString = StringUtils.replace(incomingString, s, "\\" + s);
      incomingString = StringUtils.replace(incomingString, s, " ");
    }
    return incomingString;
  }

}
