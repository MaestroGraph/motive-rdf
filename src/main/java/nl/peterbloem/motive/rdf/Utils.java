package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Functions.choose;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DTGraph;

import com.google.common.base.Function;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public abstract class Utils
{
	private Utils(){};
	
	
	/**
	 * @param pattern
	 * @return
	 */
	public static int numVarLabels(DTGraph<Integer, Integer> pattern)
	{
		int numVarLabels = 0;
		for(int label : pattern.labels())
			if(label < 0)
				numVarLabels ++;
		return numVarLabels;
	}

	
	/**
	 * Places the given elements into a uniform random partition
	 * @return
	 */
	public static <L> Map<L, Integer> partition(Collection<L> elements, Comparator<L> comp)
	{
		// * sample a number of buckets 
		int n = randExp(elements.size());
		
		List<List<L>> buckets = new ArrayList<>(n);
		for(int i :Series.series(n))
			buckets.add(new ArrayList<>());
		
		for(L element : elements)
			choose(buckets).add(element);
		
		for(List<L> bucket : buckets)
			Collections.sort(bucket, comp);
		
		Collections.sort(buckets, new Comparator<List<L>>()
		{ public int compare(List<L> one, List<L> two) {
			if(one.size() != two.size())
				return Integer.compare(two.size(), one.size()); // biggest first order
			
			if(one.isEmpty() && two.isEmpty())
				return 0;
			
			if(comp.compare(one.get(0), two.get(0)) != 0)
				return comp.compare(one.get(0), two.get(0));
			
			return this.compare(one.subList(1, one.size()), two.subList(1, two.size()));
		}});
		
		// * Remove empty buckets
		Iterator<List<L>> it = buckets.iterator();
		while(it.hasNext())
			if(it.next().isEmpty())
				it.remove();

		// * Compile the map 
		Map<L, Integer> result = new LinkedHashMap<>();
		
		for(int i : series(buckets.size()))
			for(L element : buckets.get(i))
				result.put(element, i);
		
		return result;
	}
	
	
	/**
	 * Draws a 
	 * @return
	 */
	public static int randExp(int n)
	{
		double draw = Global.random().nextDouble();
		
		double sum = 0.0;
		
		double z = (1.0/(bell(n) * Math.E));
		
		int u = 1;

		while(true)
		{
			double prob = z * (Math.pow(u, n)/Functions.factorial(u)); 
			if(draw < sum + prob)
				return u;
			
			sum += prob;	
			u++;
		}
	}
	
	/**
	 * Return the n'th Bell's number
	 */
	public static long bell(int n)
	{
		if(n >= MAXN)
			throw new IllegalArgumentException("Input too big");
		
		return bell[n][0];
	}
	
	static long[][] bell;
	static int MAXN = 100;

	static {
		
		bell = new long[MAXN][];
		
		for(int row : series(MAXN))
			if(row == 0) { 
				bell[row] = new long[]{(long)1};
			} else {
				bell[row] = new long[row+1];
				for(int col : series(row + 1))
					if(col == 0)
					{
						bell[row][col] = bell[row-1][bell[row-1].length-1];
					} else
					{
						bell[row][col] = bell[row][col-1] + bell[row-1][col-1];
					}
			}
	}
	
	
}
