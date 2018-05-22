package clone.analysis.similarity;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.mongodb.core.mapping.Document;

import clone.analysis.Matchable;
import clone.analysis.Method;
import clone.analysis.dataflow.CloneAnalysis;

@Document
public class StaticCallFlowMatchSet extends MatchSet<Method> {

   private final int editDistance;
   private final boolean isComplete;
   private final Set<MatchSet<Method>> relevantCloneSets = new TreeSet<MatchSet<Method>>();

   private final Method centroid;

   private final boolean isHighPriority;

   public StaticCallFlowMatchSet(Collection<Method> similar, Method centroid, int editDistance, boolean isComplete) {
      super(similar);
      this.editDistance = editDistance;
      this.isComplete = isComplete;
      this.centroid = centroid;

      Method firstMethod = set.iterator().next();
      this.isHighPriority = (
            firstMethod.getControlFlowSize() >= CloneAnalysis.CONTROL_FLOW_THRESHOLD
            || firstMethod.size() >= CloneAnalysis.STATIC_CALL_SIZE_THRESHOLD
            || firstMethod.getUniqueness() >= CloneAnalysis.STATIC_CALL_UNIQUENESS_THRESHOLD
            );
   }

   public int getEditDistance() {
      return editDistance;
   }

   public int getCentroidSize() {
      return 0 == editDistance ? Integer.MAX_VALUE : centroid.size();
   }

   public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Set (")
         .append("editDistance: ")
         .append(editDistance)
         .append(", ")
         .append("isComplete: ")
         .append(isComplete)
         .append(", ")
         .append("packages: ")
         .append(getPackageCount())
         .append(", ")
         .append("classes: ")
         .append(getClassCount())
         .append(", ")
         .append("uniqueness: ")
         .append(set.iterator().next().getUniqueness())
         .append(", ")
         .append("controlFlowSize: ")
         .append(set.iterator().next().getControlFlowSize())
         .append(", ")
         .append("isHighPriority: ")
         .append(isHighPriority)
         .append("): ");
      if (0 == editDistance) {
         List<String> assertNames = set.iterator().next().getStaticCallNames();
         builder.append("\nAssertions (").append(assertNames.size()).append("): ");
         for (String name : assertNames)
            builder.append("\n\t").append(name);
      }
      else {
         builder.append("\nCentroid (size: ").append(centroid.size()).append("): ").append(centroid);
      }
      builder.append("\n\nMethods (").append(size()).append("): ");
		for(Method a : set)
         builder.append(a.getLogLine());
      builder.append("\n\nRelevant Assertion clone sets (").append(relevantCloneSets.size()).append("):");
      for (MatchSet<Method> mst : relevantCloneSets)
         builder.append("\n").append(mst);
      builder.append("\n==============================================\n");
      return builder.toString();
   }

   @Override
      public int compareTo(MatchSet m) {
         StaticCallFlowMatchSet o = (StaticCallFlowMatchSet)m;
         int [] cmps = { 
            Boolean.valueOf(o.isHighPriority).compareTo(Boolean.valueOf(isHighPriority)),
            editDistance - o.editDistance,
            //new Boolean(relevantCloneSets.isEmpty()).compareTo(o.relevantCloneSets.isEmpty()),
            o.centroid.size() - centroid.size(),
            new Boolean(o.isComplete).compareTo(new Boolean(isComplete)),
            centroid.compareTo(o.centroid),
            ((Matchable)o.set.iterator().next()).size() - ((Matchable)set.iterator().next()).size(),
            getPackageCount() - o.getPackageCount(),
            getClassCount() - o.getClassCount(),
            o.size() - size(),
            //o.relevantCloneSets.size() - relevantCloneSets.size(),
            0
         };
         for (Integer i : cmps)
            if (i != 0)
               return i;
         for (Iterator<Method> iter1 = set.iterator(), iter2 = o.set.iterator(); iter1.hasNext() && iter2.hasNext();) {
            int cmp = iter1.next().getQualifiedMethodName().compareTo(iter2.next().getQualifiedMethodName());
            if (cmp != 0)
               return cmp;
         }
         return 0;
      }

   public StaticCallFlowMatchSet addRelevantCloneSet(MatchSet<Method> mst) {
      relevantCloneSets.add(mst);
      return this;
   }

   public StaticCallFlowMatchSet addAllRelevantCloneSets(Collection<MatchSet<Method>> msts) {
      relevantCloneSets.addAll(msts);
      return this;
   }

   public int relevanceSize() {
      return relevantCloneSets.size();
   }
}
