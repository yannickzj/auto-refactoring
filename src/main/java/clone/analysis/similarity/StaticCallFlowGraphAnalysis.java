package clone.analysis.similarity;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;

import clone.analysis.Method;

public class StaticCallFlowGraphAnalysis extends GraphAnalysis<Method> {
   private final int editDistance;

   public StaticCallFlowGraphAnalysis(int editDistance) {
      this.editDistance = editDistance;
   }

   public String fileName() {
      return String.format("editDistance(%d).dot", editDistance);
   }

   public void findMatchSets(ConnectivityInspector<Method, DefaultEdge> inspector) {
      if (0 == editDistance) {
         for (Set<Method> connectedSet : inspector.connectedSets())
            if (connectedSet.size() > 1)
               resultMatchSet.add(
                     new StaticCallFlowMatchSet(
                        connectedSet, Method.EMPTY_METHOD, editDistance, isCompleteSubgraph(connectedSet)).compute());
      }
      else {
         for (Method vertex : graph.vertexSet()) {
            Set<Method> subgraph = new HashSet<Method>(getNeighbors(vertex));
            subgraph.add(vertex);
            if (subgraph.size() > 1)
               resultMatchSet.add(
                     new StaticCallFlowMatchSet(
                        subgraph, vertex, editDistance, isCompleteSubgraph(subgraph)).compute());
         }
      }
   }
}
