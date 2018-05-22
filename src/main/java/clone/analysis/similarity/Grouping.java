package clone.analysis.similarity;

import static clone.analysis.similarity.ResultLogger.log;

import java.util.HashSet;
import java.util.Set;

import clone.analysis.Matchable;

public class Grouping<T extends Matchable> {
	
	private final Set<MatchSet<T>> list = new HashSet<MatchSet<T>>();

	public void group(Set<MatchSet<T>> matches)
	{
      list.addAll(matches);
	}

   public Set<MatchSet<T>> getList() {
      return list;
   }
	
	public void display()
	{
		log("Total assertions:\t"+list.size());
      for(MatchSet<T> m: list)
         m.print();

	}
}
