package clone.analysis.similarity;

import clone.analysis.StaticCall;
import clone.analysis.dataflow.CloneAnalysis;

public class StaticCallMakerAnalysisCompletionHandler implements CompletionHandler {
   public void onComplete(Object o) {
      if (null == o)
         return;
      CloneAnalysis.addAssertion((StaticCall)o);
   }
}
