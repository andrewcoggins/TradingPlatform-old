package brown.valuation;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.random.ISAACRandom;

import brown.assets.value.FullType;

public class PointValuation implements Valuation {
	private Set<FullType> GOODS; 
	private Function<Integer, Double> VALFUNCTION; 
	private Double VALUESCALE;

	
	public PointValuation (Set<FullType> goods, Function<Integer, Double> valFunction, 
			 Double valueScale) {
		this.GOODS = goods; 
		this.VALFUNCTION = valFunction; 
		this.VALUESCALE = valueScale;	
	}
	
	@Override
	public Map<Set<FullType>, Double> getTotalValuation() {
		
		Map<Set<FullType>, Double> existingSets = new HashMap<Set<FullType>, Double>();
		existingSets.put(new HashSet<FullType>(), 0.0);
		for(int i = 0; i < GOODS.size(); i++) {
			for(FullType good : GOODS) {
				Map<Set<FullType>, Double> temp = new HashMap<Set<FullType>, Double>();
				for(Set<FullType> e : existingSets.keySet()) {
					if (!e.contains(good)) {
						Set<FullType> eCopy = e; 
						eCopy.add(good);
						temp.put(eCopy, VALFUNCTION.apply(eCopy.size()) * VALUESCALE);
					}
				}
				existingSets.putAll(temp);
			}
		}
		return existingSets;
	}
	
	@Override
	public Map<Set<FullType>, Double> getValuation(Integer numberOfValuations, 
			Integer bundleSizeMean, Double bundleSizeStdDev, Double ValueScale) {
		if (bundleSizeMean > 0 && bundleSizeStdDev > 0) {
		NormalDistribution sizeDist = new NormalDistribution(new ISAACRandom(), bundleSizeMean, 
				bundleSizeStdDev);
		Map<Set<FullType>, Double> existingSets = new HashMap<Set<FullType>, Double>();
		for(int i = 0; i < numberOfValuations; i++) {
			int size = -1; 
			while (size < 1) {
			size = (int) sizeDist.sample();}
			Set<FullType> theGoods = new HashSet<>();
			List<FullType> goodList = new ArrayList<FullType>(GOODS); 
			for(int j = 0; j < size; j++) {
				FullType aGood = goodList.get((int) Math.random() * (goodList.size() - j));
				theGoods.add(aGood);
				goodList.remove(aGood);
			}
			existingSets.put(theGoods, VALFUNCTION.apply(theGoods.size()) * VALUESCALE);
		}
			return existingSets; 
	}
		else {
			System.out.println("ERROR: bundle size parameters not positive");
			throw new NotStrictlyPositiveException(bundleSizeMean);
		}
	}


}
