package eu.esco.demo.mapping.tool.model;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MatchCandidate implements ModelConcept {

	private static final double[] ranks = {7.0, 5.0, 3.5, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0};
	//  private static final double[] ranks = {4.5, 3.5, 3, 2.2, 1.6, 1.2, 0.8, 0.5, 0.0};

	public enum Rank {
		_1_(1),
		_2_(2),
		_3_(3),
		_4_(4),
		_5_(5),
		_6_(6),
		_7_(7),
		_8_(8),
		_9_(9),
		not_found(0);

		private final int display;

		Rank(int display) {
			this.display = display;
		}

		public static Rank getRank(int display) {
			for (Rank rank : Rank.values()) {
				if (rank.display == display) return rank;
			}

			return null;
		}

		public static Rank getRank(BigDecimal bigDecimal) {
			for (Rank rank : Rank.values()) {
				if (bigDecimal.compareTo(new BigDecimal(ranks[rank.display - 1])) == 1) {
					return rank;
				}
			}
			return _9_;
		}

		public int getDisplay() {
			return display;
		}

		public String getDisplayText() {
			return display == 0 ? name() : Integer.toString(display);
		}

	}

	private String uri;
	private String iscoCode;
	private String prefLabel;
	private String prefLabelEn;
	private List<String> altLabel;
	private String descriptionEn;
	private String description;
	private List<String> broader;
	private List<String> shortText;
	private BigDecimal score;

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
	
	public void setAltLabel(List<String> altLabel) {
		this.altLabel = altLabel;
	}
	
	@Override
	public List<String> getAltLabel() {
		return altLabel;
	}

	public String getDescriptionEn() {
		return descriptionEn;
	}

	public void setDescriptionEn(String descriptionEn) {
		this.descriptionEn = descriptionEn;
	}
	
	public String getDescription() {
		return description != null ? description : descriptionEn;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setBroader(List<String> broader) {
		this.broader = broader;
	}

	@Override
	public List<String> getBroader() {
		return broader;
	}
	
	public List<String> getShortText() {
		return shortText;
	}

	public void setShortText(List<String> shortText) {
		this.shortText = shortText;
	}

	public BigDecimal getScore() {
		return score;
	}

	public void setScore(String score) {
		this.score = new BigDecimal(score);
	}

	@JsonIgnore
	public Rank getRank() {
		return Rank.getRank(getScore());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MatchCandidate)) return false;

		MatchCandidate matchCandidate = (MatchCandidate) o;

		return !(prefLabel != null ? !prefLabel.equals(matchCandidate.prefLabel) : matchCandidate.prefLabel != null);

	}

	@Override
	public int hashCode() {
		return prefLabel != null ? prefLabel.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "MatchCandidate{" +
				"prefLabel='" + prefLabel + '\'' +
				", shortText='" + shortText + '\'' +
				", score='" + score + '\'' +
				'}';
	}

	@JsonIgnore
	@Override
	public boolean hasDescription() {
		return description != null || descriptionEn != null;
	}

}
