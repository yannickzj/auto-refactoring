package clone.analysis.branchedflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;

import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

public class StaticCallBranchedFlowAnalysis extends ForwardBranchedFlowAnalysis<StringPredicate> {

   private final UnitGraph g;

   public StaticCallBranchedFlowAnalysis(UnitGraph g) {
      super(g);
      this.g = g;
      doAnalysis();
   }

   protected void flowThrough(StringPredicate in, Unit unit, List<StringPredicate> fallOut, List<StringPredicate> branchOuts) {
      Stmt s = (Stmt) unit;
      for (StringPredicate p : fallOut)
         copy(in, p);
      for (StringPredicate p : branchOuts)
         copy(in, p);

      if (s instanceof IfStmt) {
         IfStmt ifs = (IfStmt)s;
         for (StringPredicate p : branchOuts)
            p.put(ifs.getCondition().toString(), true);
         for (StringPredicate p : fallOut)
            p.put(ifs.getCondition().toString(), false);
      }

      /*
      System.out.println(s);
      System.out.println("in: " + in);
      System.out.println("fallOut: " + fallOut);
      System.out.println("branchOuts: " + branchOuts);
      System.out.println("succs: " + g.getSuccsOf(unit));
      System.out.println();
      */
   }

   protected void copy(StringPredicate source, StringPredicate dest) {
      dest.copy(source);
   }

   protected void merge(StringPredicate in1, StringPredicate in2, StringPredicate out) {
      out.copy(in1).merge(in2);
   }

   protected StringPredicate entryInitialFlow()
   {
      return new StringPredicate();
   }

   protected StringPredicate newInitialFlow()
   {
      return new StringPredicate();
   }
}
