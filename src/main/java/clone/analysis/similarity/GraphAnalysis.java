package clone.analysis.similarity;

import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import clone.analysis.Matchable;
import clone.analysis.dataflow.CloneAnalysis;

public abstract class GraphAnalysis<T extends Matchable> implements Callable<Void>, Comparable<GraphAnalysis> {

   protected final SimpleGraph<T, DefaultEdge> graph = 
      new SimpleGraph<T, DefaultEdge>(DefaultEdge.class);

   protected final Set<MatchSet<T>> resultMatchSet = new HashSet<MatchSet<T>>();

   public void addVertex(T vertex) 
   {
      graph.addVertex(vertex);
   }

   public void removeVertex(T vertex) 
   {
      graph.removeVertex(vertex);
   }	

   public void addEdge(T source, T target) 
   {
      graph.addEdge(source, target);
   }

   public void removeEdge(T source, T target) {
      graph.removeEdge(source, target);
   }

   public int degreeOf(T source) {
      return graph.degreeOf(source);
   }

   public List<T> getNeighbors(T a) {
      return Graphs.neighborListOf(graph, a);
   }

   public void group(Grouping<T> grouping) {
      grouping.group(resultMatchSet);
   }

   public abstract void findMatchSets(ConnectivityInspector<T, DefaultEdge> inspector);
   public abstract String fileName();

   public void generateGraph() {
      try {
         new DOTExporter(new StringNameProvider<T>(), null, null).export(
               new FileWriter(
                  CloneAnalysis.RESULT_LOCATION+"/graph/"
                  +fileName()), graph);
      } catch(Exception e) {
         e.printStackTrace();
      }
   }

   @Override
   public Void call() {
      if (CloneAnalysis.GENERATE_GRAPH)
         generateGraph();
      findMatchSets(new ConnectivityInspector<T, DefaultEdge>(graph));
      return null;
   }

   @Override
   public int compareTo(GraphAnalysis g) {
      return g.graph.vertexSet().size() - graph.vertexSet().size();
   }

   protected boolean isCompleteSubgraph(Set<T> vertices) {
      for (T vertex : vertices) {
         List<T> subgraph = getNeighbors(vertex);
         subgraph.add(vertex);
         if (!new HashSet<T>(subgraph).equals(vertices))
            return false;
      }
      return true;
   }
}
