package eu.esco.demo.mapping.tool.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public class MatchingResult implements ModelConcept {

  private String incomingMatchUri;
  private List<String> iscoCodes;
  private String prefLabel;
  private List<String> altLabel;
  private String description;
  private List<String> broader;
  private List<MatchCandidate> matchingCandidates = new ArrayList<>();

  public String getIncomingMatchUri() {
    return incomingMatchUri;
  }

  public void setIncomingMatchUri(String incomingMatchUri) {
    this.incomingMatchUri = incomingMatchUri;
  }

  public List<String> getIscoCodes() {
    return iscoCodes;
  }

  public void setIscoCodes(List<String> iscoCodes) {
    this.iscoCodes = iscoCodes;
  }

  @JsonIgnore
  public String getIscoCodeString() {
    if (iscoCodes == null || iscoCodes.isEmpty()) return "";
    if (iscoCodes.size() == 1) return simplifyIscoCode(iscoCodes.get(0));

    List<String> result = new ArrayList<>();
    for (String iscoCode : iscoCodes) {
      result.add(simplifyIscoCode(iscoCode));
    }

    return Joiner.on(", ").join(result);
  }

  private String simplifyIscoCode(String iscoCode) {
    return StringUtils.substringAfterLast(iscoCode, "/");
  }

  @Override
  public String getPrefLabel() {
    return prefLabel;
  }

  public void setPrefLabel(String prefLabel) {
    this.prefLabel = prefLabel;
  }

  @Override
  public List<String> getAltLabel() {
    return altLabel;
  }

  public void setAltLabel(List<String> altLabels) {
    Collections.sort(altLabels, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    });
    this.altLabel = altLabels;
  }

  @Override
  public String getDescription() {
	  return description;
  }
  
  @JsonIgnore
  public boolean hasDescription() {
	  return description != null;
  }

  public void setDefinition(String definition) {
	  this.description = definition;
  }

  @Override
  public List<String> getBroader() {
	  return broader;
  }
  
  @JsonIgnore
  public boolean hasBroader() {
	  return broader != null;
  }

  public void setBroader(List<String> broader) {
	  this.broader = broader;
  }

@JsonIgnore
  public int getRank() {
    if (getMatchingCandidates() == null || getMatchingCandidates().isEmpty()) return 0;

    return getMatchingCandidates().get(0).getRank().getDisplay();
  }

  public List<MatchCandidate> getMatchingCandidates() {
    return matchingCandidates;
  }

  public void setMatchingCandidates(List<MatchCandidate> matchingCandidates) {
    Preconditions.checkNotNull(matchingCandidates);
    this.matchingCandidates = matchingCandidates;
  }

  @Override
  public String toString() {
    String result = "\n\n" + StringUtils.rightPad(getPrefLabel(), 80) + StringUtils.rightPad(incomingMatchUri, 50)
            + (getAltLabel().isEmpty() ? "" : "\n\t\t" + StringUtils.rightPad(getAltLabel().toString(), 150));
    for (int i = 0; i < matchingCandidates.size() && i <= 10; i++) {
      MatchCandidate info = matchingCandidates.get(i);
      result += "\n\t\t\t"
              + StringUtils.center(Integer.toString(info.getRank().getDisplay()), 6)
              + StringUtils.rightPad(info.getIscoCode(), 20)
              + StringUtils.rightPad(info.getPrefLabel(), 60)
              + StringUtils.leftPad(info.getShortText().toString(), 90);
    }

    return result;
  }

  @JsonIgnore
  @Override
  public String getUri() {
	  return getIncomingMatchUri();
  }

}
