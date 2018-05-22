package clone.analysis.branchedflow;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Branches have conditions in conjunction (and)
 * Merge points have conditions in disjunction (or)
 */

public class Predicate<T extends Object> {
   private final Map<T, Boolean> exprsMap = new LinkedHashMap<T, Boolean>();
   private int nullCount = -1;

   public Predicate copy(Predicate<T> p) {
      exprsMap.clear();
      exprsMap.putAll(p.exprsMap);
      nullCount = p.nullCount;
      return this;
   }

   public Predicate merge(Predicate<T> p) {
      for (Map.Entry<T, Boolean> e : p.exprsMap.entrySet()) {
         if (!exprsMap.containsKey(e.getKey()))
            exprsMap.put(e.getKey(), e.getValue());
         else {
            Boolean bool = exprsMap.get(e.getKey());
            if (e.getValue() != bool)
               if (null != bool)
                  exprsMap.put(e.getKey(), null);
         }
      }
      nullCount = -1;
      return this;
   }

   public Predicate put(T expr, Boolean bool) {
      exprsMap.put(expr, bool);
      nullCount = -1;
      return this;
   }

   @Override
      public boolean equals(Object o) {
         if (!(o instanceof Predicate))
            return false;
         Predicate p = (Predicate)o;
         return exprsMap.keySet().equals(p.exprsMap.keySet());
      }

   public int size() {
      return exprsMap.size();
   }

   public int getNullCount() {
      if (-1 == nullCount) {
         nullCount = 0;
         for (Boolean b : exprsMap.values())
            if (null == b)
               ++nullCount;
      }
      return nullCount;
   }

   public Predicate update() {
      getNullCount();
      return this;
   }

   public String toString() {
      return exprsMap.toString();
   }
}
