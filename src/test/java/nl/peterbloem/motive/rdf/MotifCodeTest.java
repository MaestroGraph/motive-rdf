package nl.peterbloem.motive.rdf;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.nodes.compression.Functions.tic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;
import org.nodes.compression.Functions;

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
    		System.out.println(nullBits);
    		
    		matches = MotifCode.prune(pattern, matches);
    		System.out.println(MotifCode.codelength(degrees, pattern, matches));
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
		int MIDDLE = 100;
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

}
