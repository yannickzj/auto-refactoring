package clone.analysis;

public class SootMethodProcessorAnalysis extends Analysis<SootMethodProcessor> {
   public SootMethodProcessorAnalysis(SootMethodProcessor m) {
      super(m, null);
   }

   public Object call() {
      return m1.process();
   }
}
