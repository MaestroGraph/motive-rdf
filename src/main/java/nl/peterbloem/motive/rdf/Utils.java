package nl.peterbloem.motive.rdf;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static nl.peterbloem.kit.Functions.choose;
import static nl.peterbloem.kit.Functions.min;
import static nl.peterbloem.kit.Functions.natural;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.collections.impl.lazy.parallel.set.sorted.SynchronizedParallelSortedSetIterable;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.MapDTGraph;

import com.google.common.base.Function;

import nl.peterbloem.kit.FrequencyModel;
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
	 * "Partitions" the variable labels of the pattern: for each variable node or
	 *  relation which occurs in more than one place in the _triple list_ of the 
	 *  pattern, we partition the occurrences of the label and split each partition
	 *  into its own label.
	 *  
	 *  Note that input patterns should not contain multiple copies of the same
	 *  triple (ie. links with exactly the same labels/tags/variables).
	 *  
	 * @param pattern
	 * @param values the values of the pattern. This list is automatically extended.
	 * @return
	 */
	public static DTGraph<Integer, Integer> partition(DTGraph<Integer, Integer> pattern, List<Integer> values)
	{
		int nvl = Utils.numVarLabels(pattern);
		List<Integer> nValues = new ArrayList<Integer>(values.subList(0, nvl));
		List<Integer> tValues = new ArrayList<Integer>(values.subList(nvl, values.size()));
				
		FrequencyModel<Integer> labFreq = new FrequencyModel<>();
		FrequencyModel<Integer> tagFreq = new FrequencyModel<>();
		
		int nextLabel = 0;
		int nextTag = 0;
		
		List<Integer> labels = new ArrayList<Integer>((int)pattern.numLinks()*2);
		List<Integer> tags = new ArrayList<Integer>((int)pattern.numLinks());
		
		for(DTLink<Integer, Integer> link : pattern.links())
		{
			int s = link.from().label(),
				p = link.tag(),
				o = link.to().label();
			
			labFreq.add(s);
			tagFreq.add(p);
			labFreq.add(o);
			
			nextLabel = min(nextLabel, min(s, o));
			nextTag = min(nextTag, p);
			
			labels.add(s);
			labels.add(o);
			tags.add(p);
		}		
		
		nextLabel --;
		nextTag --;
			
		for(Integer token : labFreq.tokens())
			if(token < 0 && labFreq.frequency(token) > 1.0)
			{
				Map<Integer, Integer> part = partition(series((int)labFreq.frequency(token)), natural());
				
				changeValues(-token -1, nValues,  part);
				List<Integer> mask = getMask(part, token, nextLabel);
				applyMask(labels, token, mask);
				
				nextLabel = min(labels) - 1;
			}
		
		for(Integer token : tagFreq.tokens())
			if(token < 0 && tagFreq.frequency(token) > 1.0)
			{
				Map<Integer, Integer> part = partition(series((int)tagFreq.frequency(token)), natural());
				
				changeValues(-token -1, tValues, part);
				List<Integer> mask = getMask(part, token, nextTag);
				applyMask(tags, token, mask);
				
				nextTag = min(tags) - 1;
			}
				
		DTGraph<Integer, Integer> result = new MapDTGraph<Integer, Integer>();
		
		int s = 0, p = 0, o = 1;

		for(int j : series((int)pattern.numLinks()))
		{
			int from = labels.get(s),
				tag = tags.get(p),
				to = labels.get(o);
			
			if(result.node(from) == null)
				result.add(from);
			if(result.node(to) == null)
				result.add(to);
			
			result.node(from).connect(result.node(to), tag);
			
			s += 2;
			p ++;
			o += 2;
		}
				
		values.clear();
		values.addAll(nValues);
		values.addAll(tValues);
				
		return result;
	}
	
	private static void changeValues(int myIndex, List<Integer> values, Map<Integer, Integer> part)
	{
		Set<Integer> set = new HashSet<Integer>(part.values());
		
		for(int reps : series(set.size()-1))
			values.add(values.get(myIndex));	
	}


	public static List<Integer> getMask(Map<Integer, Integer> part, int me, int nextLabel)
	{
		// Create a map to the new labels for each partition
		// - 'part' uses consecutive integers, we need negative integers 
		//   starting with 'nextLabel', except for partition 0, which is mapped 
		//   to 'me'. 
		Map<Integer, Integer> newLabels = new LinkedHashMap<>();
		for(int index : part.keySet())
		{
			int oldVal = part.get(index);
			if(! newLabels.containsKey(oldVal))
			{
				if(oldVal == 0)
					newLabels.put(oldVal, me);
				else
					newLabels.put(oldVal, nextLabel --);
			}
		}
		
		int max = -1;
		for(int index : part.keySet())
			max = max(max, index);
		
		List<Integer> result = new ArrayList<>();
		
		for(int index : series(max+1))
		{
			int val = newLabels.get(part.get(index));
			result.add(val);
		}
		
		return result;
	}
	
	public static <L> void applyMask(List<L> list, L elem, List<L> mask)
	{	
		int j = 0;
		for(int i : series(list.size()))
			if(list.get(i).equals(elem))
				list.set(i, mask.get(j ++));
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

	/**
	 * Generates instantiated triples of a pattern 
	 */
	public static List<Triple> triples(DTGraph<Integer, Integer> pattern, List<Integer> values)
	{	
		List<Triple> result = new ArrayList<>((int)pattern.numLinks());
		
		int numVarLabels = numVarLabels(pattern);
		assert values.size() == numVarLabels;
				
		for(DTLink<Integer, Integer> link : pattern.links())
		{
			int subject = link.from().label();
			int object = link.to().label();
			int predicate = link.tag();
			
			if(subject < 0)
				subject = values.get(-subject - 1);
			if(object < 0)
				object = values.get(-object - 1);
			if(predicate < 0)
				predicate = values.get(numVarLabels  - predicate - 1);
			 			
			result.add(new Triple(subject, predicate, object));
		}
		
		return result;
	}
	
	
}
