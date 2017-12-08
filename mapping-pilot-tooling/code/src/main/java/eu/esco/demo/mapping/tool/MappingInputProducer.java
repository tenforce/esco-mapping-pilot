package eu.esco.demo.mapping.tool;

import com.hp.hpl.jena.rdf.model.Resource;

import java.util.List;

public interface MappingInputProducer {

  List<Resource> getResourcesToMatch();

  List<String> getMatchingTerms(Resource resourceToMatch);

  String getEnglishPreferredTerm(Resource resourceToMatch);

}
