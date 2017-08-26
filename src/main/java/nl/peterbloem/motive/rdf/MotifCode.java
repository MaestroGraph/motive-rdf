package nl.peterbloem.motive.rdf;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
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
import nl.peterbloem.motive.rdf.KGraph.KNode;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Functions.prefix;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class MotifCode
{

	/**
	 * 
	 * @param degrees The graph degrees in de order: indegrees, outdegrees, predicate-degrees.
	 * @param pattern
	 * @param values
	 * @param prior
	 * @return
	 */
	public static double codelength(
			List<List<Integer>> degrees, 
			DTGraph<Integer, Integer> pattern, List<List<Integer>> values)
	{
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
		List<List<Integer>> patternDegrees = KGraph.degrees(pattern);
		
		patternBits += EdgeListModel.codelength(patternDegrees, Prior.COMPLETE);
		
		fm.add("pattern", patternBits);
		
		// * The template
		// collect the triples described by the instances
		List<Triple> triples = new ArrayList<Triple>();
		
		System.out.println(degrees.get(1).get(46900));

		// - copy the degree vector 
		degrees = asList(new ArrayList<>(degrees.get(0)), new ArrayList<>(degrees.get(1)), new ArrayList<>(degrees.get(2)));
		
		for(int i : series(values.get(0).size()))
			triples.addAll(Sampler.triples(pattern, values.get(i)));
			
		for(Triple triple : triples)
		{
			int s = triple.subject(), p = triple.predicate(), o = triple.object();
			
			dec(o, degrees.get(0)); // in
			dec(s, degrees.get(1)); // out
			if(degrees.get(1).get(s) < 0)
			{
				System.out.println(s);
				System.out.println(triple);
				System.out.println(triples);
				System.out.println(Functions.min(degrees.get(1)));
			}
			dec(p, degrees.get(2)); // predicate
		}
		
		fm.add("template", EdgeListModel.codelength(degrees, Prior.COMPLETE));
		
		// * The labels
		double labelBits = 0.0;
		int n = values.get(0).size();
		for(int val : pattern.labels())
			if(val < 0)
				labelBits += PitmanYorModel.storeIntegersOpt(repeat(val, n));
		for(int val : pattern.tags())
			if(val < 0)
				labelBits += PitmanYorModel.storeIntegersOpt(repeat(val, n));
		
		for(List<Integer> sequence : values)
			labelBits += PitmanYorModel.storeIntegersOpt(sequence);

		fm.add("labels", labelBits);
		
		fm.print(System.out);
		
		return fm.total();
	}

	public static <T> List<T> column(final int column, final List<List<T>> matrix)
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
}
