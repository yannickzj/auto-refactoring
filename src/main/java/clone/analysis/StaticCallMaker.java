package clone.analysis;

import clone.analysis.branchedflow.Predicate;
import clone.analysis.dataflow.CloneAnalysis;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.DominatorsFinder;

public class StaticCallMaker implements Matchable {
   private final Stmt s;
   private final UnitGraph g;
   private final ExceptionalUnitGraph eg;
   private final int lineNum;
   private final CallChainSet callChains;
   private final SootMethod testMeth;
   private final Predicate predicate;
   private final int makerSize;
   private final DominatorsFinder dominatorsAnalysis;

   public StaticCallMaker (Stmt s, UnitGraph g, ExceptionalUnitGraph eg, int lineNum, CallChainSet callChains, SootMethod testMeth, Predicate predicate, DominatorsFinder dominatorsAnalysis) {
      this.s = s;
      this.g = g;
      this.eg = eg;
      this.lineNum = lineNum;
      this.callChains = callChains;
      this.testMeth = testMeth;
      this.predicate = predicate;
      this.makerSize = g.size();
      this.dominatorsAnalysis = dominatorsAnalysis;
   }

   public StaticCall make() {
      return new StaticCall(s.getInvokeExpr(), lineNum, callChains, testMeth, predicate.update(), Utils.inLoop(s, g), 
            eg.getExceptionalSuccsOf(s), Utils.inCatch(s, CloneAnalysis.getHandlerNodes(testMeth), dominatorsAnalysis));
   }

	public String getPackageName() {
      return "";
   }

	public String getClassName() {
      return "";
   }

	public String getQualifiedMethodName() {
      return "";
   }

	public String getSourceLine() {
      return "";
   }

	public String getLogLine() {
      return "";
   }

	public int size() {
      return makerSize;
   }

	public int compareSize() {
      return 0;
   }

   public int compareTo(Matchable m) {
      return ((StaticCallMaker)m).makerSize - makerSize;
   }
}
