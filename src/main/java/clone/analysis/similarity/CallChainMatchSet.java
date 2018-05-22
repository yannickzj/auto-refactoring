package clone.analysis.similarity;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.data.mongodb.core.mapping.Document;

import clone.analysis.Matchable;
import clone.analysis.dataflow.CloneAnalysis;

@Document
public class CallChainMatchSet<T extends Matchable> extends MatchSet<T> {

   private final double score;
   private final int ccsize;

	public CallChainMatchSet(Collection<T> similar, double score, int ccsize) {
      super(similar);
      this.score = score;
      this.ccsize = ccsize;
	}

	public CallChainMatchSet(double score, int ccsize) {
      this.score = score;
      this.ccsize = ccsize;
	}

   public double getScore() {
      return score;
   }

   public int getCcsize() {
      return ccsize;
   }

   public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Set (score: ")
         .append(String.format("%.2f", score))
         .append(", ccsize: ")
         .append(ccsize)
         .append(", packages: ")
         .append(getPackageCount())
         .append(", classes: ")
         .append(getClassCount())
         .append("): ");
		for(Matchable a : set)
		{
			builder.append(a.getLogLine());
			if(CloneAnalysis.DUMP_STATS)
				builder.append("\n"+a.toString());
		}
      return builder.toString();
   }

   @Override
      public int compareTo(MatchSet m) {
         CallChainMatchSet o = (CallChainMatchSet)m;
         int [] cmps = { 
            Double.compare(o.score, score), 
            o.ccsize - ccsize,
            o.getPackageCount() - getPackageCount(),
            o.getClassCount() - getClassCount()
         };
         for (Integer i : cmps)
            if (i != 0)
               return i;

         for (Iterator<T> iter1 = set.iterator(), iter2 = o.set.iterator(); iter1.hasNext() && iter2.hasNext();) {
            int cmp = iter1.next().compareTo(iter2.next());
            if (cmp != 0)
               return cmp;
         }
         return o.size() - size();
      }
}
