package clone.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.fraction.Fraction;

import soot.G;
import soot.Hierarchy;
import soot.SootMethod;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.StaticInvokeExpr;

import com.google.common.collect.HashMultimap;

public class CallChain {

   public static final char SPLITTER = '%';
   private static final Hierarchy h = new Hierarchy();
    
	private final List<Value>calls = new ArrayList<Value>();

	private static final HashMultimap<Value, SootMethod> resolveList =  HashMultimap.create();

	public static final HashMultimap<SootMethod, Value> allLocals = HashMultimap.create();

	public CallChain(List<Value> callChain, Value valueString) {
		calls.addAll(callChain);
	}
	public CallChain(){		
	}

	public CallChain(CallChain cc) {
		for (Value v: cc.calls)
			add(v);
	}
	
	public CallChain(Value v) {
		calls.add(v);
	}

   public int size() {
      return calls.size();
   }

   public Collection<Value> getCalls() {
      return calls;
   }

	public void add(Value call)
	{
		assert call != null;
		if(!calls.contains(call)) {
			calls.add(call);
		}
	}

   public CallChain compute() {
      for (Value call : calls)
         if (call instanceof InstanceInvokeExpr) {
            InstanceInvokeExpr invokeExpr = (InstanceInvokeExpr) call;
            SootMethod invokeMeth = invokeExpr.getMethod();

            try {
               resolveList.putAll(call, (List<SootMethod>) h.resolveAbstractDispatch(invokeMeth.getDeclaringClass(), invokeMeth));
            } catch (Exception e) {
               System.out.println(invokeMeth.getDavaDeclaration() + invokeMeth.toString());
               continue;
               //throw e;
            }
            if (resolveList.get(call).isEmpty())
               resolveList.put(call, invokeMeth);
         }
      return this;
   }

	public Fraction match(CallChain callchain)
	{
	 	if(callchain.calls.size() != this.calls.size())
         return new Fraction(0);
      int i = 0, n = calls.size(), dividingFactor = 0, f = 0;
outerloop:
      for(; i < n; ++i)
      {
         int contributiveFactor = i; //or i if doing weighted addition
         dividingFactor+=contributiveFactor;
         Value thisElement = this.calls.get(i);
         Value otherElement = callchain.calls.get(i);

         //If the elements are actually equivalent(highly unlikely) 
         if(thisElement.equivTo(otherElement))
         {
            f+=contributiveFactor;
            continue;
         }

         //If both are constants
         if (thisElement instanceof Constant && otherElement instanceof Constant)
         {
            Constant thisConst = (Constant) thisElement;
            Constant otherConst = (Constant) otherElement;
            if(thisConst.getType().equals(otherConst.getType()))
            {
               if(thisConst.equals(otherConst))
                  f+=contributiveFactor;
               else
                  f+=contributiveFactor/2;
               continue;
            }
         }

         //If both are static invocations
         if (thisElement instanceof StaticInvokeExpr && otherElement instanceof StaticInvokeExpr)
         {
            //log("Static invokes are equal");
            //f+=1;
            StaticInvokeExpr thisStatic = (StaticInvokeExpr) thisElement;
            StaticInvokeExpr otherStatic = (StaticInvokeExpr) otherElement;
            if (thisStatic.getArgCount() == otherStatic.getArgCount() && thisStatic.getMethod(). getName().
                  equals(otherStatic.getMethod().getName()))
            {
               List<Value> tArgs = thisStatic.getArgs();
               List<Value> oArgs = otherStatic.getArgs();
               for(int j=0; j<tArgs.size(); ++j)
               {
                  if(!tArgs.get(j).getType().equals(oArgs.get(j).getType()))
                  {
                     continue outerloop;
                  }
               }
               f+=contributiveFactor;
            }
            continue;
         }
         if (thisElement instanceof InstanceInvokeExpr && otherElement instanceof InstanceInvokeExpr)
         {

            Set<SootMethod> thisInvoker = resolveList.get(thisElement);
            Set<SootMethod> otherInvoker = callchain.resolveList.get(otherElement);

            if (null == thisInvoker || null == otherInvoker)
               continue;

            for(SootMethod thisMeth: thisInvoker)
            { 						

               // 						unmatched1.append(thisMeth.getDeclaration().toString());
               for(SootMethod otherMeth: otherInvoker)
               {

                  if(otherMeth.getDeclaration().equals(thisMeth.getDeclaration()))
                  {
                     //G.v().out.println("MATCHED");
                     f+=contributiveFactor;
                     continue outerloop;
                  }
               }
            }
         }
         else
         {
            //log("could not match this: "+unmatched1 +"AND: "+unmatched2+ "\nWhere callchain was: "+this.calls+"AND: "+ callchain.calls);
         }
      }
      Fraction retVal = i <= 1 ? new Fraction(0) : new Fraction(f, dividingFactor);
      assert retVal.doubleValue() <=1: "Callchain match value = "+retVal;
      return retVal;
   }

   public String toString()
   {
      StringBuilder s = new StringBuilder("Callchain:\n");
      for(Value cc : calls)
         s.append(cc.toString()).append("\n");
      return s.toString();
   }

   /*@Override
     public int hashCode()
     {
     return this.toString().hashCode();
     }*/


   public List<String> split(String cc) {
      List<String> ccSplit = new ArrayList<String>();
      char buff[] =cc.toCharArray();
      int i = 0;
      while(i<buff.length&&buff[i++]!=SPLITTER)
         if (i==buff.length)
         {
            ccSplit.add(cc);
            return ccSplit;
         }
      for(; i<buff.length; ++i)
      {
         StringBuilder elem = new StringBuilder();
         if (buff[i]=='<')
            do			
            {
               elem.append(buff[i++]);		
            }
            while(buff[i-1]!='>');
         else continue;
         String s = new String(elem);
         ccSplit.add(s);
      }
      return ccSplit;
   }
   @Override
      public boolean equals(Object o)
      {
         boolean retVal = true;
         if (!(o instanceof CallChain))
            retVal= false;
         CallChain other = (CallChain) o;
         if (other.calls.size()!=calls.size())
            retVal= false;
         else{
            for(int i=0;i<calls.size();i++)
            {
               if(!calls.get(i).toString().equals(other.calls.get(i).toString()))
                  retVal= false;
            }}
         if(!retVal)
         {
            //		G.v().out.print("DIFFERING CALLCHAINS:\n");
            //		G.v().out.print(this.toString()+other.toString());
         }
         return retVal;
      }

   /**
    * Checks if a CallChain is a smaller version of this CallChain
    * @param otherCallChain
    * @return
    */
   public boolean isSuperSet(CallChain otherCallChain)
   {
      return new HashSet<Value>(calls).containsAll(otherCallChain.calls);
   }

   public void deepCopy(CallChain otherChain) {
      for (Value v: otherChain.calls)
         calls.add(v);
   }

}
