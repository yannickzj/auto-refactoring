package clone.analysis.similarity;

import static clone.analysis.dataflow.CloneAnalysis.getExecutorService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;

import clone.analysis.Analysis;
import clone.analysis.Matchable;
import soot.G;

public class SimilarityAnalysis {

   static public class MatchableComparator implements Comparator<Matchable> {
      @Override
         public int compare(Matchable m1, Matchable m2) {
            return m2.size() - m1.size();
         }
   }
	
	public static void analyze(List<? extends Matchable> payload, CompletionHandler handler, boolean isMatch) throws Exception {
      analyze(payload, handler, isMatch, false);
   }

	public static void analyze(List<? extends Matchable> payload, CompletionHandler handler, boolean isMatch, boolean isSorted) throws Exception {
      G.v().out.println("payload size: "+ payload.size());
      long millisecondsStart, timeSpentInMilliseconds;
      millisecondsStart = System.currentTimeMillis();

      if (!isSorted)
         Collections.sort(payload, new MatchableComparator());

      ExecutorCompletionService<Object> ecs = 
         new ExecutorCompletionService<Object>(getExecutorService());
      
      int count = 0;

      if (!isMatch) {
         for(int i=0, n=payload.size(); i<n; ++i) {
            Analysis analysis = 
               Analysis.getAnalysis(payload.get(i));
            if (null == analysis)
               continue;
            ecs.submit(analysis);
            ++count;
         }
      }
      else {
         for(int i=1, n=payload.size(); i<n; ++i)
            for(int j=0; j<i; ++j) {
               Analysis analysis = 
                  Analysis.getAnalysis(payload.get(i), payload.get(j));
               if (null == analysis)
                  continue;
               ecs.submit(analysis);
               ++count;
            }
      }

      if (null != handler)
         for(int i=0; i<count; ++i)
            handler.onComplete(ecs.take().get());
      else
         for(int i=0; i<count; ++i)
            ecs.take().get();

      timeSpentInMilliseconds = System.currentTimeMillis() - millisecondsStart;
      G.v().out.println("Executed " + count +" tasks for " + (timeSpentInMilliseconds/1000) + "s");
   }
}
