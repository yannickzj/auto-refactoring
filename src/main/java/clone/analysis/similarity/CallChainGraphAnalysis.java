package clone.analysis.similarity;

import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;

import clone.analysis.Matchable;

public class CallChainGraphAnalysis<T extends Matchable> extends GraphAnalysis<T>{
   private final double score;
   private final int ccsize;

   public CallChainGraphAnalysis(double score, int ccsize) {
      this.score = score;
      this.ccsize = ccsize;
   }

   public String fileName() {
      return String.format("score(%.2f)_ccsize(%d).dot", score, ccsize);
   }

   public void findMatchSets(ConnectivityInspector<T, DefaultEdge> inspector) {
      for (Set<T> connectedSet : inspector.connectedSets())
         if (connectedSet.size() > 1 && isCompleteSubgraph(connectedSet))
            resultMatchSet.add(new CallChainMatchSet<T>(connectedSet, score, ccsize).compute());
   }
}
