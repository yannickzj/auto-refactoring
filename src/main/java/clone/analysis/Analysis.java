package clone.analysis;

import java.util.concurrent.Callable;

public abstract class Analysis<T extends Matchable> implements Callable<Object> {

   protected final T m1;
   protected final T m2;

   public Analysis(T m1, T m2) {
      this.m1 = m1;
      this.m2 = m2;
   }
   
   public static Analysis getAnalysis(Matchable p) {
      if (p instanceof StaticCallMaker)
         return new StaticCallMakerAnalysis((StaticCallMaker)p);
      if (p instanceof SootMethodProcessor)
         return new SootMethodProcessorAnalysis((SootMethodProcessor)p);
      if (p instanceof Method)
         return new MethodAnalysis((Method)p);
      return null;
   }

   public static Analysis getAnalysis(Matchable p1, Matchable p2) {
      if (null == p1 || null == p2 || p1.compareSize() != p2.compareSize())
         return null;
      if (p1 instanceof StaticCall && p2 instanceof StaticCall)
         return new StaticCallAnalysis((StaticCall)p1, (StaticCall)p2);
      if (p1 instanceof Method && p2 instanceof Method)
         return new MethodAnalysis((Method)p1, (Method)p2);
      return null;
   }
}
