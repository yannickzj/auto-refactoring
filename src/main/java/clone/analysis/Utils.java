package clone.analysis;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;

import soot.Unit;
import soot.SootMethod;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLnPosTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.DominatorsFinder;

public class Utils {
   public static int editDistance(String s1, String s2) {
      return editDistance(Arrays.asList(s1.toCharArray()), Arrays.asList(s2.toCharArray()));
   }

   public static int editDistance(List<? extends Object> l1, List<? extends Object> l2) {
      int len1 = l1.size();
      int len2 = l2.size();
      List<? extends Object> list1 = (len1 < len2 ? l1 : l2);
      List<? extends Object> list2 = (len1 < len2 ? l2 : l1);
      len1 = list1.size();
      len2 = list2.size();
      if (0 == len1)
         return len2;
      int [] dist = new int[len1];
      for (int i = 0; i < len1; ++i)
         dist[i] = i+1;

      for (int i = 0; i < len2; ++i) {
         int distDiag = i, distLeft = i + 1;
         for (int j = 0; j < len1; ++j) {
            int distUp = dist[j];
            dist[j] = list1.get(j).equals(list2.get(i)) ? 
               distDiag : 1 + Math.min(distDiag, Math.min(distLeft, distUp));
            distDiag = distUp;
            distLeft = dist[j];
         }
      }
      return dist[len1-1];
   }

   public static int getLineNumber(Stmt s) {
      Tag t = s.getTag("LineNumberTag");
      if(null != t)
         return ((LineNumberTag) t).getLineNumber();
      t = s.getTag("SourceLnPosTag");
      if(null != t)
         return ((SourceLnPosTag) t).startLn();
      return -1;
   }

   public static boolean inLoop(Unit root, UnitGraph g) {
      Deque<Unit> stack = new ArrayDeque<Unit>();
      Set<Unit> visited = new HashSet<Unit>();
      stack.push(root);
      while (!stack.isEmpty()) {
         visited.add(stack.peek());
         for (Unit u : g.getSuccsOf(stack.pop()))
            if (!visited.contains(u))
               stack.push(u);
            else if (u.equals(root))
               return true;
      }
      return false;
   }

   public static boolean inCatch(Unit target, Set<Unit> handlerNodes, DominatorsFinder dominatorsAnalysis) {
      Set<Unit> dominators = new HashSet<Unit>(dominatorsAnalysis.getDominators(target));
      dominators.retainAll(handlerNodes);
      return !dominators.isEmpty();
   }

   public static boolean isEmptySootMethod(SootMethod method) {
      return method.retrieveActiveBody().getUnits().isEmpty();
   }

   private static InvokeExpr getInvokeExpr(Stmt s) {
      InvokeExpr invokeExpr = null;
      if (s instanceof DefinitionStmt) {
         DefinitionStmt defStmt = (DefinitionStmt) s;
         if (defStmt.getRightOp() instanceof InvokeExpr)
            invokeExpr = (InvokeExpr)defStmt.getRightOp();
      }
      else if (s instanceof InvokeStmt) {
         invokeExpr = ((InvokeStmt)s).getInvokeExpr();
      }

      return invokeExpr;
   }

   public static boolean isNetworkAccess(Stmt s) {
      InvokeExpr invokeExpr = getInvokeExpr(s);

      return null != invokeExpr ?
         invokeExpr.getMethod().getDeclaringClass().toString().startsWith("java.net") :
         false;
   }

   public static boolean isFilesystemAccess(Stmt s) {
      InvokeExpr invokeExpr = getInvokeExpr(s);

      return null != invokeExpr ?
         invokeExpr.getMethod().getDeclaringClass().toString().startsWith("java.io.File") :
         false;
   }
}
