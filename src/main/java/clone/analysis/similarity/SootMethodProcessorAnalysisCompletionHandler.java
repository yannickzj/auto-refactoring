package clone.analysis.similarity;

import java.util.List;

import clone.analysis.dataflow.CloneAnalysis;

public class SootMethodProcessorAnalysisCompletionHandler implements CompletionHandler {
   public void onComplete(Object o) {
      if (null == o)
         return;
      CloneAnalysis.analysisResults.add((List<Object>)o);
   }
}
