package clone.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.NewExpr;
import soot.jimple.StringConstant;

import com.google.common.collect.HashMultimap;

public class CallChainSet {

	private final Map <Value,Set<CallChain>> callChains =  new LinkedHashMap<Value,Set<CallChain>>();

	private final Map <Value,Set<CallChain>> secondaryCallChains =  new LinkedHashMap<Value,Set<CallChain>>();

	public CallChainSet() {}
	
	public CallChainSet(CallChainSet ccS) {
      deepCopy(ccS);
	}

	public final HashMultimap<ConditionExpr, Value> conditionExprMap =  HashMultimap.create();

	public final Map<Value, ConditionExpr> reverseConditionExprMap =  new HashMap<Value,ConditionExpr>();

   public void reverseConditionExprMapping() {
      for(Map.Entry<ConditionExpr,Collection<Value>> e : conditionExprMap.asMap().entrySet())
         for (Value v : e.getValue())
            reverseConditionExprMap.put(v, e.getKey());
      conditionExprMap.clear();
   }

   public CallChainSet compute() {
      for (Set<CallChain> scc : callChains.values())
         for (CallChain cc : scc)
            cc.compute();
      return this;
   }

   public CallChainSet deepCopy( CallChainSet ccS ) {
		for (Entry<Value,Set<CallChain>> e: ccS.callChains.entrySet())
		{
			assert e.getKey() != null;
			assert e.getValue() != null;
			callChains.put(e.getKey(), makeDeepCopy(e.getValue()));
		}
		for (Entry<Value,Set<CallChain>> e: ccS.secondaryCallChains.entrySet())
		{
			assert e.getKey() != null;
			assert e.getValue() != null;
			secondaryCallChains.put(e.getKey(), makeDeepCopy(e.getValue()));
		}
      return this;
   }

   public int totalSize() {
      int size = 0;
      for (Set<CallChain> scc : callChains.values())
         for (CallChain cc : scc)
            size += cc.size();
      return size;
   }

	public double like(CallChainSet otherCallChainSet) {
		// TODO(divam): Complete implementation here...
		return 0;
	}
	
	/**
	 * Same as Set.get
	 * @param val
	 * @return
	 */
	public Set<CallChain> get(Value val) {
		return callChains.get(val);
	}

	public Set<CallChain> getSecondary(Value val) {
		return secondaryCallChains.get(val);
	}

	/**
	 * Same as HashMap.put
	 * @param var
	 * @param callChainSet
	 */
	public void put(Value var, Set<CallChain> callChainSet) {
		assert null != var;
      assert null != callChainSet;
		callChains.put(var, callChainSet);
	}

	public void putSecondary(Value var, Set<CallChain> callChainSet) {
		assert null != var;
      assert null != callChainSet;
		secondaryCallChains.put(var, callChainSet);
	}
	
	/**
	 * Creates an Empty call chain for the variable
	 * @param var
	 */
	public void putEmptyCallChain(Value var) {
		CallChain newCallChain = new CallChain();
		newCallChain.add(StringConstant.v(var.getType().toString()));
		Set<CallChain> newCallChains = new HashSet<CallChain>();
		newCallChains.add(newCallChain);
		put(var, newCallChains);
	}
	/**
	 * Same as HashMap.containsKey
	 * @param receiver
	 * @return
	 */
	public boolean containsKey(Value receiver) {
		return callChains.containsKey(receiver);
	}
	
	private boolean callChainSetsAreEqual(Set<CallChain> first, Set<CallChain> second) {
        if (first.size()!=second.size())
        	return false;
        else
        {
                for(CallChain cc : first)
                {       boolean chainMatched = false;
                        for (CallChain cc2 : second)
                        {
                                if (cc2.equals(cc))
                                {
                                        chainMatched = true;
                                        break;
                                }
                        }
                        if (!chainMatched)
                        {
                                return false;
                        }
                }
        }
        return true;
	}

	public static Set<CallChain> makeDeepCopy(Set<CallChain> set) {
	    Set<CallChain> newCallChains = new HashSet<CallChain>();
	    for(CallChain c: set)
          newCallChains.add(new CallChain(c));
	    return newCallChains;
	}
	
	public static boolean isSubSet(Set<CallChain> first, Set<CallChain> second) {
		boolean retVal = true;
	    for(CallChain cc : first)
	    {       boolean chainMatched = false;
	            for (CallChain cc2 : second)
	            {
	                    if (cc2.equals(cc))
	                    {
	                            chainMatched = true;
	                            break;
	                    }
	            }
	            if (!chainMatched)
	            {
	                    retVal = chainMatched;
	                    return retVal;
	            }
	                    
	    }
	return retVal;
	}

	/**
	 * Merges all entries from other set to this set
	 * @param other
	 */
	public CallChainSet merge(CallChainSet other) {
		for(Value v: other.callChains.keySet())
		{
			if(callChains.containsKey(v))
			{
				next:
				for(CallChain otherChain: other.callChains.get(v))
				{
					for(CallChain thisChain: callChains.get(v))
					{
						if(thisChain.isSuperSet(otherChain))
						{
		    	//			G.v().out.println("NO CHANGE FOR "+v.toString());
		    				continue next;
						}
						if(otherChain.isSuperSet(thisChain))
						{
				//			G.v().out.println("ADDING NEW ENTRIES "+v.toString());
							thisChain.deepCopy(otherChain);
						}
					}
				}
			}
			else
			{
		//		G.v().out.println("NEW VALUE "+v.toString());
				assert other.get(v) != null;
				put(v, other.get(v));
			}
		}

      return this;
	}

	/**
	 * Adds The new Expr to all existing call chains for variable
	 * @param var The Variable
	 * @param newCallChainEntry
	 */
	public CallChainSet appendToCallChains(Value var, Value newCallChainEntry) {
		for (CallChain c: callChains.get(var))
			c.add(newCallChainEntry);

      return this;
	}

	public CallChainSet appendToSecondaryCallChains(Value var, Value newCallChainEntry) {
      if (null == secondaryCallChains.get(var))
         secondaryCallChains.put(var, getExistingSecondaryChains(var));
		for (CallChain c: secondaryCallChains.get(var))
			c.add(newCallChainEntry);

      return this;
	}

	/**
	 * Returns number of variables currently contained in the set
	 * @return
	 */
	public int size() {
		return callChains.size();
	}

	/**
	 * Wipes call chains for existing variable var
	 * @param var
	 */
	public void clear(Value var) {
		if (callChains.containsKey(var))
			callChains.get(var).clear();
		else
			throw new RuntimeException("Variable not in CallChainSet");	
	}
	
	public void clearCallChain(Value var) {
		callChains.get(var).clear();	
	}


	/**
	 * HashMap.containsKey
	 * @param var
	 * @return
	 */
	public boolean contains(Value var) {
		return callChains.containsKey(var);
	}

	public Set<CallChain> getExistingChains(Value val) {
		Set<CallChain> set = null;
		if(val instanceof Constant || val instanceof NewExpr || (set = callChains.get(val)) == null)
		{
			set =  new HashSet<CallChain>();
			CallChain cc = new CallChain();
			cc.add(val);
         set.add(cc);
		}
		return set;
	}

	public Set<CallChain> getExistingSecondaryChains(Value val) {
		Set<CallChain> set = null;
		if(val instanceof Constant || val instanceof NewExpr || (set = secondaryCallChains.get(val)) == null)
		{
			set =  new HashSet<CallChain>();
			CallChain cc = new CallChain();
			cc.add(val);
         set.add(cc);
		}
		return set;
	}

	public Iterator<Value> getVariableIterator() {
		return callChains.keySet().iterator();
	}
	
	public Set<Value> getVariableSet() {
		return callChains.keySet();
	}

	public Set<Entry<Value, Set<CallChain>>> getEntrySet() {
		return callChains.entrySet();
	}

	public void clearAll() {
		callChains.clear();
      secondaryCallChains.clear();
	}

	public Iterator<Entry<Value, Set<CallChain>>> getIterator() {
		return callChains.entrySet().iterator();
	}

   public Set<Value> getAllValues() {
      Set<Value> values = new HashSet<Value>();
		for (Entry<Value,Set<CallChain>> e: getEntrySet()) {
         values.add(e.getKey());
         for (CallChain cc : e.getValue())
            values.addAll(cc.getCalls());
      }
      return values;
   }
	
	@Override
	public boolean equals(Object o)
	{
	//	G.v().out.println("BEGINNING EQUALITY CHECK");
		if(!(o instanceof CallChainSet))
			return false;
		CallChainSet other = (CallChainSet) o; 
		for(Entry<Value,Set<CallChain>> e: other.getEntrySet())
		{
			if(!callChains.containsKey(e.getKey()))
				return false;
			if (callChainSetsAreEqual(callChains.get(e.getKey()), e.getValue()))
				continue;
			return false;
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for (Entry<Value,Set<CallChain>> e: callChains.entrySet())
		{
         builder.append(e.getKey()).append("\n");
         for (CallChain c: e.getValue())
            builder.append(c).append("\n");
		}
		return builder.toString();
	}
}
