package eu.esco.demo.mapping.esco;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class EscoConcept {

  private String uri;
  private String iscoCode;
  private String prefLabel;
  private String prefLabelEn;
  private String descriptionEn;
  private List<String> shortText;

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getIscoCode() {
    return iscoCode;
  }

  public void setIscoCode(String iscoCode) {
    this.iscoCode = iscoCode;
  }

  @JsonIgnore
  public String getIscoCodeString() {
    if (iscoCode == null) return "";
    return StringUtils.substringAfterLast(iscoCode, "/");
  }

  public String getPrefLabel() {
    return prefLabel;
  }

  public void setPrefLabel(String prefLabel) {
    this.prefLabel = prefLabel;
  }

  public String getPrefLabelEn() {
    return prefLabelEn;
  }

  public void setPrefLabelEn(String prefLabelEn) {
    this.prefLabelEn = prefLabelEn;
  }

  public String getDescriptionEn() {
    return descriptionEn;
  }

  public void setDescriptionEn(String descriptionEn) {
    this.descriptionEn = descriptionEn;
  }

  public List<String> getShortText() {
    return shortText;
  }

  public void setShortText(List<String> shortText) {
    this.shortText = shortText;
  }

}
