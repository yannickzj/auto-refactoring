package clone.analysis.similarity;

import static clone.analysis.similarity.ResultLogger.log;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import clone.analysis.Matchable;

@Document
public abstract class MatchSet<T extends Matchable> implements Comparable<MatchSet>, Iterable<T> {
	
   @Id
   private ObjectId id;

   protected int packageCount = -1;
   protected int classCount = -1;

   protected final Set<T> set = new TreeSet<T>();

	public MatchSet(Collection<T> similar) {
		set.addAll(similar);
	}

	public MatchSet(MatchSet<T> similar) {
		set.addAll(similar.set);
	}

	public MatchSet() {}

	public Set<String> getUniqueMethods()
	{
		HashSet<String> methods =  new HashSet<String>();
		for (Matchable a : set)
         methods.add(a.getQualifiedMethodName());
		return methods;
	}

   public Iterator<T> iterator() {
      return set.iterator();
   }

   public ObjectId getId() {
      return id;
   }

   public Set<T> getSet() {
      return set;
   }

	public void print()
	{
		log(toString());
	}
	
   public int size() {
      return set.size();
   }

   public boolean isEmpty() {
      return set.isEmpty();
   }

   public void add(T m) {
      set.add(m);
   }

   public void addAll(Collection<T> c) {
      set.addAll(c);
   }

   public int getPackageCount() {
      if (packageCount >= 0)
         return packageCount;
      Set<String> packageNames = new HashSet<String>();
      for (Matchable a : set)
         packageNames.add(a.getPackageName());
      packageCount = packageNames.size();
      return packageCount;
   }

   public int getClassCount() {
      if (classCount >= 0)
         return classCount;
      Set<String> classNames = new HashSet<String>();
      for (Matchable a : set)
         classNames.add(a.getClassName());
      classCount = classNames.size();
      return classCount;
   }

   public MatchSet<T> compute() {
      getPackageCount();
      getClassCount();
      return this;
   }

   public abstract String toString();
}
