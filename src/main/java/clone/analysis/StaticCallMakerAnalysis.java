package clone.analysis;

public class StaticCallMakerAnalysis extends Analysis<StaticCallMaker> {
   public StaticCallMakerAnalysis(StaticCallMaker m) {
      super(m, null);
   }

   public Object call() {
      return m1.make();
   }
}
