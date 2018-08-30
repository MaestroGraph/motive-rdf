package nl.peterbloem.motive.rdf;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Triple.t;
import static org.junit.Assert.*;
import static org.nodes.compression.Functions.tic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.util.SynchronizedSymbolTable;
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
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class MotifCodeTest
{

	@Test
	public void testSynth()
	{
		int MIDDLE = 10000;
		KGraph graph = Datasets.test(MIDDLE);
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n0 = pattern.add(0),
    				                 n1 = pattern.add(1),
    				                 v = pattern.add(-1);
    		
    		v.connect(n0, 0);
    		v.connect(n1, 1);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		assertEquals(MIDDLE, matches.size());
    		
    		List<List<Integer>> degrees = KGraph.degrees(graph);

    		double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
    		System.out.println("null bits: " + nullBits);
    		
    		Collections.shuffle(matches);
    		
    		matches = MotifCode.prune(pattern, matches);
    		MotifCode.sort(matches);
    		
    		System.out.println(matches.size());
    		System.out.println("motif bits: " +  MotifCode.codelength(degrees, pattern, matches));
		}
	}
	
	@Test
	public void testSynth3()
	{
		int MIDDLE = 10000;
		KGraph graph = Datasets.test2(MIDDLE);
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n0 = pattern.add(0),
    				                 n1 = pattern.add(1),
    				                 v1 = pattern.add(-1),
    				                 v2 = pattern.add(-2);

    		
    		v1.connect(n0, 0);
    		v2.connect(n1, 1);
    		v1.connect(v2, 2);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		assertEquals(MIDDLE, matches.size());
    		
    		List<List<Integer>> degrees = KGraph.degrees(graph);

    		double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
    		System.out.println("null bits: " + nullBits);
    		
    		Collections.shuffle(matches);
    		System.out.println("num matches: " + matches.size());
    		
    		matches = MotifCode.prune(pattern, matches);
    		MotifCode.sort(matches);
    		
    		System.out.println(matches.size());
    		System.out.println("motif bits: " +  MotifCode.codelength(degrees, pattern, matches));
		}
	}	

	/**
	 * Test that patterns with no variables behave as expected.
	 * 
	 * NOTE: My expectation was wrong: since the triples in a constant pattern 
	 * are still taken out of the main graph, a larger constant pattern can 
	 * occasionally compress more than a smaller one (like in the first case). 
	 */
	// @Test Disabled, incorrect test
	public void testSynth2()
	{
		int MIDDLE = 1000;
		KGraph graph = Datasets.test(MIDDLE);
		
		{
    		DTGraph<Integer, Integer> pattern1 = new MapDTGraph<>();
    		DTNode<Integer, Integer> n67 = pattern1.add(67),
    				                 n0  =  pattern1.add(0);
    		
    		n67.connect(n0, 0);
    	
    		DTGraph<Integer, Integer> pattern2 = new MapDTGraph<>();
    		DTNode<Integer, Integer> m67 = pattern2.add(67),
    				                 m0  = pattern2.add(0),
    				                 m1  = pattern2.add(1);
    		
    		m67.connect(m0, 0);
    		m67.connect(m1, 1);
    		
    		
    		List<List<Integer>> matches;    		
    		List<List<Integer>> degrees = KGraph.degrees(graph);
    		
    		matches = Find.find(pattern1, graph);
    		matches = MotifCode.prune(pattern1, matches);
    		double cl1 = MotifCode.codelength(degrees, pattern1, matches);
    		
    		matches = Find.find(pattern2, graph);	    		
    		matches = MotifCode.prune(pattern2, matches);
    		double cl2 = MotifCode.codelength(degrees, pattern2, matches);

    		System.out.println(cl1);
    		System.out.println(cl2);
    		
    		assertTrue(cl1 < cl2);
		}
		{
    		DTGraph<Integer, Integer> pattern1 = new MapDTGraph<>();
    		DTNode<Integer, Integer> n67 = pattern1.add(67),
    				                 n0  =  pattern1.add(0),
    				                 n1  = pattern1.add(1);
    		
    		n67.connect(n0, 0);
    		n67.connect(n1, 1);
    	
    		DTGraph<Integer, Integer> pattern2 = new MapDTGraph<>();
    		DTNode<Integer, Integer> m67 = pattern2.add(67),
    				                 m55 = pattern2.add(55),
    				                 m0  = pattern2.add(0),
    				                 m1  = pattern2.add(1);
    		
    		m67.connect(m0, 0);
    		m67.connect(m1, 1);
    		m55.connect(m0, 0);
    		
    		
    		List<List<Integer>> matches;    		
    		List<List<Integer>> degrees = KGraph.degrees(graph);
    		
    		matches = Find.find(pattern1, graph);
    		matches = MotifCode.prune(pattern1, matches);
    		double cl1 = MotifCode.codelength(degrees, pattern1, matches);
    		
    		
    		matches = Find.find(pattern2, graph);	
    		
    		matches = MotifCode.prune(pattern2, matches);
    		double cl2 = MotifCode.codelength(degrees, pattern2, matches);
    		
    		assertTrue(cl1 < cl2);
		}
	}
	
	@Test
	public void testDogfood()
	{
		
		List<String> labels = new ArrayList<>();
		List<String> tags   = new ArrayList<>();
		KGraph graph = Datasets.dogfood(labels, tags);		
		System.out.println("data loaded.");
		
		String yearStr = "http://swrc.ontoware.org/ontology#year";
		String typeStr = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
		String inProcStr = "http://swrc.ontoware.org/ontology#InProceedings";
		
		int year   = tags.indexOf(yearStr);
		int type   = tags.indexOf(typeStr);
		int inProc = labels.indexOf(inProcStr);
		
		System.out.println(year + " " + type + " " + inProc);
		System.out.println(labels.get(5326));
		
		List<List<Integer>> degrees = KGraph.degrees(graph);

		double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
		System.out.println(format("null model: %.3f bits", nullBits));
		
		
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
    		
    		
    		matches = MotifCode.prune(pattern, matches);
    		System.out.println(format("Matches, after pruning: %d", matches.size()));
    		
    		System.out.println(format("     Motif code length: %.3f ", MotifCode.codelength(degrees, pattern, matches)));
		}
		
		{	// Query 2
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(-2),
    				                 n3 = pattern.add(-3);
    		
    		n1.connect(n2, year);
    		n1.connect(n3, type);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 2, time:        " + Functions.toc() + " seconds."); // 4.2s in rdflib    		
    		System.out.println("query 2, num results: " + matches.size());
    		
    		
    		matches = MotifCode.prune(pattern, matches);
    		System.out.println(format("Matches, after pruning: %d", matches.size()));
    		
    		System.out.println(format("     Motif code length: %.3f ", MotifCode.codelength(degrees, pattern, matches)));
		}	
		
		{	// Query 3
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(5326),
    				                 n3 = pattern.add(-2);
    		
    		n1.connect(n2, year);
    		n1.connect(n3, type);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 3, time:        " + Functions.toc() + " seconds."); // 4.2s in rdflib    		
    		System.out.println("query 3, num results: " + matches.size());
    		
    		
    		matches = MotifCode.prune(pattern, matches);
    		System.out.println(format("Matches, after pruning: %d", matches.size()));
    		
    		System.out.println(format("     Motif code length: %.3f ", MotifCode.codelength(degrees, pattern, matches)));
		}	
		
		{	// Query 4
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(-1),
    				                 n2 = pattern.add(5326),
    				                 n3 = pattern.add(inProc);
    		
    		n1.connect(n2, year);
    		n1.connect(n3, type);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 4, time:        " + Functions.toc() + " seconds."); // 4.2s in rdflib    		
    		System.out.println("query 4, num results: " + matches.size());
    		
    		matches = MotifCode.prune(pattern, matches);
    		System.out.println(format("Matches, after pruning: %d", matches.size()));
    		
    		System.out.println(format("     Motif code length: %.3f ", MotifCode.codelength(degrees, pattern, matches)));
		}
		
		{	// Query 5
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n1 = pattern.add(5320),
    				                 n2 = pattern.add(-1),
    				                 n3 = pattern.add(-2);
    		
    		n1.connect(n2, year);
    		n1.connect(n3, type);
    
    		tic();
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		System.out.println("query 5, time:        " + Functions.toc() + " seconds."); // 4.2s in rdflib    		
    		System.out.println("query 5, num results: " + matches.size());
    		
    		matches = MotifCode.prune(pattern, matches);
    		System.out.println(format("Matches, after pruning: %d", matches.size()));
    		
    		System.out.println(format("     Motif code length: %.3f ", MotifCode.codelength(degrees, pattern, matches)));
		}
		
	}

	
	@Test
	public void randomGraphTest()
	{
		int motifSize = 3,
			motifLinks = 2,
			motifVNodes = 3,
			motifVLinks = 0,
			numRelations = 2,
			n = 1000, m = 5000,
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
		
//		for(Triple t : triples)
//			System.out.println(t);
				
		KGraph data = new KGraph(triples);
		
		Global.info(String.format("Sampled data. (%d, %d)", data.size(), data.numLinks()) );
		
		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
		Global.info("Codelength under null model " + nullBits);
		
		List<List<Integer>> matches = Find.find(pattern, data);
		Global.info("Pattern matches: " + matches.size());
		
		matches = MotifCode.prune(pattern, matches);
		Global.info("after pruning: " + matches.size());
		
		double patternBits = MotifCode.codelength(KGraph.degrees(data), pattern, matches);
		Global.info("logfactor with target pattern " + (nullBits - patternBits));
	}
	
	@Test
	public void randomGraphTestSingleTriple()
	{
		int numRelations = 2,
			n = 1000, m = 5000;
		
		// * Create the motif
		
		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
		DTNode<Integer, Integer> m1 = pattern.add(-1),
		                         m2 = pattern.add(-2);
		
		m1.connect(m2, -3);
		
		Global.info("Pattern: " + pattern);
		
		// * Sample the data
		DGraph<?> topology = RandomGraphs.randomDirectedFast(n, m);
		
		List<Triple> triples = new ArrayList<>();
		
		for(DLink<?> link : topology.links())
			triples.add(t(
					link.from().index(), 
					Global.random().nextInt(numRelations), 
					link.to().index()));
				
		KGraph data = new KGraph(triples);
		
		Global.info(String.format("Sampled data. (%d, %d)", data.size(), data.numLinks()) );
		
		System.out.println("NULL.");
		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
		System.out.println("DONE.");
		Global.info("Codelength under null model " + nullBits);
		
		// - Doig this manually, because the search is a bit slow with this pattern
		Set<Triple> trips = data.find(null, null, null);
		List<List<Integer>> matches = new ArrayList<>(trips.size());
		for(Triple t : trips)
			matches.add(asList(t.subject(), t.object(), t.predicate()));
		
		Global.info("Pattern matches: " + matches.size());
		
		matches = MotifCode.prune(pattern, matches);
		Global.info("after pruning: " + matches.size());
		
		double patternBits = MotifCode.codelength(KGraph.degrees(data), pattern, matches);
		Global.info("Motif model bits: " + patternBits); // This should be close to the null bits

		Global.info("logfactor with target pattern " + (nullBits - patternBits));
	}
	
	/**
	 * Check that a random graph never produce a meaningful compression
	 */
	@Test
	public void randomGraphTest2()
	{
		int REPEATS = 100;
		for(int rep : series(REPEATS))
		{
    		int motifSize = 3,
    			motifLinks = 2,
    			motifVNodes = 3,
    			motifVLinks = 0,
    			numRelations = 2,
    			n = 4000, m = 20000;
    		
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
    				
    		KGraph data = new KGraph(triples);
    		
    		Global.info(String.format("Sampled data. (%d, %d)", data.size(), data.numLinks()) );
    		
    		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
    		Global.info("Codelength under null model:  " + nullBits);
    		
    		List<List<Integer>> matches = Find.find(pattern, data);
    		Global.info("Pattern matches: " + matches.size());
    		
    		matches = MotifCode.prune(pattern, matches);
    		Global.info("after pruning: " + matches.size());
    		
    		double patternBits = MotifCode.codelength(KGraph.degrees(data), pattern, matches);
    		Global.info("Codelength under motif model: " + patternBits);
    
    		Global.info("logfactor with target pattern " + (nullBits - patternBits));
    		
    		assert(nullBits < patternBits + 5);
		}
	}
	
	/**
	 * Check that a random graph never produce a meaningful compression, even
	 * under search
	 */
	@Test
	public void randomGraphTestSA()
	{
		int motifSize = 3,
			motifLinks = 2,
			motifVNodes = 3,
			motifVLinks = 0,
			numRelations = 2,
			n = 4000, m = 20000;
		
		// * Sample the data
		DGraph<?> topology = RandomGraphs.randomDirectedFast(n, m);
		
		List<Triple> triples = new ArrayList<>();
		
		for(DLink<?> link : topology.links())
			triples.add(t(
					link.from().index(), 
					Global.random().nextInt(numRelations), 
					link.to().index()));
				
		KGraph data = new KGraph(triples);
		
		Global.info(String.format("Sampled data. (%d, %d)", data.size(), data.numLinks()) );
		
		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
		Global.info("Codelength under null model:  " + nullBits);
		
		SimAnnealing search = new SimAnnealing(data, 0.7);
		int ITERATIONS = 1000;
		for (int it : series(ITERATIONS))
		{
			nl.peterbloem.kit.Functions.dot(it, ITERATIONS);
			search.iterate();
		}
		
		DTGraph<Integer, Integer> pattern = search.byScore(1).get(0);
		double patternBits = search.score(pattern);
		Global.info("Best Pattern" + pattern);
		Global.info("Codelength under motif model:  " + patternBits);

		
		assert(nullBits < patternBits + 10);
	}
	
	@Test
	public void matchesSortTest()
	{
		List<List<Integer>> matches = asList(
			asList(0, 0, 5),
			asList(0, 1, 3),
			asList(0, 0, 4),
			asList(0, 0, 2),
			asList(0, 0, 1),
			asList(0, 1, 3),
			asList(0, 0, 5),
			asList(0, 0, 3),
			asList(0, 0, 4),
			asList(0, 1, 2),
			asList(0, 0, 1)	
		); 
		
		MotifCode.sort(matches);
		
		for(List<Integer> match : matches)
			System.out.println(match);
	}
	
	@Test
	public void testPatternDegrees()
	{
		DTGraph<Integer, Integer> pattern = new MapDTGraph<Integer, Integer>();
		DTNode<Integer, Integer> m1 = pattern.add(-1),
				                 m2 = pattern.add(-2),
				                 n1 = pattern.add( 3);
		m1.connect(m2, -3);
		m1.connect(n1, -4);
		
		List<List<Integer>> values = asList(
				asList(2, 2, 1, 1),
				asList(3, 3, 0, 0),
				asList(4, 4, 1, 0),
				asList(5, 5, 0, 0),
				asList(6, 6, 1, 0),
				asList(7, 7, 0, 0),
				asList(9, 2, 1, 0)
		);
		
		Map<?, ?> degrees = MotifCode.patternDegrees(values, pattern, 10, 2);
		
		System.out.println(degrees);
	}
}
