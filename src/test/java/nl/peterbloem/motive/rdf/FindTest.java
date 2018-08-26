package nl.peterbloem.motive.rdf;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static nl.peterbloem.motive.rdf.Triple.t;
import static org.junit.Assert.*;
import static org.nodes.compression.Functions.tic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;
import org.nodes.compression.Functions;

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
	
}
