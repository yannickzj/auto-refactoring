package clone.analysis.similarity;

import static clone.analysis.dataflow.CloneAnalysis.getExecutorService;
import static clone.analysis.similarity.ResultLogger.log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.fraction.Fraction;

import soot.G;
import clone.analysis.Method;
import clone.analysis.StaticCall;
import clone.analysis.database.DBUtil;
import clone.analysis.dataflow.CloneAnalysis;

import com.google.common.collect.HashMultimap;

public class GraphCloneFinder {

   private static final Map<Pair<Fraction, Integer>, CallChainGraphAnalysis<StaticCall>> staticCallGraphs =
      new HashMap<Pair<Fraction, Integer>, CallChainGraphAnalysis<StaticCall>>();

   private static final Map<Integer, StaticCallFlowGraphAnalysis> methodGraphs =
      new HashMap<Integer, StaticCallFlowGraphAnalysis>();

   private static final List<MatchSet<Method>> methodList = new ArrayList<MatchSet<Method>>();
   private static final Map<Set<String>, MatchSet<Method>> mergedMethodMap = new LinkedHashMap<Set<String>, MatchSet<Method>>();

   private static final Map<Pair<String, String>, Integer> totalStaticCallCounts = new HashMap<Pair<String, String>, Integer>();
   private static final Map<Pair<String, String>, Integer> totalStaticCallInCloneSetCounts = new TreeMap<Pair<String, String>, Integer>();

   private static final Map<String, Integer> totalMethodCounts = new HashMap<String, Integer>();
   private static final Map<String, Integer> totalMethodInCloneSetCounts = new TreeMap<String, Integer>();

   private static final List<StaticCallFlowMatchSet> testMethodList = new ArrayList<StaticCallFlowMatchSet>();

   public static void mergeStaticCalls(Grouping<StaticCall> grouping) throws Exception {
      long millisecondsStart, timeSpentInMilliseconds;
      millisecondsStart = System.currentTimeMillis();

      getExecutorService().invokeAll(staticCallGraphs.values());

      for (GraphAnalysis g : staticCallGraphs.values())
         g.group(grouping);

      timeSpentInMilliseconds = System.currentTimeMillis() - millisecondsStart;
      G.v().out.println("Executed mergeStaticCalls for " + (timeSpentInMilliseconds/1000) + "s");
   }

   public static void mergeMethods(Grouping<Method> grouping) throws Exception {
      long millisecondsStart, timeSpentInMilliseconds;
      millisecondsStart = System.currentTimeMillis();

      getExecutorService().invokeAll(methodGraphs.values());

      for (GraphAnalysis g : methodGraphs.values())
         g.group(grouping);

      timeSpentInMilliseconds = System.currentTimeMillis() - millisecondsStart;
      G.v().out.println("Executed mergeMethods for " + (timeSpentInMilliseconds/1000) + "s");
   }

   public static void addEdge(StaticCall a1, StaticCall a2, Pair<Fraction, Integer> matchFactor) {
      CallChainGraphAnalysis<StaticCall> g = staticCallGraphs.get(matchFactor);
      if(null == g)
      {
         g = new CallChainGraphAnalysis<StaticCall>(matchFactor.getLeft().doubleValue(), matchFactor.getRight());
         staticCallGraphs.put(matchFactor, g);
      }
      g.addVertex(a1);
      g.addVertex(a2);
      g.addEdge(a1, a2);
   }

   public static void addEdge(Method t1, Method t2, Integer editDistance) {
      StaticCallFlowGraphAnalysis g = methodGraphs.get(editDistance);
      if(null == g)
      {
         g = new StaticCallFlowGraphAnalysis(editDistance);
         methodGraphs.put(editDistance, g);
      }
      g.addVertex(t1);
      g.addVertex(t2);
      g.addEdge(t1, t2);
   }

   public static void aggregateStaticCalls(Set<MatchSet<StaticCall>> matchSet) {
      HashMultimap<String,StaticCall> list =  HashMultimap.create();
      for(Iterator<MatchSet<StaticCall>> iter = matchSet.iterator(); iter.hasNext(); )
      {
         CallChainMatchSet<StaticCall> s = (CallChainMatchSet<StaticCall>)iter.next();
         list.clear();
         for(StaticCall assertion: s)
            list.put(assertion.getQualifiedMethodName(), assertion);

         MatchSet<Method> m = new CallChainMatchSet<Method>(s.getScore(), s.getCcsize());
         for(Map.Entry<String, Collection<StaticCall>> e : list.asMap().entrySet())
         {
            Method method = new Method();
            for(StaticCall a: e.getValue())
               method.add(a);
            m.add(method);
         }
         if (m.size()<2) {
            iter.remove();
            continue;
         }
         methodList.add(m);
      }

      DBUtil.v().save(matchSet, CloneAnalysis.project);

      HashMultimap<Pair<String, String>,StaticCall> plist =  HashMultimap.create();
      for(MatchSet<StaticCall> s : matchSet)
      {
         for(StaticCall assertion: s)
            plist.put(Pair.of(assertion.getClassName(), assertion.getMethodName()), assertion);
      }
      for(Map.Entry<Pair<String, String>, Collection<StaticCall>> e : plist.asMap().entrySet())
      {
         totalStaticCallInCloneSetCounts.put(e.getKey(), e.getValue().size());
      }
   }

   public static void aggregateMethods(Set<MatchSet<Method>> matchSet) {
      HashMultimap<Set<String>, MatchSet<Method>> filteredMethods = HashMultimap.create();
      for (MatchSet<Method> mst : methodList) {
         Set<String> names = new HashSet<String>();
         for (Method tm : mst)
            names.add(tm.getQualifiedMethodName());
         filteredMethods.put(names, mst.compute());
      }

      for (MatchSet<Method> mst : matchSet) {
         StaticCallFlowMatchSet methodSet = (StaticCallFlowMatchSet)mst;
         if (methodSet.size() <= 1)
            continue;
         // Find relevant assertion clone sets
         Set<String> names = new HashSet<String>();
         for (Method tm : methodSet)
            names.add(tm.getQualifiedMethodName());

         for (Map.Entry<Set<String>, Collection<MatchSet<Method>>> entry : filteredMethods.asMap().entrySet()) {
            if (entry.getKey().containsAll(names) || names.containsAll(entry.getKey()))
               methodSet.addAllRelevantCloneSets(entry.getValue());
         }

         if (
               methodSet.getCentroidSize() > 1
            )
            testMethodList.add(methodSet);
      }

      Collections.sort(testMethodList);

      DBUtil.v().save(testMethodList, CloneAnalysis.project);
   }

   public static int computeSummary()
   {
      int val = 0;
      int sum1, sum2, count;

      // Assert sets by call chain
      MatchSet[] ms = methodList.toArray(new MatchSet[0]);
      Arrays.sort(ms);
      for(MatchSet<Method> m : ms)
         log(new StringBuilder()
               .append("[")
               .append(++val)
               .append("] ")
               .append(m));

      // All methods
      count = 0;
      ResultLogger.createLog(new File(CloneAnalysis.RESULT_LOCATION+"/all_methods").getAbsolutePath());

      for (Method tm : new TreeSet<Method>(CloneAnalysis.getTestMethods())) {
         StringBuilder builder = new StringBuilder()
            .append("[")
            .append(++count)
            .append("]")
            .append(tm.getLogLine());
         for (String name : tm.getStaticCallNames())
            builder.append("\n\t\t\t").append(name);
         builder.append("\n");
         log(builder);
      }

      ResultLogger.closeLog();

      // Method sets by assert flow
      count = 0;
      ResultLogger.createLog(new File(CloneAnalysis.RESULT_LOCATION+"/method_sets_by_assert_flow").getAbsolutePath());

      for (StaticCallFlowMatchSet methodSet : testMethodList)
         log(new StringBuilder()
               .append("[")
               .append(++count)
               .append("] ")
               .append(methodSet)
               .toString());

      ResultLogger.closeLog();

      // Methods in both results
      count = 0;
      ResultLogger.createLog(new File(CloneAnalysis.RESULT_LOCATION+"/methods_in_at_least_one_clone_set").getAbsolutePath());
      Set<String> names = new TreeSet<String>();

      for(MatchSet<Method> m : ms)
         for (Method tm : m)
            names.add(tm.toString());

      for (StaticCallFlowMatchSet methodSet : testMethodList)
         for (Method tm : methodSet)
            names.add(tm.toString());

      for (String s : names)
         log(new StringBuilder()
               .append("[")
               .append(++count)
               .append("] ")
               .append(s)
               .toString());

      ResultLogger.closeLog();


      // Pre-processing
      for (StaticCall a : CloneAnalysis.getAssertions()) {
         Pair<String, String> qualifiedMethodName = Pair.of(a.getClassName(), a.getMethodName());
         Integer c = totalStaticCallCounts.get(qualifiedMethodName);
         if (null == c)
            totalStaticCallCounts.put(qualifiedMethodName, 1);
         else
            totalStaticCallCounts.put(qualifiedMethodName, c+1);
      }

      // Method results
      Map<String, Method> mmMap = new HashMap<String, Method>();
      Map<String, Method> mMap = new HashMap<String, Method>();
      for(MatchSet<Method> m : ms) {
         Set<String> methodNames = new LinkedHashSet<String>();
         for (Method tm : m)
            methodNames.add(tm.getQualifiedMethodName());
         MatchSet<Method> mm = mergedMethodMap.get(methodNames);
         if (null == mm)
            mergedMethodMap.put(methodNames, m);
         else {
            mmMap.clear();
            mMap.clear();
            for (Method t : mm)
               mmMap.put(t.getQualifiedMethodName(), t);
            for (Method t : m)
               mMap.put(t.getQualifiedMethodName(), t);
            for (String name : methodNames)
               mmMap.get(name).getStaticCalls().addAll(mMap.get(name).getStaticCalls());
         }
      }
      ResultLogger.createLog(new File(CloneAnalysis.RESULT_LOCATION+"/method_results").getAbsolutePath());
      int val2 = 0;
      for (MatchSet<Method> m : mergedMethodMap.values())
         log(new StringBuilder()
               .append("[")
               .append(++val2)
               .append("] ")
               .append(m));
      ResultLogger.closeLog();

      String format = "%s: %s(%d) %s(%d) percentage(%.2f%%)";

      // Method stats
      sum1 = sum2 = count = 0;
      ResultLogger.createLog(new File(CloneAnalysis.RESULT_LOCATION+"/method_stats").getAbsolutePath());
      for (Map.Entry<Pair<String, String>, Integer> e : totalStaticCallInCloneSetCounts.entrySet()) {
         Integer c = totalStaticCallCounts.get(e.getKey());
         log(String.format(format,
                  new StringBuilder()
                  .append("[")
                  .append(++count)
                  .append("] ")
                  .append(e.getKey().getLeft())
                  .append(".")
                  .append(e.getKey().getRight())
                  .toString(),
                  "assertsInCloneSet", e.getValue(),
                  "asserts", c,
                  e.getValue()/(double)c*100));
         sum1 += e.getValue();
         sum2 += c;
      }
      log(String.format(format,
               "\nTotal", "assertsInCloneSet", sum1, "asserts", sum2, sum1/(double)sum2*100));
      if (count != 0)
         log(String.format(format,
                  "Average", "assertsInCloneSet", sum1/count, "asserts", sum2/count,
                  (sum1/count)/(double)(sum2/count)*100));
      ResultLogger.closeLog();

      // Class stats
      sum1 = sum2 = count = 0;
      ResultLogger.createLog(new File(CloneAnalysis.RESULT_LOCATION+"/class_stats").getAbsolutePath());

      for (Map.Entry<Pair<String, String>, Integer> e : totalStaticCallInCloneSetCounts.entrySet()) {
         Integer c = totalMethodInCloneSetCounts.get(e.getKey().getLeft());
         if (null == c)
            totalMethodInCloneSetCounts.put(e.getKey().getLeft(), 1);
         else
            totalMethodInCloneSetCounts.put(e.getKey().getLeft(), c+1);
      }

      for (Map.Entry<Pair<String, String>, Integer> e : totalStaticCallCounts.entrySet()) {
         Integer c = totalMethodCounts.get(e.getKey().getLeft());
         if (null == c)
            totalMethodCounts.put(e.getKey().getLeft(), 1);
         else
            totalMethodCounts.put(e.getKey().getLeft(), c+1);
      }

      for (Map.Entry<String, Integer> e : totalMethodInCloneSetCounts.entrySet()) {
         Integer c = totalMethodCounts.get(e.getKey());
         log(String.format(format,
                  new StringBuilder()
                  .append("[")
                  .append(++count)
                  .append("] ")
                  .append(e.getKey())
                  .toString(),
                  "methodsInCloneSet", e.getValue(),
                  "methods", c,
                  e.getValue()/(double)c*100));
         sum1 += e.getValue();
         sum2 += c;
      }

      log(String.format(format,
               "\nTotal", "methodsInCloneSet", sum1, "methods", sum2, sum1/(double)sum2*100));
      if (count != 0)
         log(String.format(format,
                  "Average", "methodsInCloneSet", sum1/count, "methods", sum2/count,
                  (sum1/count)/(double)(sum2/count)*100));
      ResultLogger.closeLog();

      return val;
   }

   public static void logSummary()
   {
      computeSummary();
   }
}
