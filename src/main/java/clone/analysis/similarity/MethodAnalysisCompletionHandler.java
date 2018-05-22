package clone.analysis.similarity;

import org.apache.commons.lang3.tuple.Triple;

import clone.analysis.Method;

public class MethodAnalysisCompletionHandler implements CompletionHandler {
   public void onComplete(Object o) {
      if (null == o)
         return;
      Triple<Method, Method, Integer> triple = 
         (Triple<Method, Method, Integer>)o;
      GraphCloneFinder.addEdge(triple.getLeft(), triple.getMiddle(), triple.getRight());
   }
}
