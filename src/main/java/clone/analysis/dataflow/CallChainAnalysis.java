package clone.analysis.dataflow;

import java.util.HashSet;
import java.util.Set;

import clone.analysis.CallChain;
import clone.analysis.CallChainSet;
import soot.Immediate;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.UnopExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.tagkit.StringTag;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.util.Chain;

// STEP 1: Decide whether it is a backwards or forwards analysis.
// BACKWARDS
//
// STEP 2: What are the lattice elements mappings between variables and possible signs for values they can hold
// SETS OF VARIABLES with corresponding sets of call chains
//
public class CallChainAnalysis extends ForwardFlowAnalysis<Unit, CallChainSet>
{
   public final Set<Value> localSet = new HashSet<Value>();

   private ConditionExpr conditionExpr = null;

   public CallChainAnalysis (UnitGraph g, Chain<Local> localChain)
   {
      super(g);
      this.localSet.addAll(localChain);
      doAnalysis();
   }

   private void resolveInvokeExpr(InvokeExpr invoke, CallChainSet outSet, Value assignedToValue)
   {
      if (invoke instanceof InstanceInvokeExpr) {
         Value base = ((InstanceInvokeExpr) invoke).getBase();
         Set<CallChain> callChainOutSet = outSet.getExistingChains(base);
         outSet.put(assignedToValue, CallChainSet.makeDeepCopy(callChainOutSet));
         outSet.appendToCallChains(assignedToValue, invoke);
         if (null != outSet.getSecondary(base))
            outSet.putSecondary(assignedToValue, outSet.getSecondary(base));
      }
      else {
         outSet.put(assignedToValue, outSet.getExistingChains(invoke));
      }

      if((invoke.getMethod().getDeclaringClass().getName().equals("junit.framework.TestCase") ||
                  invoke.getMethod().getDeclaringClass().getName().equals("junit.framework.Assert"))
               && invoke instanceof StaticInvokeExpr)
         return;
      for (int i = 0, n = invoke.getArgCount(); i < n; ++i) {
         Set<CallChain> callChainOutSet = outSet.getExistingSecondaryChains(invoke.getArg(i));
         for (int j = 0; j < n; ++j) {
            if (i == j)
               continue;
            if (null == outSet.get(invoke.getArg(j)))
               continue;
            callChainOutSet.addAll(outSet.get(invoke.getArg(j)));
         }
         if (callChainOutSet.size() > 1)
            outSet.putSecondary(invoke.getArg(i), callChainOutSet);
      }
   }

   private void resolveFeildRef(FieldRef fRef, CallChainSet outSet, Value assignedToValue)
   {
      if (fRef instanceof InstanceFieldRef) {
         Value base = ((InstanceFieldRef)fRef).getBase();
         Set<CallChain> callChainOutSet = outSet.getExistingChains(base);
         outSet.put(assignedToValue, callChainOutSet);
         for (CallChain cc : callChainOutSet)
            for (Value v : cc.getCalls())
               outSet.appendToCallChains(assignedToValue, v);
         outSet.appendToCallChains(assignedToValue, fRef);
         if (null != outSet.getSecondary(base))
            outSet.putSecondary(assignedToValue, outSet.getSecondary(base));
      }
      else {
         outSet.put(assignedToValue, outSet.getExistingChains(fRef));
      }
   }

   private void resolveValue(Value val, CallChainSet outSet, Value assignedToValue)
   {

      if (val instanceof Constant && conditionExpr != null)
         outSet.conditionExprMap.put(conditionExpr, assignedToValue);

      if (val instanceof Immediate)
         outSet.put(assignedToValue, outSet.getExistingChains(val));				

      if (val instanceof InvokeExpr)
         resolveInvokeExpr((InvokeExpr) val, outSet, assignedToValue);

      if (val instanceof FieldRef /*&& ((FieldRef)val).getField().isFinal()*/)
         resolveFeildRef((FieldRef) val, outSet, assignedToValue);				

      if (val instanceof CastExpr)
         resolveValue(((CastExpr) val).getOp(), outSet, assignedToValue);

      if (val instanceof UnopExpr)
         resolveValue(((UnopExpr) val).getOp(), outSet, assignedToValue);

      if (val instanceof BinopExpr) {
         BinopExpr binExpr = (BinopExpr) val;
         resolveValue(binExpr.getOp1(), outSet, assignedToValue);
         resolveValue(binExpr.getOp2(), outSet, assignedToValue);
      }

      if (val instanceof InstanceOfExpr) {
         outSet.put(assignedToValue, outSet.getExistingChains(((InstanceOfExpr)val).getOp()));				
         outSet.appendToCallChains(assignedToValue, StringConstant.v(((InstanceOfExpr)val).getCheckType().toString()));
      }

      if (val instanceof NewExpr) {
         outSet.put(assignedToValue, outSet.getExistingChains(val));				
      }

      if (val instanceof CaughtExceptionRef) {
         outSet.put(assignedToValue, outSet.getExistingChains(val));				
      }

   }


   public static boolean callChainSetsAreEqual(CallChainSet resolvedCallChains, CallChainSet callChainSet)
   {

      return resolvedCallChains.equals(callChainSet);
   }
   // STEP 3: What is the merge operator?
   // UNION
   protected void merge(CallChainSet in1, 
         CallChainSet in2, 
         CallChainSet out)
   {    
      copy(in1,out);
      if  (in1.equals(in2))
      {	
         return;
      }
      out.merge(in2);
   }

   // STEP 4: Define flow equations.
   protected void flowThrough(CallChainSet in, 
         Unit unit, 
         CallChainSet out)  
   {
      Stmt s = (Stmt)    unit;
      copy(in, out);

      //StringBuilder builder = new StringBuilder("[\nStmt type: ").append(s.getClass());
      if (s instanceof DefinitionStmt )
      {
         DefinitionStmt defStmt = (DefinitionStmt) s;
         resolveValue(defStmt.getRightOp(), out, defStmt.getLeftOp());
         //builder
         //   .append("\nLeftOp type: ")
         //   .append(defStmt.getLeftOp().getClass())
         //   .append("\nRightOp type: ")
         //   .append(defStmt.getRightOp().getClass());
      }

      if (s instanceof IfStmt)
         conditionExpr = (ConditionExpr)((IfStmt)s).getCondition();

      //builder.append("\nCallChainSet: \n").append(out);
      //s.addTag(new StringTag(builder.append("\n]").toString()));
   }

   protected void copy(CallChainSet source, CallChainSet destination) {
      destination.clearAll();	
      destination.deepCopy(source);
   }

   // STEP 5: Determine value for start/end node.
   // end node:              empty set
   protected CallChainSet entryInitialFlow()
   {
      CallChainSet ccS = new CallChainSet();
      for (Value var: localSet)
      {
         ccS.putEmptyCallChain(var);
         if(var instanceof Constant)
            ccS.appendToCallChains(var, StringConstant.v(var.getType().toString()));
      }
      return ccS;
   }
   // STEP 6: Initial approximation (bottom).
   // initial approximation: empty set
   protected CallChainSet newInitialFlow()
   {
      return entryInitialFlow();
   }
}
