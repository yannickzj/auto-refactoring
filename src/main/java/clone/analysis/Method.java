package clone.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Method implements Matchable {

   private final Set<StaticCall> staticCalls = new TreeSet<StaticCall>();
   private int controlFlowSize = -1;
   private int uniqueness = -1;

   @Transient
   private String qualifiedMethodName = "";
   @Transient
   private String packageName = "";
   @Transient
   private String className = "";

   @Transient
   private boolean staticCallsUpdated = false;
   @Transient
   private final List<String> staticCallNames = new ArrayList<String>();

   public static final Method EMPTY_METHOD = new Method();

   public Method(){
   }

   public void add(StaticCall a) {
      staticCalls.add(a);
      staticCallsUpdated = false;
      controlFlowSize = -1;
      uniqueness = -1;
   }

   public Set<StaticCall> getStaticCalls() {
      return staticCalls;
   }

   @Override
      public int size() {
         return staticCalls.size();
      }

   @Override
      public int compareSize() {
         return 0;
      }

   @Override 
      public String getPackageName() {
         if (packageName.equals("") && !staticCalls.isEmpty())
            packageName = staticCalls.iterator().next().getPackageName();
         return packageName;
      }

   @Override 
      public String getClassName() {
         if (className.equals("") && !staticCalls.isEmpty())
            className = staticCalls.iterator().next().getClassName();
         return className;
      }

   @Override 
      public String getQualifiedMethodName() {
         if (qualifiedMethodName.equals("") && !staticCalls.isEmpty())
            qualifiedMethodName = staticCalls.iterator().next().getQualifiedMethodName();
         return qualifiedMethodName;
      }

   @Override
      public String getSourceLine() {
         throw new RuntimeException("Methods do not have sourceLines");
      }

   @Override
      public int compareTo(Matchable o) {
         Method t = (Method)o;
         int [] cmps = { 
            getClassName().compareTo(t.getClassName()),
            t.staticCalls.size() - staticCalls.size()
         };
         for (Integer i : cmps)
            if (i != 0)
               return i;
         for (Iterator<StaticCall> iter1 = staticCalls.iterator(), iter2 = t.staticCalls.iterator(); iter1.hasNext() && iter2.hasNext();) {
            int cmp = iter1.next().getLineNum() - iter2.next().getLineNum();
            if (cmp != 0)
               return cmp;
         }
         return 0;
      }

   @Override
      public String getLogLine() {
         StringBuilder builder = new StringBuilder()
            .append("\n\t")
            .append(getQualifiedMethodName());
         for (StaticCall a : staticCalls)
            builder.append("\n\t\t").append(a.getLogLine());
         return builder.toString();
      }

   public String toString()
   {
      return getQualifiedMethodName();
   }

   public List<String> getStaticCallNames() {
      if (staticCallsUpdated)
         return staticCallNames;
      staticCallNames.clear();
      for (StaticCall a : staticCalls)
         staticCallNames.add(a.getStaticCallNameWithControlFlowStats());
      staticCallsUpdated = true;
      return staticCallNames;
   }

   public int getControlFlowSize() {
      if (controlFlowSize != -1)
         return controlFlowSize;
      controlFlowSize = 0;
      for (StaticCall sc : staticCalls)
         controlFlowSize += sc.getControlFlowSize();
      return controlFlowSize;
   }

   public int getUniqueness() {
      if (uniqueness != -1)
         return uniqueness;
      uniqueness = new HashSet(getStaticCallNames()).size();
      return uniqueness;
   }

   public Method compute() {
      getUniqueness();
      getControlFlowSize();
      return this;
   }
}
