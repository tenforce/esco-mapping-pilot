package eu.esco.demo.mapping.esco;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.esco.demo.mapping.tool.model.MatchCandidate;
import eu.esco.demo.mapping.tool.model.MatchingResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EscoMatch {

  private String uri;
  private List<EscoMatchPair> escoMatchPairs = new ArrayList<>();

  public EscoMatch(String uri) {
    this.uri = uri;
  }

  public void addMatchingResult(MatchingResult matchingResult, MatchCandidate matchCandidate) {
    EscoMatchPair escoMatchPair = new EscoMatchPair();

    EscoConcept escoConcept = new EscoConcept();
    escoConcept.setUri(matchCandidate.getUri());
    escoConcept.setIscoCode(matchCandidate.getIscoCode());
    escoConcept.setPrefLabel(matchCandidate.getPrefLabel());
    escoConcept.setPrefLabelEn(matchCandidate.getPrefLabelEn());
    escoConcept.setDescriptionEn(matchCandidate.getDescriptionEn());
    escoConcept.setShortText(matchCandidate.getShortText());
    escoMatchPair.setEsco(escoConcept);

    PesConcept pesConcept = new PesConcept();
    pesConcept.setIncomingMatchUri(matchingResult.getIncomingMatchUri());
    pesConcept.setIscoCodes(matchingResult.getIscoCodes());
    pesConcept.setPrefLabel(matchingResult.getPrefLabel());
    pesConcept.setAltLabels(matchingResult.getAltLabel());
    escoMatchPair.setPes(pesConcept);

    escoMatchPair.setScore(matchCandidate.getScore());

    escoMatchPairs.add(escoMatchPair);

    Collections.sort(escoMatchPairs, new Comparator<EscoMatchPair>() {
      @Override
      public int compare(EscoMatchPair o1, EscoMatchPair o2) {
        return - o1.getScore().compareTo(o2.getScore());
      }
    });
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public List<EscoMatchPair> getEscoMatchPairs() {
    return escoMatchPairs;
  }

  public List<EscoMatchPair> getEscoMatchPairsFilteredOnRankAndCount(final int levelLimit, int sizeLimit) {
    List<EscoMatchPair> result = Lists.newArrayList(Iterables.filter(escoMatchPairs, new Predicate<EscoMatchPair>() {
      @Override
      public boolean apply(EscoMatchPair input) {
        return input.getRank().ordinal() < levelLimit;
      }
    }));
    return result.subList(0, Math.min(sizeLimit, result.size()));
  }

  public void setEscoMatchPairs(List<EscoMatchPair> escoMatchPairs) {
    this.escoMatchPairs = escoMatchPairs;
  }

  public MatchCandidate.Rank getRank() {
    if (escoMatchPairs.isEmpty()) return MatchCandidate.Rank.not_found;

    return escoMatchPairs.get(0).getRank();
  }
}
