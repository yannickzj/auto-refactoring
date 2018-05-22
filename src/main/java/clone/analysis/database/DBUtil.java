package clone.analysis.database;

import java.util.Collection;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import clone.analysis.StaticCall;
import clone.analysis.Stats;
import clone.analysis.similarity.MatchSet;

import com.mongodb.MongoClient;

public class DBUtil {
   private MongoOperations mongoOperation = null;
   private static final String dbname = "ccanalysis";

   private static DBUtil instance = null;

   private DBUtil() {
      try {
         mongoOperation = new MongoTemplate(new MongoClient(), dbname);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static DBUtil v() {
      if (null == instance)
         instance = new DBUtil();
      return instance;
   }

   public boolean dropCollection(String collection) {
      if (null == mongoOperation)
         return false;
      mongoOperation.dropCollection(collection);
      return true;
   }

   public boolean save(Stats stats, String collection) {
      if (null == mongoOperation)
         return false;
      mongoOperation.insert(stats, collection);
      return true;
   }

   public boolean save(Collection<? extends MatchSet> methodSet, String collection) {
      if (null == mongoOperation)
         return false;
      mongoOperation.insert(methodSet, collection);
      return true;
   }
}
