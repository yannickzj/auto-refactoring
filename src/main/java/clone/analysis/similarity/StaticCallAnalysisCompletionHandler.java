package clone.analysis.similarity;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.fraction.Fraction;

import clone.analysis.Matchable;
import clone.analysis.StaticCall;

public class StaticCallAnalysisCompletionHandler implements CompletionHandler {
   public void onComplete(Object o) {
      if (null == o)
         return;
      Triple<StaticCall, StaticCall, Pair<Fraction, Integer>> triple = 
         (Triple<StaticCall, StaticCall, Pair<Fraction, Integer>>)o;
      GraphCloneFinder.addEdge(triple.getLeft(), triple.getMiddle(), triple.getRight());
   }
}
