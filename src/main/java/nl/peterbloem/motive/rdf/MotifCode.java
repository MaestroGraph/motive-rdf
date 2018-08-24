package nl.peterbloem.motive.rdf;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.omg.Messaging.SyncScopeHelper;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.PitmanYorModel;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.KGraphList.KNode;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Functions.prefix;
import static nl.peterbloem.kit.Functions.tic;
import static nl.peterbloem.kit.Functions.toc;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class MotifCode
{

	/**
	 * 
	 * @param degrees The graph degrees in the order: indegrees, outdegrees, predicate-degrees.
	 * @param pattern
	 * @param values
	 * @param prior
	 * @return
	 */
	public static double codelength(
			List<List<Integer>> degrees, 
			DTGraph<Integer, Integer> pattern, List<List<Integer>> values)
	{
		return codelength(degrees, pattern, values, false);
	}
	
	public static double codelength(
			List<List<Integer>> degrees, 
			DTGraph<Integer, Integer> pattern, List<List<Integer>> values, boolean fastPY)
	{		
		// * number of links in the graph
		long m = 0;
		for(int degree : degrees.get(0))
			m += degree;
		
		//System.out.println("Summed degree sequence: " + toc());
		
		FrequencyModel<String> fm = new FrequencyModel<String>();
				
		// * Graph dimensions
		fm.add("dimensions", 
				prefix(degrees.get(0).size()) + // - nr. of nodes
				prefix(m) +                     // - nr. of links
				prefix(degrees.get(2).size())); // - nr. of relations
		
		// System.out.println("Computed dimension size: " + toc());
		
		// * Pattern (structure)
		double patternBits = 0.0;
		List<List<Integer>> patternDegrees = KGraphList.degrees(pattern);
		
		patternBits += EdgeListModel.codelength(patternDegrees, Prior.COMPLETE);
		
		fm.add("pattern", patternBits);
		
		//System.out.println("Computed pattern size: " + toc());
		
		// * The template		
		// - copy the degree vector 
		// degrees = asList(new ArrayList<>(degrees.get(0)), new ArrayList<>(degrees.get(1)), new ArrayList<>(degrees.get(2)));
		List<SparseList> degsub = asList(
				new SparseList(degrees.get(0).size()), 
				new SparseList(degrees.get(1).size()), 
				new SparseList(degrees.get(2).size()));
		
		// - collect the triples described by the instances
		List<Triple> triples = Sampler.allTriples(pattern, values);
		
		//System.out.println("Collected triples: " + toc());
		// System.out.println(values);
		for(Triple triple : triples)
		{
			int s = triple.subject(), p = triple.predicate(), o = triple.object();
			
			degsub.get(0).inc(o);
			degsub.get(1).inc(s);
			degsub.get(2).inc(p);
		}
		
		// System.out.println(degrees);
		// System.out.println(degsub);
		//System.out.println("Computed new degree sequence: " + toc());
		
		degrees = asList(
				minus(degrees.get(0), degsub.get(0)),
				minus(degrees.get(1), degsub.get(1)),
				minus(degrees.get(2), degsub.get(2)));
		
		fm.add("template", EdgeListModel.codelength(degrees, fastPY ? Prior.COMPLETE_FAST : Prior.COMPLETE));
		
		//System.out.println("Computed template bits: " + toc());
		
		// * The labels
		int c = 0;
		
		double labelBits = 0.0;
		int n = values.size();
		
		labelBits += Functions.prefix(n); // Store the number of instances
		
		for(int val : pattern.labels())
			if(val < 0)
			{
				labelBits += fastPY ? PitmanYorModel.storeIntegers(repeat(val, n)) : PitmanYorModel.storeIntegersOpt(repeat(val, n));
				c ++;
			}
				
		//System.out.println("Computed label bits (for "+c+" labels): " + toc());
		
		c = 0;
		for(int val : pattern.tags())
			if(val < 0)
			{
				labelBits += fastPY ? PitmanYorModel.storeIntegers(repeat(val, n)) : PitmanYorModel.storeIntegersOpt(repeat(val, n));
				c++;
			}
		
		//System.out.println("Computed label bits (for "+c+" tags): " + toc());
		
		for(int i : series(values.get(0).size()))
		{
			List<Integer> column = column(i, values);
			
			labelBits += fastPY ? PitmanYorModel.storeIntegers(column) : PitmanYorModel.storeIntegersOpt(column);
			// System.out.println("Computed variable label bits: " + column.size() + " " + toc());
		}

		

		fm.add("labels", labelBits);
				
		//System.out.println("Stored labels: " + toc());

		return fm.total();
	}

	/**
	 * Multiple-motif version of the code
	 * 
	 * @param degrees
	 * @param patterns
	 * @param values List of lists of values (one list per pattern). These should
	 *  be filtered so that each triple is only "captured" by one pattern. 
	 * @param fastPY
	 * @return
	 */
	public static double codelength(
			List<List<Integer>> degrees, 
			List<DTGraph<Integer, Integer>> patterns, List<List<List<Integer>>> values, boolean fastPY)
	{
		assert(patterns.size() == values.size());
		
		// * number of links in the graph
		long m = 0;
		for(int degree : degrees.get(0))
			m += degree;
		
		FrequencyModel<String> fm = new FrequencyModel<String>();
				
		// * Graph dimensions
		fm.add("dimensions", 
				prefix(degrees.get(0).size()) + // - nr. of nodes
				prefix(m) +                     // - nr. of links
				prefix(degrees.get(2).size())); // - nr. of relations
		
		
		// * Pattern (structure)
		double patternBits = 0.0;
		for(DTGraph<Integer, Integer> pattern : patterns)
		{
			List<List<Integer>> patternDegrees = KGraphList.degrees(pattern);
			patternBits += EdgeListModel.codelength(patternDegrees, Prior.COMPLETE);
		}
		
		fm.add("patterns", patternBits);
				
		// * The template
		
		// - collect the triples described by the instances
		List<Triple> triples = new ArrayList<>();   
		
		for(int j  : series(patterns.size()))
		{
			DTGraph<Integer, Integer> pattern = patterns.get(j);
			List<List<Integer>> patternValues = values.get(j);
			
    		triples.addAll(Sampler.allTriples(pattern, patternValues));
		}
		
		assert(triples.size() == new HashSet<>(triples).size());
		
		// - vectors to subtract from the degree vector
		List<SparseList> degsub = asList(
				new SparseList(degrees.get(0).size()), 
				new SparseList(degrees.get(1).size()), 
				new SparseList(degrees.get(2).size()));
		
    	for(Triple triple : triples)
    	{
    			int s = triple.subject(), p = triple.predicate(), o = triple.object();
    			
    			degsub.get(0).inc(o);
    			degsub.get(1).inc(s);
    			degsub.get(2).inc(p);
    	}
    		
    		
    	degrees = asList(
    		minus(degrees.get(0), degsub.get(0)),
    		minus(degrees.get(1), degsub.get(1)),
    		minus(degrees.get(2), degsub.get(2)));
		
		fm.add("template", EdgeListModel.codelength(degrees, fastPY ? Prior.COMPLETE_FAST : Prior.COMPLETE));
		
		
		// * The labels
		int c = 0;
		
		double labelBits = 0.0;
		
		for(int j  : series(patterns.size()))
		{
			DTGraph<Integer, Integer> pattern = patterns.get(j);
			List<List<Integer>> patternValues = values.get(j);
			
    		if(! patternValues.isEmpty())
    		{
    			int n = patternValues.size();
    			
    			labelBits += Functions.prefix(n);
    		
        		for(int val : pattern.labels())
        			if(val < 0)
        			{
        				labelBits += fastPY ? PitmanYorModel.storeIntegers(repeat(val, n)) : PitmanYorModel.storeIntegersOpt(repeat(val, n));
        				c ++;
        			}
    				
        		//System.out.println("Computed label bits (for "+c+" labels): " + toc());
    		
        		c = 0;
        		for(int val : pattern.tags())
        			if(val < 0)
        			{
        				labelBits += fastPY ? PitmanYorModel.storeIntegers(repeat(val, n)) : PitmanYorModel.storeIntegersOpt(repeat(val, n));
        				c++;
        			}
    		
        		//System.out.println("Computed label bits (for "+c+" tags): " + toc());
        		
    
        		for(int i : series(patternValues.get(0).size()))
        		{
        			List<Integer> column = column(i, patternValues);
        			
        			labelBits += fastPY ? PitmanYorModel.storeIntegers(column) : PitmanYorModel.storeIntegersOpt(column);
        			// System.out.println("Computed variable label bits: " + column.size() + " " + toc());
        		}
    		}
    		
		}

		fm.add("labels", labelBits);
				
		// System.out.println("Stored labels: " + toc());

		return fm.total();
	}
	
	private static List<Integer> minus(final List<Integer> one, final SparseList two)
	{
		assert(one.size() == two.size());
		
		return new AbstractList<Integer>()
		{

			@Override
			public Integer get(int index)
			{
				return one.get(index) - two.get(index);
			}

			@Override
			public int size()
			{
				// TODO Auto-generated method stub
				return one.size();
			}
			
		};
	}

	private static <T> List<T> column(final int column, final List<List<T>> matrix)
	{
		return new AbstractList<T>()
		{
			@Override
			public T get(int index)
			{
				return matrix.get(index).get(column);
			}
	
			@Override
			public int size()
			{
				return matrix.size();
			}
		};
	}
	

	private static void dec(int i, List<Integer> list)
	{
		list.set(i, list.get(i) - 1);
	}

	private static List<Integer> repeat(final int elem, final int size)
	{
		return new AbstractList<Integer>()
		{
			@Override
			public Integer get(int index)
			{
				return elem;
			}

			@Override
			public int size()
			{
				return size;
			}
		};
	}
	
	private static class SparseList extends AbstractList<Integer>
	{
		private int size;
		private HashMap<Integer, Integer> values = new HashMap<>();
		
		public SparseList(int size)
		{
			this.size = size;
		}

		@Override
		public Integer get(int index)
		{
			if(! values.containsKey(index))
				return 0;
			
			return values.get(index);
		}

		@Override
		public int size()
		{
			return size;
		}
		
		public void inc(int index)
		{
			if(! values.containsKey(index))
				values.put(index, 0);
			
			values.put(index, values.get(index) + 1);
		}	
	}
	
	/**
	 * Prunes the list of values for the given list of patterns. For every triple 
	 * produced twice, the instance providing the later occurrence is removed.
	 * 
	 * @param patterns
	 * @param values
	 * @return
	 */
	public static List<List<List<Integer>>> pruneValues(List<DTGraph<Integer, Integer>> patterns, List<List<List<Integer>>> values)
	{
		assert(patterns.size() == values.size());
		
		List<List<List<Integer>>> result = new ArrayList<>(values.size());
		Set<Triple> seen = new HashSet<>();
		
		for(int i : series(patterns.size()))
		{			
			DTGraph<Integer, Integer> pattern = patterns.get(i);
			List<List<Integer>> patternValues = values.get(i);
			
			List<List<Integer>> keptValues = new ArrayList<>(patternValues.size());
			result.add(keptValues);
		
			for(List<Integer> instance : patternValues)
			{
				List<Triple> triples = Utils.triples(pattern, instance);
				
				if(! contains(seen, triples))
				{
					seen.addAll(triples);
					keptValues.add(instance);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Prunes the list of values for the given list of patterns. For every triple 
	 * produced twice, the instance providing the later occurrence is removed.
	 * 
	 * @param patterns
	 * @param values
	 * @return
	 */
	public static List<List<Integer>> prune(DTGraph<Integer, Integer> pattern, List<List<Integer>> matches)
	{		
		List<List<Integer>> result = new ArrayList<>(matches.size());
		Set<Triple> seen = new HashSet<>();
					
		for(List<Integer> instance : matches)
		{
			List<Triple> triples = Utils.triples(pattern, instance);
			
			if(! contains(seen, triples))
			{
				result.add(instance);
				seen.addAll(triples);
			}
		}
		
		
		return result;
	}
	
	/**
	 * True if the set contains one or more of the collection of elements 
	 * 
	 * @param set
	 * @param elements
	 * @return
	 */
	public static <T> boolean contains(Set<T> set, Collection<T> elements)
	{
		for(T element : elements)
			if(set.contains(element))
				return true;
		
		return false;
	}
}
