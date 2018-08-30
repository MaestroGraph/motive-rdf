package nl.peterbloem.motive.rdf;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Triple.t;
import static org.junit.Assert.*;
import static org.nodes.compression.Functions.tic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.compression.Functions;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class FindTest
{

	@Test
	public void findTest1()
	{
		List<Triple> triples = 
				asList(
					t(0, 0, 1),
					t(0, 0, 2),
					t(0, 1, 3),
					t(1, 0, 3),
					t(2, 0, 3),
					t(3, 1, 4),
					t(4, 1, 5),
					t(4, 0, 5)
				);
		
		KGraph graph = new KGraph(triples);
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n3 = pattern.add(3),
    				                 n4 = pattern.add(4),
    				                 n5 = pattern.add(5);
    		
    		n3.connect(n4, 1);
    		n4.connect(n5, -1);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		
    		System.out.println(matches);
    		
    		assertEquals(2, matches.size());
		}
		
		{
    		DTGraph<Integer, Integer>pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n0 = pattern.add(0),
    				                 n3 = pattern.add(3),
    				                 v1 = pattern.add(-1);
    		
    		n0.connect(n3, -2);
    		n0.connect(v1, -3);
    		v1.connect(n3, -4);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		
    		List<List<Integer>> targetList = asList(
    				asList(1, 1, 0, 0), asList(2, 1, 0, 0));
    		Set<List<Integer>> target = new LinkedHashSet<>(targetList);
    		
    		assertEquals(2, matches.size());
		}
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(-2),
    				                 n3 = pattern.add(-3);
    		
    		n1.connect(n2, -4);
    		n1.connect(n3, -5);
     		n2.connect(n3, -6);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		
    		List<List<Integer>> targetList = asList(
    				asList(0, 1, 3, 0, 1, 0), asList(0, 2, 3, 0, 1, 0));
    		Set<List<Integer>> target = new LinkedHashSet<>(targetList);  
    		
    		assertEquals(2, matches.size());
    		assertEquals(target, new LinkedHashSet<>(matches));
		}
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(-2),
    				                 n3 = pattern.add(-3);
    		
    		n1.connect(n3, -4);
     		n2.connect(n3, -5);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		
    		for(List<Integer> match : matches)
    			System.out.println(match);
    		
    		assertEquals(6, matches.size());

    		List<List<Integer>> targetList = asList(
    				asList(0, 2, 3, 1, 0), 
    				asList(2, 0, 3, 0, 1),
    				asList(0, 1, 3, 1, 0),
    				asList(1, 0, 3, 0, 1),
    				asList(1, 2, 3, 0, 0),
    				asList(2, 1, 3, 0, 0)
    			);
    		Set<List<Integer>> target = new LinkedHashSet<>(targetList);
    		
    		assertEquals(target, new LinkedHashSet<>(matches));
		}
	}
	
	/**
	 * Use the BGP matcher as a triple pattern matcher, compare to the KGraph's 
	 * matches.
	 */
	@Test
	public void testTriple()
	{
		KGraph graph = Datasets.dogfood();
		System.out.println("data loaded.");
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(-2);    		
    		n1.connect(n2, 0);
    		
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		
    		assertEquals(graph.find(null, 0, null).size(), matches.size());
		}
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(0);    		
    		n1.connect(n2, -2);
    		
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		
    		assertEquals(graph.find(null, null, 0).size(), matches.size());
		}
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(0),
    				                 n2 = pattern.add(-1);    		
    		n1.connect(n2, -2);
    		
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		
    		assertEquals(graph.find(0, null, null).size(), matches.size());
		}
		
//		{
//    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
//    		DTNode<Integer, Integer> n1 = pattern.add(-1),
//    				                 n2 = pattern.add(-2);    		
//    		n1.connect(n2, -3);
//    		
//    		List<List<Integer>> matches = Find.find(pattern, graph);
//    		
//    		assertEquals(graph.find(null, null, null).size(), matches.size());
//		}
	}
		
	
	@Test
	public void testSynth()
	{
		int MIDDLE = 10;
		KGraph graph = Datasets.test(MIDDLE);
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n0 = pattern.add(0),
    				                 n1 = pattern.add(1),
    				                 v = pattern.add(-3);
    		
    		v.connect(n0, 0);
    		v.connect(n1, 1);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println(matches.size());
    		System.out.println(matches);
    		
    		assertEquals(MIDDLE, matches.size());
		}
	}
	
	@Test
	public void testSynth2()
	{
		int MIDDLE = 100;
		KGraph graph = Datasets.test(MIDDLE);
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> m1 = pattern.add(-1),
    				                 n0 = pattern.add(0),
    				                 n1 = pattern.add(1);
    		
    		m1.connect(n0, -2);
    		m1.connect(n1,  1);
    		
    		System.out.println(pattern);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println(matches.size());
    		System.out.println(matches);
    		
    		assertEquals(MIDDLE, matches.size());
		}		
		{	// * tests that the matcher generates _distinct_ result triples
			//   for each triple in the pattern.
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> m2 = pattern.add(-2),
    				                 m1 = pattern.add(-1),
    				                 n0 = pattern.add(0);
    		
    		m2.connect(m1, -3);
    		m2.connect(n0,  0);
    		
    		System.out.println(pattern);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println(matches.size());
    		System.out.println(matches);
    		
    		assertEquals(MIDDLE, matches.size());
		}
	}

	@Test
	public void testDogfood()
	{
		
		List<String> labels = new ArrayList<>();
		List<String> tags   = new ArrayList<>();
		KGraph graph = Datasets.dogfood(labels, tags);
		
		String yearStr = "http://swrc.ontoware.org/ontology#year";
		String typeStr = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
		String inProcStr = "http://swrc.ontoware.org/ontology#InProceedings";
		
		int year   = tags.indexOf(yearStr);
		int type   = tags.indexOf(typeStr);
		int inProc = labels.indexOf(inProcStr);
		
		System.out.println(year + " " + type + " " + inProc);
		System.out.println(labels.get(5326));
		
		System.out.println("data loaded.");
		
		{	// Query 0
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(5326),
    				                 n3 = pattern.add(-3);
    		
    		n1.connect(n2, year);
    		n1.connect(n3, type);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 0, time:        " + Functions.toc() + " seconds.");  		
    		System.out.println("query 0, num results: " + matches.size());
		}	
		
		{	// Query 1
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(-2),
    				                 n3 = pattern.add(inProc);
    		
    		n1.connect(n2, year);
    		n1.connect(n3, type);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 1, time:        " + Functions.toc() + " seconds."); // 4.2s in rdflib
    		
    		System.out.println("query 1, num results: " + matches.size());
    		
    		assertEquals(3307, matches.size());
		}
		
		{	// Query 2
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(-2),
    				                 n3 = pattern.add(inProc);
    		
    		n1.connect(n2, year);
    		n1.connect(n3, -3);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 2, time:        " + Functions.toc() + " seconds."); // 1.0s in rdflib
    		
    		System.out.println("query 2, num results: " + matches.size());
    		
    		assertEquals(3307, matches.size());
   		}
		
		
//		{	// Query 3.0
//    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
//    		DTNode<Integer, Integer> n1 = pattern.add(-1),
//    				                 n2 = pattern.add(-2),
//    				                 n3 = pattern.add(inProc);
//    		
//    		n1.connect(n2, -3);
//    		n1.connect(n3, type);
//    
//    		tic();
//    		List<List<Integer>> matches = Find.find(pattern, graph, 60);
//    		System.out.println("query 3, time:        " + Functions.toc() + " seconds."); // 17.3s in rdflib
//    		                                                                              // (132 seconds mine)
//    		System.out.println("query 3, num results: " + matches.size()); 
//    		
//    		System.out.println(Find.nonuniques);
//		}
		
		{	// Query 3
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(-2),
    				                 n3 = pattern.add(inProc);
    		
    		n1.connect(n2, -3);
    		n1.connect(n3, type);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 3, time:        " + Functions.toc() + " seconds."); // 17.3s in rdflib
    		                                                                              // (132 seconds mine)
    		System.out.println("query 3, num results: " + matches.size());

    		// assertEquals(81855, matches.size());
    		assertEquals(77897, matches.size());
    		
		}
		
	}
	
	@Test
	public void motifTest()
	{
		int REPEATS = 500;
		
		Global.randomSeed();
		
		for(int i : series(REPEATS))
		{
			nl.peterbloem.kit.Functions.dot(i, REPEATS);
			int numInstances = Global.random().nextInt(25) + 1;
			int motifSize = Global.random().nextInt(2) + 3;
			int motifLinks = Global.random().nextInt(motifSize * motifSize - 2 * motifSize) + motifSize;
			int motifVLinks = Global.random().nextInt(motifLinks);
			
			
			motifTest(numInstances, motifSize, motifLinks, motifVLinks);
		}
		
	}
	
	public void motifTest(int numInstances, int motifSize, int motifLinks, int motifVLinks)
	{
    	int motifVNodes = motifSize,
    		numRelations = 2;    		
    	
		// * Sample the motif
    	int x = 0;
		DGraph<?> topology= null;
		while(topology == null || ! Graphs.connected(topology))
		{
			topology = RandomGraphs.randomDirectedFast(motifSize, motifLinks);
			if(x++ > 100)
			{
				Global.info(" ***** " +  motifSize + " " + motifLinks);
				return;
			}
		}
		
		List<Integer> vNodes = nl.peterbloem.kit.Functions.sampleInts(motifVNodes, motifSize);
		List<Integer> vLinks = nl.peterbloem.kit.Functions.sampleInts(motifVLinks, motifLinks);
		int nextVNode = -1, nextVLink = -motifVNodes -1;
		
		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
		for(int i : series(motifSize))
		{
			// figure out the node label
			int label;
			if(vNodes.contains(i))
				label = nextVNode --;
			else 
				label = Global.random().nextInt(0); // shouldn't happen
			pattern.add(label);
		}
		
		int i = 0;
		for(DLink<?> link : topology.links())
		{
			int from = link.from().index(), to = link.to().index();
			int tag;
			if(vLinks.contains(i))
				tag = nextVLink --;
			else
				tag = Global.random().nextInt(numRelations);
			
			pattern.get(from).connect(pattern.get(to), tag);
			
			i++;
		}
		
		Global.info("Pattern: " + pattern);
		
		List<Triple> triples = new ArrayList<>();
		
		int node = 0;
		for(int j : series(numInstances))
		{
			List<Integer> smp = Series.series(motifSize);		
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int s = link.from().label(),
					p = link.tag(),
					o = link.to().label();
				
				// instantiate any variables with random choices
				if(s < 0)
					s = node + smp.get(-s - 1);
				if(p < 0)
					p = Global.random().nextInt(numRelations);
				if(o < 0)
					o = node + smp.get(-o - 1);
				
				triples.add(Triple.t(s, p, o));
			}
			
			node += motifSize;
		}
			
		for(Triple t : triples)
			System.out.println(t);
		
		KGraph data = new KGraph(triples);
		
		Global.info("Patterns inserted: " + numInstances);
		
		List<List<Integer>> matches = Find.find(pattern, data);
		Global.info("Pattern matches: " + matches.size());
		//assertEquals(numInstances, matches.size());
		System.out.println(matches.get(0).size() + " " + matches.get(0));
		
		matches = MotifCode.prune(pattern, matches);
		Global.info("after pruning: " + matches.size());
		assertEquals(numInstances, matches.size());
	}
	
	/**
	 * For manual tests
	 */
	@Test
	public void randomGraph()
	{
		Global.randomSeed();
		
		int motifSize = 4,
				motifLinks = 2,
				motifVNodes = 3,
				motifVLinks = 0,
				numRelations = 3,
				n = 5, m = 10,
				numInstances = 0;
			
			// * Sample the motif
			DGraph<?> topology = RandomGraphs.randomDirectedFast(motifSize, motifLinks);
			List<Integer> vNodes = nl.peterbloem.kit.Functions.sampleInts(motifVNodes, motifSize);
			List<Integer> vLinks = nl.peterbloem.kit.Functions.sampleInts(motifVLinks, motifLinks);
			int nextVNode = -1, nextVLink = -motifVNodes -1;
			
			DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
			for(int i : series(motifSize))
			{
				// figure out the node label
				int label;
				if(vNodes.contains(i))
					label = nextVNode --;
				else 
					label = Global.random().nextInt(n);
				pattern.add(label);
			}
			
			int i = 0;
			for(DLink<?> link : topology.links())
			{
				int from = link.from().index(), to = link.to().index();
				int tag;
				if(vLinks.contains(i))
					tag = nextVLink --;
				else
					tag = Global.random().nextInt(numRelations);
				
				pattern.get(from).connect(pattern.get(to), tag);
				
				i++;
			}
			
			Global.info("Sampled pattern: " + pattern);
			
			// * Sample the data
			topology = RandomGraphs.randomDirectedFast(n, m);
			
			List<Triple> triples = new ArrayList<>();
			
			for(DLink<?> link : topology.links())
				triples.add(t(
						link.from().index(), 
						Global.random().nextInt(numRelations), 
						link.to().index()));
			
			for(int j : series(numInstances))
			{
				List<Integer> smp = nl.peterbloem.kit.Functions.sampleInts(motifSize, n);			
				for(DTLink<Integer, Integer> link : pattern.links())
				{
					int s = link.from().label(),
						p = link.tag(),
						o = link.to().label();
					
					// instantiate any variables with random choices
					if(s < 0)
						s = smp.get(-s - 1);
					if(p < 0)
						p = Global.random().nextInt(numRelations);
					if(o < 0)
						o = smp.get(-o - 1);
					
					triples.add(Triple.t(s, p, o));
				}
			}
			
			for(Triple t : triples)
				System.out.println(t);
					
			KGraph data = new KGraph(triples);
			
			Global.info(String.format("Sampled data. (%d, %d)", data.size(), data.numLinks()) );
			
			List<List<Integer>> matches = Find.find(pattern, data);
		
			System.out.println(String.format("%d matches:", matches.size()));
			for(List<Integer> match : matches)
				System.out.println(match);
	}
	
}
