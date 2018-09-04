package nl.peterbloem.motive.rdf;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.omg.Messaging.SyncScopeHelper;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.OnlineModel;
import nl.peterbloem.kit.PitmanYorModel;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.KGraphList.KNode;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Functions.log2Factorial;
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
		
		// System.out.println("Summed degree sequence: " + toc());
		
		FrequencyModel<String> fm = new FrequencyModel<String>();
				
		// * Graph dimensions
		int n = degrees.get(0).size(), r = degrees.get(2).size();
		
		fm.add("dimensions", 
				prefix(n) + // - nr. of nodes
				prefix(m) + // - nr. of links
				prefix(r)); // - nr. of relations
		
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
		List<Triple> triples = Utils.allTriples(pattern, values);
		
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
		// System.out.println("Computed new degree sequence: " + toc());
		
		List<List<Integer>> degTemplate = asList(
				minus(degrees.get(0), degsub.get(0)),
				minus(degrees.get(1), degsub.get(1)),
				minus(degrees.get(2), degsub.get(2)));
			
		fm.add("template", EdgeListModel.codelength(degTemplate, fastPY ? Prior.COMPLETE_FAST : Prior.COMPLETE));
		
		// fm.add("labels", matchesCodelength(values, pattern, fastPY));
		fm.add("labels", matchesCodelengthAlt(values, pattern, n, r, fastPY));
				
		//System.out.println("Stored labels: " + toc());
		
		// fm.print(System.out);

		return fm.total();
	}

	public static double matchesCodelength(List<List<Integer>> values, DTGraph<Integer, Integer> pattern, boolean fastPY)
	{
		//System.out.println("Computed template bits: " + toc());
		
		// * The labels
		
		double labelBits = 0.0;
		int n = values.size();
		
		labelBits += Functions.prefix(n); // Store the number of instances
		
		// - Store the non-variable nodes as a sequence of a single tag
		//   (this may seem wasteful, but these sequences are stored very efficiently)
		for(int val : pattern.labels())
			if(val >= 0)
			{
				double columnBits = fastPY ? PitmanYorModel.storeIntegers(repeat(val, n)) : PitmanYorModel.storeIntegersOpt(repeat(val, n));
				labelBits += columnBits;
    			System.out.println("node val: " + val + ", " + columnBits);
    			
    			// labelBits += OnlineModel.storeIntegers(repeat(val, n));
			}
				
		//System.out.println("Computed label bits (for "+c+" labels): " + toc());
		
		// - Store the non-variable tags as a sequence of a single tag
		//   (this may seem wasteful, but these sequences are stored very efficiently)
		for(int val : pattern.tags())
			if(val >= 0)
			{
				double columnBits = fastPY ? PitmanYorModel.storeIntegers(repeat(val, n)) : PitmanYorModel.storeIntegersOpt(repeat(val, n));
				// double columnBits = OnlineModel.storeIntegers(repeat(val, n));
				labelBits += columnBits;
    			System.out.println("tag val " + val + ", " + columnBits);

    			// 
			}
		
		// System.out.println("Computed label bits (for "+c+" tags): " + toc());
		
		// - Store the values for the variable tags and nodes as one sequence per variable
		if(! values.isEmpty())
    		for(int i : series(values.get(0).size()))
    		{
    			List<Integer> column = column(i, values);
    			
    			double columnBits = fastPY ? PitmanYorModel.storeIntegers(column) : PitmanYorModel.storeIntegersOpt(column);
    			// double columnBits = OnlineModel.storeIntegers(column);
    			System.out.println("var " + i + ", " + columnBits);
    			labelBits += columnBits;
    			
    			// labelBits += OnlineModel.storeIntegers(column);
    			// System.out.println("Computed variable label bits: " + column.size() + " " + toc());
    		}
		return labelBits;
	}
	
	/**
	 * 
	 * @param values
	 * @param pattern
	 * @param n Nodes in the graph
	 * @param r Relations in the graph
	 * @return
	 */
	public static double matchesCodelengthAlt(List<List<Integer>> values, DTGraph<Integer, Integer> pattern, int n, int r, boolean fastPY)
	{
		Map<Integer, SparseList> degrees = patternDegrees(values, pattern, n, r);
		
		double degreeBits = patternDegreesCodelength(degrees, fastPY);
		
		if(values.isEmpty())
			return degreeBits;
		
		// number of links in the graph
		long k = values.size();
		int nv = values.get(0).size();
		
		double bits = 0.0;
		bits += (nv - 1) * Functions.log2Factorial(k);
		
		for(List<Integer> degreeList : degrees.values())
			for(int d : degreeList)
				bits -= log2Factorial(d);
		
		return bits + degreeBits;
	}
	
	/**
	 * NB. Assumes that the graph dimensions are already known.
	 * 
	 * @param degrees
	 * @param n
	 * @param r
	 * @return
	 */
	public static double patternDegreesCodelength(Map<Integer, SparseList> degrees, boolean fastPY)
	{
		double bits = 0.0;
		
		// * Store the sequences using the PY model
		for(SparseList sequence : degrees.values())
			bits += fastPY ? PitmanYorModel.storeIntegers(sequence) : PitmanYorModel.storeIntegersOpt(sequence);
		
		return bits;	
	}
	
	public static Map<Integer, SparseList> patternDegrees(List<List<Integer>> values, DTGraph<Integer, Integer> pattern, int numNodes, int numRelations)
	{
		// * Count degrees (how often is each node/relation involved in each variable in the pattern.
		Map<Integer, SparseList> degrees = new LinkedHashMap<>();
		
		for(int label : pattern.labels())
			if(label < 0)
				degrees.put(label, new SparseList(numNodes));
		
		for(int tag : pattern.tags())
			if(tag < 0)
				degrees.put(tag, new SparseList(numRelations));

		for(List<Integer> match : values)
			for (int i : series(match.size()))
			{
				int val = match.get(i);
				int var = - i - 1;
				degrees.get(var).inc(val);
			}
		
		return degrees;
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
	 * Sorts a list of matches. We sort first by the variable for which the values 
	 * have the highest entropy. Ensuring that these values are sorted will save 
	 * a lot of bits in storing the members ofr the PY model.
	 * 
	 * 
	 * @param matches
	 * @return
	 */
	public static void sort(List<List<Integer>> matches)
	{
		if(matches.isEmpty())
			return;
		
		int numValues = matches.get(0).size();
		
		List<FrequencyModel> fms = new ArrayList<>(numValues);
		
		for(int i : series(numValues))
			fms.add(new FrequencyModel<Integer>(column(i, matches)));
		
		List<Integer> order = new ArrayList<>(series(numValues));
		
		// * Sort the value indices by entropy (descending)
		Collections.sort(order, new Comparator<Integer>()
		{
			@Override
			public int compare(Integer i1, Integer i2)
			{
				double e1 = fms.get(i1).entropy(), e2 = fms.get(i2).entropy();
				
				return - Double.compare(e1, e2); // highest entropy first
			}
		});
		
		// * Sort the matches first by the most entropic index, and so on
		Collections.sort(matches, new Comparator<List<Integer>>(){

			@Override
			public int compare(List<Integer> match1, List<Integer> match2)
			{
				for(int index : order)
				{
					int v1 = match1.get(index), v2 = match2.get(index);
					if(v1 != v2)
						return Integer.compare(v1, v2);
				}
				
				return 0;
			}
		});
		
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
			
    		triples.addAll(Utils.allTriples(pattern, patternValues));
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
		fm.print(System.out);
		
		return fm.total();
	}
}
