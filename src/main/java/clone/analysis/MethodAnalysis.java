package clone.analysis;

import org.apache.commons.lang3.tuple.Triple;

public class MethodAnalysis extends Analysis<Method> {

	public static final int MAX_EDIT_DISTANCE = 0;

   public MethodAnalysis(Method m) {
      super(m, null);
   }

   public MethodAnalysis(Method m1, Method m2) {
      super(m1, m2);
   }

   public Object call() {
      if (null == m2)
         return m1.compute();
      int editDistance = 
         Utils.editDistance(m1.getStaticCallNames(), m2.getStaticCallNames());
      return editDistance <= MAX_EDIT_DISTANCE ? Triple.of(m1, m2, editDistance) : null;
   }
}
