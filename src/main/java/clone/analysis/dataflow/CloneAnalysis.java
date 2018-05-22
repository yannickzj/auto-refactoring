package clone.analysis.dataflow;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import soot.Body;
import soot.BooleanType;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.ConditionExpr;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.tagkit.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.MHGDominatorsFinder;
import clone.analysis.CallChain;
import clone.analysis.CallChainSet;
import clone.analysis.Method;
import clone.analysis.SootMethodProcessor;
import clone.analysis.StaticCall;
import clone.analysis.StaticCallAnalysis;
import clone.analysis.StaticCallMaker;
import clone.analysis.Stats;
import clone.analysis.Utils;
import clone.analysis.branchedflow.Predicate;
import clone.analysis.branchedflow.StaticCallBranchedFlowAnalysis;
import clone.analysis.database.DBUtil;
import clone.analysis.similarity.StaticCallAnalysisCompletionHandler;
import clone.analysis.similarity.StaticCallMakerAnalysisCompletionHandler;
import clone.analysis.similarity.GraphCloneFinder;
import clone.analysis.similarity.Grouping;
import clone.analysis.similarity.ResultLogger;
import clone.analysis.similarity.SimilarityAnalysis;
import clone.analysis.similarity.SootMethodProcessorAnalysisCompletionHandler;
import clone.analysis.similarity.MethodAnalysisCompletionHandler;

import com.google.common.collect.HashMultimap;

/**
 * A scene transformer that records variable asspertion protocol
 * information in tags.
 */
public class CloneAnalysis extends SceneTransformer {

    public static String RESULT_LOCATION = "results/";
    public static final String CACHE_LOCATION = "cache/";
    public static boolean DUMP_STATS = false;
    private static boolean WRITE_CACHE = true;
    private static boolean USE_CACHE = false;
    private static final Set<SootClass> testClassList = new HashSet<SootClass>();
    public static final boolean GENERATE_GRAPH = false;

    public static final boolean RUN_ON_TEST = true;

    public static final boolean ENABLE_CC = true;
    public static final boolean ENABLE_AF = true;

    public static final int STATIC_CALL_SIZE_THRESHOLD = 5;
    public static final int CONTROL_FLOW_THRESHOLD = 1;
    public static final int STATIC_CALL_UNIQUENESS_THRESHOLD = 2;

    public static String project = "$PROJECT";

    public static final String junitTestCaseClassName = "org.junit.Assert";
    //public static final String junitTestCaseClassName = "junit.framework.TestCase";

    public static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    private static final int MAX_TASK_NUM = 32;

    private static final ThreadPoolExecutor excutorService =
            new ThreadPoolExecutor(NUM_CORES, NUM_CORES, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(MAX_TASK_NUM),
                    new ThreadPoolExecutor.CallerRunsPolicy());


    private static final List<StaticCall> asserts = new ArrayList<StaticCall>();
    private static final List<Method> methods = new ArrayList<Method>();
    private static final Map<String, Method> methodMap = new HashMap<String, Method>();
    private static final List<StaticCallMaker> staticCallMakers = new ArrayList<StaticCallMaker>();

    private static final HashMultimap<SootMethod, Unit> handlerNodes = HashMultimap.create();

    private static final List<SootMethodProcessor> sootMethodProcessors = new ArrayList<SootMethodProcessor>();

    public static final List<List<Object>> analysisResults = new ArrayList<List<Object>>();

    public static ExecutorService getExecutorService() {
        return excutorService;
    }

    public static List<StaticCall> getAssertions() {
        return asserts;
    }

    public static List<Method> getTestMethods() {
        return methods;
    }

    public static void addAssertion(StaticCall a) {
        asserts.add(a);
        Method m = methodMap.get(a.getQualifiedMethodName());
        if (null == m) {
            m = new Method();
            methodMap.put(a.getQualifiedMethodName(), m);
        }
        m.add(a);
    }

    public static void addCatchBlock(SootMethod method, Unit handlerNode) {
        if (null == handlerNode)
            return;
        handlerNodes.put(method, handlerNode);
    }

    public static Set<Unit> getHandlerNodes(SootMethod method) {
        return handlerNodes.get(method);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Syntax: java " +
                    "SignAnalysisMain mainClass " +
                    "[soot options]");
            System.exit(0);
        }

        final List<String> sootArgsLst = new ArrayList<String>();
        final List<String> similarityArgs = new ArrayList<String>();
        for (int i = 0; i < args.length; i++)
            if (args[i].equals("--similarityargs")) {
                for (int j = i + 1; j < args.length; j++) {
                    similarityArgs.add(args[j]);
                }
                break;
            } else {
                sootArgsLst.add(args[i]);
            }

        for (String s : similarityArgs) {
            if (s.contains("writecache"))
                WRITE_CACHE = true;
            if (s.contains("usecache"))
                USE_CACHE = true;
            if (s.startsWith("project=")) {
                project = s.replace("project=", "");
            }
            if (s.startsWith("min="))
                StaticCallAnalysis.MATCH_FACTOR_MIN = Float.parseFloat(s.replace("min=", ""));
            if (s.startsWith("result_location="))
                RESULT_LOCATION = s.replace("result_location=", "") + "/" + project;
        }

        String[] sootArgs = new String[1];
        sootArgs = sootArgsLst.toArray(sootArgs);


        G.v().out.println("Soot arguments:");
        for (int i = 0; i < sootArgs.length; i++)
            G.v().out.print(sootArgs[i] + " ");

        new File(RESULT_LOCATION).mkdirs();
        if (GENERATE_GRAPH) {
            File graph_dir = new File(RESULT_LOCATION + "/graph");
            if (graph_dir.exists()) {
                for (String s : graph_dir.list())
                    if (s.substring(s.lastIndexOf('.') + 1).equals("dot"))
                        new File(graph_dir.getPath(), s).delete();
            } else {
                graph_dir.mkdirs();
            }
        }
        ResultLogger.createLog(new File(RESULT_LOCATION + "/results").getAbsolutePath());

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.signs", new CloneAnalysis()));

        // Call main
        soot.Main.main(sootArgs);

        ResultLogger.closeLog();
        excutorService.shutdown();
    }

    public static final Set<SootMethod> staticCallsLookup = new HashSet<SootMethod>();

    private static void generateTestClassList() {
        final SootClass junitTestCaseClass = Scene.v().getSootClass(junitTestCaseClassName);

        //testClassList.addAll(Scene.v().getActiveHierarchy().getSubclassesOfIncluding(junitTestCaseClass));
        testClassList.addAll(Scene.v().getApplicationClasses());

        G.v().out.println("testClassList size:" + testClassList.size());

        if (RUN_ON_TEST) {
            for (SootMethod sm : junitTestCaseClass.getMethods())
                if (sm.isPublic() && sm.isStatic() && sm.isConcrete() && !sm.isMain())
                    staticCallsLookup.add(sm);
        } else {
            for (SootClass sc : Scene.v().getClasses())
                for (SootMethod sm : sc.getMethods())
                    if (sm.isPublic() && sm.isStatic() && sm.isConcrete() && !sm.isMain())
                        staticCallsLookup.add(sm);
        }
    }

    private static boolean directlyInheritsTestCase(SootClass sc) {
        final SootClass junitTestCaseClass = Scene.v().getSootClass(junitTestCaseClassName);

        List cl = Scene.v().getActiveHierarchy().getSuperclassesOf(sc);

        return cl.contains(junitTestCaseClass) && cl.get(0).equals(junitTestCaseClass);
    }

    private static final String[] testCaseMethodNames = {"setUp", "tearDown"};

    private static boolean containsNonEmptySetupOrTeardown(SootClass sc) {
        List emptyList = new LinkedList();
        for (String name : testCaseMethodNames) {
            try {
                SootMethod sm = sc.getMethod(name, emptyList, VoidType.v());
                if (Utils.isEmptySootMethod(sm))
                    continue;
                return true;
            } catch (final Exception e) {
                continue;
            }
        }
        return false;
    }

    void runSimilarityAnalysis() {
        try {
            if (ENABLE_CC || ENABLE_AF) {
                SimilarityAnalysis.analyze(staticCallMakers, new StaticCallMakerAnalysisCompletionHandler(), false);
                staticCallMakers.clear();
                DBUtil.v().dropCollection(project);
            }


            if (ENABLE_CC) {
                SimilarityAnalysis.analyze(asserts, new StaticCallAnalysisCompletionHandler(), true);
                Grouping<StaticCall> assertionGrouping = new Grouping<StaticCall>();
                GraphCloneFinder.mergeStaticCalls(assertionGrouping);
                //assertionGrouping.display();
                GraphCloneFinder.aggregateStaticCalls(assertionGrouping.getList());
            }

            if (ENABLE_AF) {
                methods.addAll(methodMap.values());
                SimilarityAnalysis.analyze(methods, null, false);
                SimilarityAnalysis.analyze(methods, new MethodAnalysisCompletionHandler(), true, true);
                Grouping<Method> methodGrouping = new Grouping<Method>();
                GraphCloneFinder.mergeMethods(methodGrouping);
                //methodGrouping.display();
                GraphCloneFinder.aggregateMethods(methodGrouping.getList());
            }

            if (ENABLE_CC || ENABLE_AF) {
                GraphCloneFinder.logSummary();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final Map<SootMethod, Map<SootMethod, Integer>> staticCallsCounts = new HashMap<SootMethod, Map<SootMethod, Integer>>();

    protected void internalTransform(String phaseName, Map options) {
        Scene.v().loadNecessaryClasses();
        generateTestClassList();

        final Set<SootClass> classNames = new HashSet<SootClass>();
        final Set<String> packageNames = new HashSet<String>();

        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (!isInterestingClass(sc))
                continue;
            //G.v().out.println("interesting class: " + sc.getName());
            for (SootMethod aMethod : sc.getMethods()) {
                if (!isInterestingMethod(aMethod))
                    continue;

                ++Stats.v().totalMethods;
                classNames.add(sc);
                packageNames.add(sc.getPackageName());

                CallChain.allLocals.putAll(aMethod, aMethod.retrieveActiveBody().getLocals());

                sootMethodProcessors.add(new SootMethodProcessor(aMethod));
            }
        }

        G.v().out.println("totalMethods: " + Stats.v().totalMethods);

        Stats.v().totalClasses = classNames.size();
        Stats.v().totalPackages = packageNames.size();
        for (SootClass sc : classNames) {
            if (!directlyInheritsTestCase(sc))
                ++Stats.v().totalClassesNotDirectlyInheritingTestCase;
            if (containsNonEmptySetupOrTeardown(sc))
                ++Stats.v().totalClassesContainingNonEmptySetupOrTeardown;
        }

        try {
            SimilarityAnalysis.analyze(sootMethodProcessors, new SootMethodProcessorAnalysisCompletionHandler(), false);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        sootMethodProcessors.clear();

        // TODO
        if (!RUN_ON_TEST) {
            for (List<Object> items : analysisResults) {
                SootMethod method = (SootMethod) items.get(0);
                Map<SootMethod, Integer> staticCallsCount = (Map<SootMethod, Integer>) items.get(5);
                staticCallsCounts.put(method, staticCallsCount);
            }
        }

	  /*
      for (SootMethod sm : staticCallsLookup) {
		G.v().out.println("staticCallsLookup contains:" + sm.getName());
	  }
	  */

        for (List<Object> items : analysisResults) {

            SootMethod method = (SootMethod) items.get(0);

            UnitGraph unitGraph = (UnitGraph) items.get(1);

            StaticCallBranchedFlowAnalysis assertionBranchedFlowAnalysis = (StaticCallBranchedFlowAnalysis) items.get(2);

            DominatorsFinder<Unit> dominatorsAnalysis = (DominatorsFinder<Unit>) items.get(3);

            CallChainAnalysis assertionProtocolAnalysis = (CallChainAnalysis) items.get(4);

            ExceptionalUnitGraph exceptionalUnitGraph = new ExceptionalUnitGraph(method.retrieveActiveBody());

            // TODO: aggregate counts and analyze

            //G.v().out.println("Analyzing: "+ aMethod.toString());

            boolean hasBranches = false;
            boolean hasLoops = false;
            boolean hasNetworkAccess = false;
            boolean hasFilesystemAccess = false;
            for (Unit u : method.retrieveActiveBody().getUnits()) {
                Stmt s = (Stmt) u;

                /*** Stats ***/
                if (!hasBranches)
                    if (s instanceof IfStmt || s instanceof LookupSwitchStmt || s instanceof TableSwitchStmt) {
                        ++Stats.v().totalMethodsWithBranches;
                        hasBranches = true;
                    }

                if (!hasLoops)
                    if (Utils.inLoop(u, unitGraph)) {
                        ++Stats.v().totalMethodsWithLoops;
                        hasLoops = true;
                    }

                if (!hasNetworkAccess)
                    if (Utils.isNetworkAccess(s)) {
                        ++Stats.v().totalMethodsWithNetworkAccess;
                        hasNetworkAccess = true;
                    }

                if (!hasFilesystemAccess)
                    if (Utils.isFilesystemAccess(s)) {
                        ++Stats.v().totalMethodsWithFilesystemAccess;
                        hasFilesystemAccess = true;
                    }
                /*** END ***/

                // Add StringTags listing of signs
                //G.v().out.println("Analyzing Statement: "+s.toString());

            /*   	G.v().out.print("\nFollowSet Contains: ");
                  for (Value v : variableProtocols.keySet())
                  G.v().out.print(v);*/
            /*Iterator<Entry<Value, Set<CallChain>>> variableIt = variableProtocols.getIterator();
              while( variableIt.hasNext() ) {

              final Entry<Value, Set<CallChain >> value = variableIt.next();
              String stTag = "Call Chains for: "+ value.getKey().toString() + " :\n";
              for (CallChain potentialCallChain: value.getValue())
              {
              stTag = stTag+potentialCallChain.toString();
              }
            //	G.v().out.println("## ==> " + stTag);
            StringTag t = new StringTag( stTag );
            //	s.addTag( t );
            }*/


                //s.addTag(new StringTag("unitGraph.Preds: " + unitGraph.getPredsOf(s)));
                //s.addTag(new StringTag("unitGraph.Succs: " + unitGraph.getSuccsOf(s)));
                //s.addTag(new StringTag("Preds: " + exceptionalUnitGraph.getPredsOf(s)));
                //s.addTag(new StringTag("Succs: " + exceptionalUnitGraph.getSuccsOf(s)));
                //s.addTag(new StringTag("ExceptionalPreds: " + exceptionalUnitGraph.getExceptionalPredsOf(s)));
                //s.addTag(new StringTag("ExceptionalSuccs: " + exceptionalUnitGraph.getExceptionalSuccsOf(s)));
                //s.addTag(new StringTag("UnexceptionalPreds: " + exceptionalUnitGraph.getUnexceptionalPredsOf(s)));
                //s.addTag(new StringTag("UnexceptionalSuccs: " + exceptionalUnitGraph.getUnexceptionalSuccsOf(s)));
                //s.addTag(new StringTag("ExceptionDests: " + exceptionalUnitGraph.getExceptionDests(s)));

                //s.addTag(new StringTag("Dominators: " + dominatorsAnalysis.getDominators(s)));

                for (ExceptionalUnitGraph.ExceptionDest ed : exceptionalUnitGraph.getExceptionDests(s))
                    addCatchBlock(method, ed.getHandlerNode());

                if (!s.containsInvokeExpr())
                    continue;

                InvokeExpr invkExpr = s.getInvokeExpr();

                if (!staticCallsLookup.contains(invkExpr.getMethod())) {
                    continue;
                }

                CallChainSet variableProtocols = assertionProtocolAnalysis.getFlowAfter(s);

                Predicate predicate = assertionBranchedFlowAnalysis.getFlowBefore(s);

                //	G.v().out.println("\nJUNIT.ASSERT: "+invkMeth.getDeclaringClass().getName().toString());
                int line = Utils.getLineNumber(s);

                CallChainSet callChains = new CallChainSet();

                variableProtocols.reverseConditionExprMapping();
                for (Value variable : invkExpr.getArgs()) {
                    Set<CallChain> x = variableProtocols.getExistingChains(variable);
                    callChains.put(variable, x);
                    Set<CallChain> xx = variableProtocols.getSecondary(variable);
                    if (null != xx)
                        callChains.putSecondary(variable, xx);

                    StringBuilder str_tag_builder = new StringBuilder("VAR: ")
                            .append(variable)
                            .append("\nCALLCHAIN: ")
                            .append(x.toString());
                    if (null != xx)
                        str_tag_builder.append("\nSECONDARY_CALLCHAIN: ").append(xx.toString());
                    StringTag tag = new StringTag(str_tag_builder.toString());
                    s.addTag(tag);

                    if (!(variable.getType() instanceof BooleanType))
                        continue;
                    ConditionExpr conditionExpr = variableProtocols.reverseConditionExprMap.get(variable);
                    if (null == conditionExpr)
                        continue;
                    Value[] ops = {conditionExpr.getOp1(), conditionExpr.getOp2()};
                    for (Value op : ops) {
                        for (CallChain cc : variableProtocols.getExistingChains(op))
                            for (Value v : cc.getCalls())
                                variableProtocols.appendToCallChains(variable, v);
                        variableProtocols.appendToCallChains(variable, op);
                    }
                }

                staticCallMakers.add(new StaticCallMaker(s, unitGraph, exceptionalUnitGraph, line, callChains.compute(), method, predicate, dominatorsAnalysis));


            }
        }
        analysisResults.clear();

        long millisecondsStart, timeSpentInMilliseconds;
        millisecondsStart = System.currentTimeMillis();
        runSimilarityAnalysis();
        timeSpentInMilliseconds = System.currentTimeMillis() - millisecondsStart;
        G.v().out.println("Execution time for runSimilarityAnalysis: " + (timeSpentInMilliseconds / 1000) + "s");

        Stats.v().totalStaticCalls = asserts.size();
        for (Method m : methods)
            if (m.size() >= STATIC_CALL_SIZE_THRESHOLD)
                ++Stats.v().totalMethodsWithAtLeast5Asserts;
        Stats.v().totalMethodsWithNoStaticCalls = Stats.v().totalMethods - methods.size();

        Stats.v().save();
    }

    private static final String[] skippedClasses = {"java.", "sun.", "javax.", "com.sun.", "org.xml.", "org.apache.kafka.test."};

    private static final String testAnnotationType = "Lorg/junit/Test;";

    private boolean isInterestingClass(SootClass aClass) {
        for (String sClass : skippedClasses)
            if (aClass.getName().startsWith(sClass))
                return false;
        if (aClass.isJavaLibraryClass() || aClass.isLibraryClass())
            return false;

        if (RUN_ON_TEST)
            if (testClassList.contains(aClass)) {
                //G.v().out.println("RUN_ON_TEST");
                //G.v().out.println("interestingClass contains: " + aClass.getName());
                return true;
            } else if (!testClassList.contains(aClass)) {
                //G.v().out.println("RUN_NOT_ON_TEST");
                //G.v().out.println("interestingClass contains: " + aClass.getName());
                return true;
            }

        return false;
    }

    private boolean isInterestingMethod(SootMethod aMethod) {
        if (aMethod.isConcrete()) {
            if (RUN_ON_TEST) {
                /*
                for (Tag tag: aMethod.getTags()) {
                    if (tag instanceof VisibilityAnnotationTag && containsTestAnnotation((VisibilityAnnotationTag) tag)) {
                        return true;
                    }
                }
                */
                return aMethod.getName().startsWith("test") || aMethod.getName().startsWith("should");
            }
        }
        return false;
    }

    private boolean containsTestAnnotation(VisibilityAnnotationTag vTag) {
        for (AnnotationTag aTag: vTag.getAnnotations()) {
            if (isTestAnnotation(aTag)) {
                //System.out.println(aTag.getType());
                return true;
            }
        }
        return false;
    }

    private boolean isTestAnnotation(AnnotationTag aTag) {
        return aTag.getType().equals(testAnnotationType);
    }
}
