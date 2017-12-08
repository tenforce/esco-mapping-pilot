package eu.esco.demo.mapping.tool.model;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MatchingResultSummary {

  private List<MatchingResult> matchingResults;

  public MatchingResultSummary() {
  }

  public MatchingResultSummary(List<MatchingResult> matchingResults) {
    this.matchingResults = matchingResults;
  }

  public List<MatchingResult> getMatchingResults() {
    return matchingResults;
  }

  public void setMatchingResults(List<MatchingResult> matchingResults) {
    this.matchingResults = matchingResults;
  }

  public Map<MatchCandidate.Rank, Collection<MatchingResult>> getSummary() {
    return
            new TreeMap<>(Multimaps.index(matchingResults, new Function<MatchingResult, MatchCandidate.Rank>() {
              @Nullable
              @Override
              public MatchCandidate.Rank apply(MatchingResult input) {
                return MatchCandidate.Rank.getRank(input.getRank());
              }
            }).asMap());
  }

  public double getAverage() {
    Integer sum = 0;
    Integer count = 0;
    for (Map.Entry<MatchCandidate.Rank, Collection<MatchingResult>> entry : getSummary().entrySet()) {
      count += entry.getValue().size();
      sum += (entry.getKey().ordinal() + 1) * entry.getValue().size();
    }

    return (double) sum / count;
  }

  public String getMatchingQualityReport() {
    String result = "Quality report - average " + String.format("%.2f", getTextAverage()) + "\n";

    List<Integer> countsPerRank = getCountsPerRank();
    for (int i = 1; i <= 9; i++) {
      result += StringUtils.center(String.valueOf(i), 9) + StringUtils.leftPad(String.valueOf(countsPerRank.get(i)), 5) + "\n";
    }
    result += StringUtils.center("-", 9) + StringUtils.leftPad(String.valueOf(countsPerRank.get(0)), 5) + "\n";

    return result;
  }

  private double getTextAverage() {
    List<Integer> countsPerRank = getCountsPerRank();

    int count = 0;
    int total = 0;
    for (int i = 1; i <= 9; i++) {
      count += countsPerRank.get(i);
      total += (9 - i) * countsPerRank.get(i);
    }

    count += countsPerRank.get(0);
    total -= countsPerRank.get(0);

    return 9 - (double) total / count;
  }

  private List<Integer> getCountsPerRank() {
    ImmutableListMultimap<Integer, MatchingResult> index = Multimaps.index(matchingResults, new Function<MatchingResult, Integer>() {
      @Override
      public Integer apply(MatchingResult matchingResult) {
        List<MatchCandidate> matchingCandidates = matchingResult.getMatchingCandidates();
        return matchingCandidates.isEmpty() ? 0 : matchingCandidates.get(0).getRank().getDisplay();
      }
    });

    List<Integer> result = new ArrayList<>();
    for (int i = 0; i <= MatchCandidate.Rank._9_.getDisplay(); i++) {
      result.add(0);
    }

    for (Integer integer : index.keySet()) {
      result.set(integer, index.get(integer).size());
    }
    return result;
  }

}
