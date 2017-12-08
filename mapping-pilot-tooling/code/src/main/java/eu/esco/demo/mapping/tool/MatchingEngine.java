package eu.esco.demo.mapping.tool;

import com.hp.hpl.jena.rdf.model.Resource;
import eu.esco.demo.mapping.tool.model.MatchingResult;
import org.apache.commons.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MatchingEngine implements Callable<List<MatchingResult>> {

  private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

  private final HttpClient httpClient;
  private final MappingInputProducer mappingInputProducer;
  private final MappingConfiguration mappingConfiguration;

  public MatchingEngine(MappingInputProducer mappingInputProducer, MappingConfiguration mappingConfiguration) {
    httpClient = new HttpClient();
    this.mappingInputProducer = mappingInputProducer;
    this.mappingConfiguration = mappingConfiguration;
  }

  @Override
  public List<MatchingResult> call() {
    ExecutorService executor = Executors.newFixedThreadPool(1);
    List<Future<MatchingResult>> futures = new ArrayList<>();

    addMatchingCalls(executor, futures);

    List<MatchingResult> result = collectMatchingResults(futures);

    executor.shutdown();

    return result;
  }

  private void addMatchingCalls(ExecutorService executor, List<Future<MatchingResult>> futures) {
//    int max = 0;
    for (Resource resource : mappingInputProducer.getResourcesToMatch()) {
//      if (++max == 10) break;
      MappingOutputProducer mappingOutputProducer = new MappingOutputProducer(httpClient, mappingInputProducer, resource, mappingConfiguration);

      Future<MatchingResult> future = executor.submit(mappingOutputProducer);
      futures.add(future);
    }
  }

  private List<MatchingResult> collectMatchingResults(List<Future<MatchingResult>> futures) {
    List<MatchingResult> result = new ArrayList<>();
    for (Future<MatchingResult> future : futures) {
      try {
        MatchingResult matchingResult = future.get();
        //System.out.println("matchingResult.getPrefLabel() = " + matchingResult.getPrefLabel());
        result.add(matchingResult);
      }
      catch (InterruptedException | ExecutionException e) {
        log.error("Failed to get result.", e);
      }
    }
    return result;
  }

}
