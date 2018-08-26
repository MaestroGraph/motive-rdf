//package nl.peterbloem.motive.rdf;
//
//import static java.lang.Math.max;
//import static java.lang.Math.min;
//import static java.util.Arrays.asList;
//import static java.util.Collections.sort;
//import static nl.peterbloem.kit.Functions.sampleInts;
//import static nl.peterbloem.kit.Functions.tic;
//import static nl.peterbloem.kit.Global.random;
//import static nl.peterbloem.kit.Pair.p;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.nodes.DTGraph;
//import org.nodes.DTLink;
//import org.nodes.DTNode;
//import org.nodes.Graphs;
//import org.nodes.MapDTGraph;
//import org.nodes.Subgraph;
//import org.nodes.UGraph;
//import org.nodes.UNode;
//import org.nodes.motifs.MotifCompressor;
//import org.nodes.motifs.MotifVarTags;
//import org.nodes.random.SimpleSubgraphGenerator;
//
//import nl.peterbloem.kit.FrequencyModel;
//import nl.peterbloem.kit.Functions;
//import nl.peterbloem.kit.Generator;
//import nl.peterbloem.kit.Generators;
//import nl.peterbloem.kit.Global;
//import nl.peterbloem.kit.Order;
//import nl.peterbloem.kit.Pair;
//import nl.peterbloem.kit.Series;
//import nl.peterbloem.motive.rdf.KGraphList.KNode;
//
//public class Sampler
//{
//	private static final int MAX_TRIES = 500;
//	private int pruneEvery = 500000;
//	private static final int PRUNE_BELOW_FREQ = 2;
//	
//	private KGraphList data;
//	private int samples;
//
//	private List<DTGraph<Integer, Integer>> tokens;
//
//	private Generator<Integer> intGen;
//	private SimpleSubgraphGenerator gen;
//	
//	private MotifVarTags mvTop = null;
//	
//	private Map<DTGraph<Integer, Integer>, List<List<Integer>>> sample;
//	private static final int minFreq = 1;
//	
//	private FrequencyModel<DTGraph<Integer, Integer>> fm;
//	
//	private int maxVarNodes;
//	private int maxVarTags;
//
//	
//	public Sampler(
//			KGraphList data,
//			int numSamples, int pruneEvery,
//			int minSize, int maxSize,
//			int maxVarNodes, int maxVarTags)
//	{	
//		this.data = data;
//		
//		this.samples = numSamples;
//		this.pruneEvery = pruneEvery;
//		
//		this.maxVarNodes = maxVarNodes;
//		this.maxVarTags = maxVarTags;
//
//		intGen = Generators.uniform(minSize, maxSize + 1);
//		
//		run();
//	}
//	
//	private void prune()
//	{		
//		// * Remove overlapping occurrences 
//		FrequencyModel<DTGraph<Integer, Integer>> newFm = 
//				new FrequencyModel<>();
//		Map<MapDTGraph<Integer, Integer>, List<List<Integer>>> newOccurrences = 
//				new LinkedHashMap<>();
//		
//		Iterator<DTGraph<Integer, Integer>> subs = sample.keySet().iterator(); 
//		while(subs.hasNext())
//		{
//			DTGraph<Integer, Integer> sub = subs.next();
//			if(sample.get(sub).size() < PRUNE_BELOW_FREQ)
//			{
//				subs.remove();
//				continue;
//			}
//			
//			List<List<Integer>> instances = sample.get(sub); 
//			
//			// * An instance should be removed if it contains triples already 
//			//   included in other instances.
//			
//			Iterator<List<Integer>> it = instances.iterator();
//			Set<Triple> seen = new HashSet<Triple>();
//			while(it.hasNext())
//			{
//				List<Integer> instance = it.next();
//				List<Triple> triples = Utils.triples(sub, instance);
//				if(containsAny(seen, triples))
//					it.remove();
//				else
//					seen.addAll(triples);
//			}
//		}
//		
//	}
//	
//	private void run()
//	{
//		Global.log().info("Sampling motifs");		
//
//		// * The (overlapping) instances
//		sample = new LinkedHashMap<>();
//
//		gen = new SimpleSubgraphGenerator(data, intGen);
//		
//		Global.log().info("Start sampling.");
//
//		for (int i : Series.series(samples))
//		{
//			Functions.dot(i, samples);
//			
//			List<Integer> values = new ArrayList<Integer>();
//			int size = intGen.generate();
//			DTGraph<Integer, Integer> sub = sample(data, size, values, maxVarNodes, maxVarTags);
//						
//			// * record the occurrence
//			if (!sample.containsKey(sub))
//				sample.put(sub, new ArrayList<List<Integer>>());
//
//			sample.get(sub).add(values);
//			
//			if(i % pruneEvery == 0)
//			{				
//				System.out.print('!');
//				prune();
//			}
//			
////			System.out.print(':');
////			if(Global.random().nextDouble() < 0.01)
////				System.out.println("\n"+sample.size());
//		}
//		
//		prune();
//		
//		Global.log().info("Finished sampling motifs and removing overlaps.");
//		
//		fm = new FrequencyModel<>(); 
//		for(DTGraph<Integer, Integer> sub : sample.keySet())
//			fm.add(sub, sample.get(sub).size());
//	}
//	
//	public List<DTGraph<Integer, Integer>> patterns()
//	{
//		return fm.sorted();
//	}
//	
//	public List<DTGraph<Integer, Integer>> patterns(int max)
//	{
//		List<DTGraph<Integer, Integer>> list = fm.sorted();
//		return list.subList(0, Math.min(list.size(), max));
//	}
//	
//	public List<List<Integer>> instances(DTGraph<Integer, Integer> pattern)
//	{
//		return sample.get(pattern);
//	}
//	
//	private <L> boolean containsAny(Set<L> set, Collection<L> collection)
//	{
//		for(L l : collection)
//			if(set.contains(l))
//				return true;
//		
//		return false;
//	}
//	
//	/**
//	 * Samples a single pattern out of a graph with a single match
//	 * 
//	 * @param data
//	 * @param size
//	 * @param values The instantiations of the variable nodes/links
//	 * @return
//	 */
//	public static DTGraph<Integer, Integer> sample(KGraphList data, int size, List<Integer> values, int maxNodesToVar, int maxTagsToVar)
//	{
//		SimpleSubgraphGenerator gen = new SimpleSubgraphGenerator(data, size);
//		
//		for(int attempt : Series.series(MAX_TRIES))
//		{
//			values.clear();
//    		List<Integer> indices = gen.generate();
//    		
//    		// * Extract induced subgraph
//    		DTGraph<Integer, Integer> sub = Subgraph.dtSubgraphIndices(data, indices);
//    		
//    		System.out.println(sub);
//    		
//    		// * remove some links
//    		int linksToRemove = Global.random().nextInt(max((int)sub.numLinks() - sub.size(), 1));
//    		List<DTLink<Integer, Integer>> links = new ArrayList<>((int)sub.numLinks());
//    		
//    		for(DTLink<Integer, Integer> link : sub.links())
//    			links.add(link);
//    		for(int i : sampleInts(linksToRemove, links.size()))
//    			links.get(i).remove();
//    		
//    		// * Reject the sample if it's not connected
//    		if(! Graphs.connected(sub))
//    			continue;
//    		
//    		// * make some links/nodes variable
//    		
//    		int linksToVar = random().nextInt(maxTagsToVar+1);
//    		int nodesToVar = random().nextInt(maxNodesToVar+1);
//    		
//    		linksToVar = min(linksToVar, sub.tags().size());
//    		nodesToVar = min(nodesToVar, sub.size());
//    		   		
//    		Map<Integer, Integer> nodeMap = new LinkedHashMap<>();
//    		Map<Integer, Integer> tagMap = new LinkedHashMap<>();
//    		
//    		int nextTag = -1, nextLabel = -1;
//    		for(int tag : Functions.subset(sub.tags(), linksToVar))
//    			tagMap.put(tag, nextTag--);
//    		for(DTNode<Integer, Integer> node : Functions.subset(sub.nodes(), nodesToVar))
//    			nodeMap.put(node.label(), nextLabel--);
//    			
//    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
//    				
//    		for(DTNode<Integer, Integer> node : sub.nodes())
//    		{
//    			int label;
//    			
//    			if(nodeMap.containsKey(node.label()))
//    			{    				
//    				values.add(node.label());
//    				label = nodeMap.get(node.label());
//    			} else
//    				label = node.label();
//    			
//    			pattern.add(label);
//    		}
//    		
//    		for(DTLink<Integer, Integer> link : sub.links())
//    		{
//    			int tag = link.tag();
//    			
//    			if(tagMap.containsKey(tag))
//    			{    				
//    				values.add(tag);
//    				tag = tagMap.get(tag);
//    			} else 
//    				tag = link.tag();
//    			
//    			pattern.get(link.from().index()).connect(pattern.get(link.to().index()), tag);
//
//    		}
//    	
//    		System.out.println(pattern);
//    		System.out.println(values);
//    		
//    		// * randomly partition recurring variables
//    		pattern = Utils.partition(pattern, values);
//			
//    		System.out.println(pattern);    		
//    		System.out.println(values);
//    		
//    		System.out.println();
//    		
//    		// * Reject the sample if it's not connected
//    		if(! Graphs.connected(pattern))
//    			continue;
//
//    		pattern = Nauty.canonical(pattern, values);
//
//    		System.out.println("c " +  pattern);    		
//    		System.out.println("  " +  values);
//    		
//    		System.out.println();
//
//    		return pattern;
//		}
//		
//		throw new IllegalStateException("Could not sample connected pattern after "+MAX_TRIES+" attempts. Graph may not contain patterns of the requested size.");
//	
//	}
//}
