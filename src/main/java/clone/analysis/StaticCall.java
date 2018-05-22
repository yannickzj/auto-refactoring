package clone.analysis;

import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import clone.analysis.branchedflow.Predicate;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;

@Document
public class StaticCall implements Matchable {

	protected final String staticCallName;
	protected final String packageName;
	protected final String className;
	protected final String methodName;
	protected final int lineNum;
	protected final double methodInvolvementRatio;

	protected final Predicate predicate;
	protected final boolean inLoop;
	protected final boolean inCatch;
	protected final int exceptionalSuccsCount;

   @Transient
	protected final InvokeExpr invoke;
   @Transient
	protected final String qualifiedMethodName;

   @Transient
	private final CallChainSet callChains;
   @Transient
   private final int size;
   @Transient
   private final List<Unit> exceptionalSuccs;

   public StaticCall (InvokeExpr invoke, int lineNum, CallChainSet callChains, SootMethod testMeth, Predicate predicate, boolean inLoop, List<Unit> exceptionalSuccs, boolean inCatch)
   {
      methodName = testMeth.getSubSignature();
      this.invoke = invoke;
      staticCallName = invoke.getMethod().toString();
      this.lineNum = lineNum;
      this.packageName = testMeth.getDeclaringClass().getPackageName();
      this.className = testMeth.getDeclaringClass().getName();
      this.callChains = new CallChainSet(callChains);
      qualifiedMethodName = testMeth.getSignature();
      size = callChains.totalSize();
      methodInvolvementRatio = callChains.getAllValues().size()/(double)CallChain.allLocals.get(testMeth).size();
      this.predicate = predicate;
      this.inLoop = inLoop;
      this.inCatch = inCatch;
      this.exceptionalSuccsCount = exceptionalSuccs.size();
      this.exceptionalSuccs = exceptionalSuccs;
   }

   public int getLineNum() {
      return lineNum;
   }

   public int getControlFlowSize() {
      return predicate.size() + (inLoop ? 1 : 0) + (inCatch ? 1 : 0) + exceptionalSuccsCount;
   }

   @Override 
      public String getPackageName() {
         return packageName;
      }

   @Override 
      public String getClassName() {
         return className;
      }

   public String getMethodName() {
      return methodName;
   }

   public String getStaticCallName() {
      return staticCallName;
   }

   public String getStaticCallNameWithControlFlowStats() {
      return new StringBuilder(staticCallName)
         .append(" (bools:")
         .append(predicate.size() - predicate.getNullCount())
         .append(", nulls:")
         .append(predicate.getNullCount())
         .append(", inLoop:")
         .append(inLoop)
         .append(", inCatch:")
         .append(inCatch)
         .append(", exceptionalSuccsSize:")
         .append(exceptionalSuccsCount)
         .append(")")
         .toString();
   }

   public String getSourceLineWithControlFlow() {
      return new StringBuilder(getSourceLine())
         .append(" (predicates:")
         .append(predicate)
         .append(", inLoop:")
         .append(inLoop)
         .append(", inCatch:")
         .append(inCatch)
         .append(", exceptionalSuccs:")
         .append(exceptionalSuccs)
         .append(")")
         .toString();
   }

   public CallChainSet getCallChains() {
      return callChains;
   }

   public List<Value> getArgs() {
      return invoke.getArgs();
   }

   public int getArgCount() {
      return invoke.getArgCount();
   }

   public InvokeExpr getInvokeExpr() {
      return invoke;
   }

   @Override
      public int size() {
         return size;
      }

   @Override
      public int compareSize() {
         return getArgCount();
      }

   @Override
      public String getQualifiedMethodName()
      {
         return qualifiedMethodName;
      }

   public String getSourceLine()
   {
      return String.format(
            "<%s.java:%d, %.2f%%>",
            this.className,
            this.lineNum,
            methodInvolvementRatio*100
            );
   }

   public String toString()
   {
      return getSourceLine();
   }

   public String toStringDetails()
   {
      return new StringBuilder("For assertion at: ").append(getSourceLine()).append("\n")
         .append("invoke: ").append(invoke).append("\n")
         .append("method: ").append(methodName).append("\n") 
         .append("type: ").append(staticCallName).append("\n")
         .append(callChains).toString();
   }

   @Override
      public int compareTo(Matchable o) {
         StaticCall a = (StaticCall)o;
         int cmp = qualifiedMethodName.compareTo(a.qualifiedMethodName);
         return cmp != 0 ? cmp : lineNum - a.lineNum;
      }

   @Override
      public String getLogLine() {
         //return getSourceLine();
         return getSourceLineWithControlFlow();
      }
}
