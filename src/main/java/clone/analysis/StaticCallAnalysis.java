package clone.analysis;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.fraction.Fraction;

import soot.Value;

public class StaticCallAnalysis extends Analysis<StaticCall> {

	public static float MATCH_FACTOR_MIN = 0.6f;
	public static final float MATCH_FACTOR_MAX = 1.0f;
	public static final double MATCHING_RATIO_THRESHOLD = 0.7f;
	public static final int DENOMINATOR_SCALE = 10;

   public StaticCallAnalysis(StaticCall m1, StaticCall m2) {
      super(m1, m2);
   }

	public static boolean like(double score) {
		return score>=MATCH_FACTOR_MIN && score<=MATCH_FACTOR_MAX;
	}

   public Object call() {
      Pair<Fraction, Integer> pair = match();
      return like(pair.getLeft().doubleValue()) ? Triple.of(m1, m2, pair) : null;
   }

   public Pair<Fraction, Integer> match() {
      Fraction matchFactor = new Fraction(0);
      if (m1.getArgCount()!=m2.getArgCount())
         return Pair.of(matchFactor, 0);
      int counter=0;
      List<Value> m1Args = m1.getArgs();
      List<Value> m2Args = m2.getArgs();
      int matchSize = 0;
      for (int i = 0, n = m1.getArgCount(); i < n; ++i)
      {	
         Fraction maxMatchFactor = new Fraction(0);
         for(CallChain m1CallChain: m1.getCallChains().get(m1Args.get(i))) 
         {
            for(CallChain m2CallChain: m2.getCallChains().get(m2Args.get(i))) 
            {
               Fraction factor = m1CallChain.match(m2CallChain);
               if (factor.compareTo(maxMatchFactor) > 0) 
               {
                  maxMatchFactor = factor;
                  matchSize = Math.max(matchSize, m1CallChain.size());
               }
            }
         }

         Fraction secondaryMaxMatchFactor = new Fraction(0);
         Set<CallChain> m1CallChainSet = m1.getCallChains().getSecondary(m1Args.get(i));
         Set<CallChain> m2CallChainSet = m2.getCallChains().getSecondary(m2Args.get(i));
         if (null != m1CallChainSet && null != m2CallChainSet)
            for(CallChain m1CallChain: m1CallChainSet) 
               for(CallChain m2CallChain: m2CallChainSet) {
                  Fraction factor = m1CallChain.match(m2CallChain);
                  if (factor.compareTo(secondaryMaxMatchFactor) > 0) 
                     secondaryMaxMatchFactor = factor;
               }

         matchFactor = matchFactor.add(maxMatchFactor).add(secondaryMaxMatchFactor);
         ++counter;
      }

      if(counter!=0)
         matchFactor = matchFactor.divide(counter);
      //+some points is for name match, -1 if methods don't match or the method is not assert
      if (0 < matchSize) {
         int common_point = 0;
         if (!m1.getStaticCallName().equals(m2.getStaticCallName()))
            --common_point;
         if (!m1.getInvokeExpr().getMethod().getName().startsWith("assert")
               || !m2.getInvokeExpr().getMethod().getName().startsWith("assert"))
            --common_point;
         double matchingRatio = 1-Utils.editDistance(m1.getMethodName(), m2.getMethodName())/
            (double)Math.min(m1.getMethodName().length(), m2.getMethodName().length());
         if (matchingRatio > MATCHING_RATIO_THRESHOLD)
            ++common_point;
         if (common_point != 0)
            if (matchFactor.getDenominator() != 1)
               matchFactor = new Fraction(matchFactor.getNumerator()+common_point, matchFactor.getDenominator()+common_point);
            else
               matchFactor = new Fraction(Math.min(DENOMINATOR_SCALE, DENOMINATOR_SCALE+common_point), DENOMINATOR_SCALE);
      }
      return Pair.of(matchFactor.doubleValue() <= 1 ? matchFactor : new Fraction(1, 1), matchSize);
   }

}
