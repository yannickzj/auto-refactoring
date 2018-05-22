package clone.analysis;

import org.springframework.data.mongodb.core.mapping.Document;

import clone.analysis.database.DBUtil;
import clone.analysis.dataflow.CloneAnalysis;

@Document
public class Stats {

   public int totalStaticCalls = 0;
   public int totalMethods = 0;
   public int totalClasses = 0;
   public int totalPackages = 0;

   public int totalClassesNotDirectlyInheritingTestCase = 0;
   public int totalClassesContainingNonEmptySetupOrTeardown = 0;

   public int totalMethodsWithAtLeast5Asserts = 0;
   public int totalMethodsWithBranches = 0;
   public int totalMethodsWithLoops = 0;


   public int totalMethodsWithNoStaticCalls = 0;

   public int totalMethodsWithNetworkAccess = 0;
   public int totalMethodsWithFilesystemAccess = 0;

   private static Stats instance = null;

   private Stats() {
   }

   public static Stats v() {
      if (null == instance)
         instance = new Stats();
      return instance;
   }

   public boolean save() {
      return DBUtil.v().save(instance, CloneAnalysis.project);
   }
}
