package eu.esco.demo.mapping.esco;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.esco.demo.mapping.tool.model.MatchCandidate;

import java.math.BigDecimal;

public class EscoMatchPair {

  private EscoConcept esco;
  private PesConcept pes;
  private BigDecimal score;

  public EscoConcept getEsco() {
    return esco;
  }

  public void setEsco(EscoConcept esco) {
    this.esco = esco;
  }

  public PesConcept getPes() {
    return pes;
  }

  public void setPes(PesConcept pes) {
    this.pes = pes;
  }

  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  @JsonIgnore
  public MatchCandidate.Rank getRank() {
    return MatchCandidate.Rank.getRank(score);
  }

}
