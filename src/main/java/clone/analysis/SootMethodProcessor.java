package clone.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import clone.analysis.branchedflow.StaticCallBranchedFlowAnalysis;
import clone.analysis.dataflow.CallChainAnalysis;
import clone.analysis.dataflow.CloneAnalysis;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.MHGDominatorsFinder;

public class SootMethodProcessor implements Matchable {

   private final SootMethod method;
   private final int localCount;

   public SootMethodProcessor(SootMethod method) {
      this.method = method;
      this.localCount = method.retrieveActiveBody().getLocalCount();
   }

   public List<Object> process() {
      List<Object> result = new ArrayList<Object>();

      Body mBody = method.retrieveActiveBody();
      UnitGraph unitGraph = new BriefUnitGraph(mBody);

      result.add(method);

      result.add(unitGraph);

      result.add(new StaticCallBranchedFlowAnalysis(unitGraph));

      result.add(new MHGDominatorsFinder<Unit>(unitGraph));

      result.add(new CallChainAnalysis(unitGraph, mBody.getLocals()));

      if (!CloneAnalysis.RUN_ON_TEST) {
         result.add(countStaticCalls());
      }

      return result;
   }

   private Map<SootMethod, Integer> countStaticCalls() {
      Map<SootMethod, Integer> staticCallsCount = new HashMap<SootMethod, Integer>();
      for (Unit u : method.retrieveActiveBody().getUnits()) {
         Stmt s = (Stmt)u;
         if (!s.containsInvokeExpr())
            continue;
         SootMethod invokeMethod = s.getInvokeExpr().getMethod();
         Integer count = staticCallsCount.get(invokeMethod);
         if (null != count)
            staticCallsCount.put(invokeMethod,  count + 1);
         else if (CloneAnalysis.staticCallsLookup.contains(invokeMethod))
            staticCallsCount.put(invokeMethod, 1);
      }

      return staticCallsCount;
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
      return localCount;
   }

   public int compareSize() {
      return 0;
   }

   public int compareTo(Matchable m) {
      return ((SootMethodProcessor)m).localCount - localCount;
   }
}
