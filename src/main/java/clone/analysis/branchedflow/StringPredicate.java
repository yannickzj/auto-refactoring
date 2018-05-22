package clone.analysis.branchedflow;

import soot.Value;
public class StringPredicate extends Predicate<String> {
   @Override
      public Predicate put(String expr, Boolean bool) {
         return super.put(new StringBuilder("(").append(expr).append(")").toString(), bool);
      }
}
