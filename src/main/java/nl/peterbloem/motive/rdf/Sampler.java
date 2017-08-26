package nl.peterbloem.motive.rdf;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static nl.peterbloem.kit.Functions.sampleInts;
import static nl.peterbloem.kit.Global.random;
import static nl.peterbloem.kit.Pair.p;
import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.motifs.MotifCompressor;
import org.nodes.motifs.MotifVarTags;
import org.nodes.random.SimpleSubgraphGenerator;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Generator;
import nl.peterbloem.kit.Generators;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Order;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

public class Sampler
{
	private static final int MAX_TRIES = 500;
	private static final int PRUNE_EVERY = 50000;
	private static final int PRUNE_BELOW_FREQ = 2;
	
	private KGraph data;
	private int samples;

	private List<DTGraph<Integer, Integer>> tokens;

	private Generator<Integer> intGen;
	private SimpleSubgraphGenerator gen;
	
	private MotifVarTags mvTop = null;
	
	private Map<DTGraph<Integer, Integer>, List<List<Integer>>> sample;
	private int minFreq = 1;
	
	private FrequencyModel<DTGraph<Integer, Integer>> fm;
	
	private int maxVarLabels = Integer.MAX_VALUE;
	private int maxVarTags = Integer.MAX_VALUE;
	
	public Sampler(
			KGraph data,
			int numSamples,
			int minSize,
			int maxSize,
			int maxVarLabels,
			int maxVarTags)
	{
		this(data, numSamples, minSize, maxSize, 1, maxVarLabels, maxVarTags);
	}
	
	
	public Sampler(
			KGraph data,
			int numSamples,
			int minSize,
			int maxSize,
			int minFreq,	
			int maxVarLabels,
			int maxVarTags)
	{	
		this.data = data;
		this.samples = numSamples;
		this.minFreq = minFreq;
		
		this.maxVarLabels = maxVarLabels;
		this.maxVarTags = maxVarTags;
		
		intGen = Generators.uniform(minSize, maxSize + 1);
		
		run();
	}
	
	private void prune()
	{		
		// * Remove overlapping occurrences 
		//   (keep the ones with the lowest exdegrees)
		FrequencyModel<DTGraph<Integer, Integer>> newFm = 
				new FrequencyModel<>();
		Map<MapDTGraph<Integer, Integer>, List<List<Integer>>> newOccurrences = 
				new LinkedHashMap<>();
		
		Iterator<DTGraph<Integer, Integer>> subs = sample.keySet().iterator(); 
		while(subs.hasNext())
		{
			DTGraph<Integer, Integer> sub = subs.next();
			if(sample.get(sub).size() < PRUNE_BELOW_FREQ)
			{
				subs.remove();
				continue;
			}
			
			List<List<Integer>> instances = sample.get(sub); 
			
			// * An instance should be removed if it contains triples already 
			//   included in other instances.
			
			Iterator<List<Integer>> it = instances.iterator();
			Set<Triple> seen = new HashSet<Triple>();
			while(it.hasNext())
			{
				List<Integer> instance = it.next();
				List<Triple> triples = triples(sub, instance);
				if(containsAny(seen, triples))
					it.remove();
				else
					seen.addAll(triples);
			}
		}
		
	}
	
	private void run()
	{
		Global.log().info("Sampling motifs");		

		// * The (overlapping) instances
		sample = new LinkedHashMap<>();

		gen = new SimpleSubgraphGenerator(data, intGen);
		
		Global.log().info("Start sampling.");

		for (int i : Series.series(samples))
		{
			Functions.dot(i, samples);
			
			List<Integer> values = new ArrayList<Integer>();
			int size = intGen.generate();
			DTGraph<Integer, Integer> sub = sample(data, size, values, maxVarLabels, maxVarTags);
						
			// * record the occurrence
			if (!sample.containsKey(sub))
				sample.put(sub, new ArrayList<List<Integer>>());

			sample.get(sub).add(values);
			
			if(i % PRUNE_EVERY == 0)
			{				
				System.out.print('!');
				prune();
			}
		}
		
		prune();
		
		Global.log().info("Finished sampling motifs and removing overlaps.");
		
		fm = new FrequencyModel<>(); 
		for(DTGraph<Integer, Integer> sub : sample.keySet())
			fm.add(sub, sample.get(sub).size());
	}
	
	public List<DTGraph<Integer, Integer>> patterns()
	{
		return fm.sorted();
	}
	
	public List<List<Integer>> instances(DTGraph<Integer, Integer> pattern)
	{
		return sample.get(pattern);
	}
	
	private <L> boolean containsAny(Set<L> set, Collection<L> collection)
	{
		for(L l : collection)
			if(set.contains(l))
				return true;
		
		return false;
	}
	
	/**
	 * Generates instantiated triples of a pattern 
	 */
	public static List<Triple> triples(DTGraph<Integer, Integer> pattern, List<Integer> values)
	{	
		List<Triple> result = new ArrayList<>((int)pattern.numLinks());
		
		int numVarLabels = 0;
		for(int label : pattern.labels())
			if(label < 0)
				numVarLabels ++;
				
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

	/**
	 * Samples a single pattern out of a graph with a single match
	 * 
	 * @param data
	 * @param size
	 * @param values The instantiations of the variable nodes/links
	 * @return
	 */
	public static DTGraph<Integer, Integer> sample(KGraph data, int size, List<Integer> values, int maxVarLabels, int maxVarTags)
	{
		SimpleSubgraphGenerator gen = new SimpleSubgraphGenerator(data, size);
		
		for(int attempt : Series.series(MAX_TRIES))
		{
    		List<Integer> indices = gen.generate();
    		
    		// * Extract induced subgraph
    		DTGraph<Integer, Integer> sub = Subgraph.dtSubgraphIndices(data, indices);
    		
    		// * remove some links
    		int linksToRemove = Global.random().nextInt(max((int)sub.numLinks() - sub.size(), 1));
    		List<DTLink<Integer, Integer>> toRemove = new ArrayList<>(linksToRemove);
    		List<DTLink<Integer, Integer>> links = new ArrayList<>((int)sub.numLinks());
    		
    		for(DTLink<Integer, Integer> link : sub.links())
    			links.add(link);
    		for(int i : sampleInts(linksToRemove, links.size()))
    			links.get(i).remove();
    		
    		// * Reject the sample if it's not connected
    		if(! Graphs.connected(sub))
    			continue;
    		
    		// * make some links/nodes variable
    		int nextTag = -1, nextLabel = -1;
    		
    		int linksToVar = random().nextInt(min((int)sub.numLinks(), maxVarTags));
    		int nodesToVar = random().nextInt(min(sub.size(), maxVarLabels));
    		Set<Integer> linksToChange = new HashSet<>(Functions.sampleInts(linksToVar, (int)sub.numLinks()));
    		Set<Integer> nodesToChange = new HashSet<>(Functions.sampleInts(nodesToVar, sub.size()));

    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    				
    		for(DTNode<Integer, Integer> node : sub.nodes())
    		{
    			int label;
    			
    			if(nodesToChange.contains(node.index()))
    			{
    				label = nextLabel --;
    				values.add(node.label());
    			} else
    				label = node.label();
    			
    			pattern.add(label);
    		}
    		
    		int i = 0;
    		for(DTLink<Integer, Integer> link : sub.links())
    		{
    			int tag;
    			
    			if(linksToChange.contains(i))
    			{
    				tag = nextTag -- ;
    				values.add(link.tag());
    			} else 
    				tag = link.tag();
    			
    			pattern.get(link.from().index()).connect(pattern.get(link.to().index()), tag);
    			i++;
    		}
    		
    		boolean print = false; //new HashSet<>(sub.labels()).contains(46900) && pattern.numLinks() == 2 && pattern.size() == 3;
    		if(print)
			{
        		System.out.println(nodesToChange);
        		System.out.println(linksToChange);
        		System.out.println(sub);
        		System.out.println(pattern);
        		System.out.println(values);
        		
        		System.out.println(triples(pattern, values));
			}
        		    		
    		// * Reorder nodes to canonical ordering;
    		pattern = Nauty.canonical(pattern, values);
    		
    		if(print)
			{
    			System.out.println(pattern);
        		System.out.println(values);
        		System.out.println(triples(pattern, values));
			}
    		return pattern;
		}
		
		throw new IllegalStateException("Could not sample connected pattern after "+MAX_TRIES+" attempts. Graph may not contain patterns of the requested size.");
	
	}
}
