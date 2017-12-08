package eu.esco.demo.mapping.esco;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PesConcept {

  private String incomingMatchUri;
  private List<String> iscoCodes;
  private String prefLabel;
  private List<String> altLabels;

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

  public String getPrefLabel() {
    return prefLabel;
  }

  public void setPrefLabel(String prefLabel) {
    this.prefLabel = prefLabel;
  }

  public List<String> getAltLabels() {
    return altLabels;
  }

  public void setAltLabels(List<String> altLabels) {
    this.altLabels = altLabels;
  }
}
