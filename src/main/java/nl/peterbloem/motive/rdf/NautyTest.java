package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Graphs;
import org.nodes.LightDGraph;
import org.nodes.MapDTGraph;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Order;
import nl.peterbloem.kit.Series;

public class NautyTest
{

	public DTGraph<Integer, Integer> graph1()
	{
		DTGraph<Integer, Integer> graph = new MapDTGraph<>();
		
		DTNode<Integer,Integer> n1 = graph.add(1),
		                        m2 = graph.add(-2),
		                        m1 = graph.add(-1),
		                        n0 = graph.add(0);
		
		n0.connect(n1, 0);
		n0.connect(m2, 1);
		n0.connect(m1, -2);
		n1.connect(m1, -1);
		m2.connect(m1, -1);
	
		return graph;
	}
	
	public DTGraph<Integer, Integer> graph2()
	{
		DTGraph<Integer, Integer> graph = new MapDTGraph<>();
		
		DTNode<Integer,Integer> n4 = graph.add(4),
		                        n5 = graph.add(5),
		                        n0 = graph.add(0);
		
		n4.connect(n5, -2);
		n4.connect(n0, 3);
		n5.connect(n0, 3);
		n0.connect(n4, -3);
		n0.connect(n5, -1);
	
		return graph;
	}
	
	
// expected <digraph {-2 -> -5 [label=0]; -5 -> -4 [label=0]; -3 -> -5 [label=0]; -4 -> -3 [label=0]; -4 -> -2 [label=0]}> 
//      was <digraph {-1 -> -2 [label=0]; -2 -> -3 [label=0]; -3 -> -4 [label=0]; -3 -> -1 [label=0]; -4 -> -2 [label=0]}>

	public DTGraph<Integer, Integer> graph3()
	{
		DTGraph<Integer, Integer> graph = new MapDTGraph<>();
		
		DTNode<Integer,Integer> n2 = graph.add(-2),
		                        n3 = graph.add(-3),
		                        n4 = graph.add(-4),
		                        n5 = graph.add(-5);
		
		n2.connect(n5, 0);
		n5.connect(n4, 0);
		n3.connect(n5, 0);
		n4.connect(n3, 0);
		n4.connect(n2, 0);
	
		return graph;
	}

	
// expected: <digraph {-2 -> -5 [label=0]; -5 -> -4 [label=0]; -3 -> -5 [label=0]; -4 -> -3 [label=0]; -4 -> -2 [label=0]}> 
//  but was: <digraph {-1 -> -4 [label=0]; -4 -> -3 [label=0]; -2 -> -4 [label=0]; -3 -> -2 [label=0]; -3 -> -1 [label=0]}>

	public DTGraph<Integer, Integer> graph4()
	{
		DTGraph<Integer, Integer> graph = new MapDTGraph<>();
		
		DTNode<Integer,Integer> n2 = graph.add(-2),
		                        n3 = graph.add(-3),
		                        n4 = graph.add(-4),
		                        n5 = graph.add(-5);
		
		n2.connect(n5, 0);
		n5.connect(n4, 0);
		n3.connect(n5, 0);
		n4.connect(n3, 0);
		n4.connect(n2, 0);
	
		return graph;
	}
	

	
	public DTGraph<Integer, Integer> random(int n, int m, int numTags, int nVar, int mVar)
	{
		for(int j : series(500))
		{
             DGraph<String> str = RandomGraphs.randomDirectedFast(n, m);
             MapDTGraph<Integer, Integer> result = new MapDTGraph<>();
            
             List<Integer> labels = Functions.sampleInts(n, n*2);
             
             List<Integer> vars = Functions.sampleInts(nVar, labels.size());	 
             for(int var : vars)
            	 labels.set(var, - labels.get(var));
             
             for(int label : labels)
            	 result.add(label);
             
             labels = new ArrayList<Integer>(m);
             for(int i : Series.series(m))
            	 labels.add(Global.random().nextInt(numTags));
             
             vars = Functions.sampleInts(mVar, labels.size());
             for(int var : vars)
            	 labels.set(var, - labels.get(var));
            	 
             int i = 0;
             for(DLink<String> link : str.links())
             {
            	 int from = link.from().index(), to = link.to().index();
            	 result.get(from).connect(result.get(to), labels.get(i++));
             }
             
             if(Graphs.connected(result))
            	 return result;
		}
		
		throw new IllegalStateException("Could not sample a connected graph.");
	}
	
	/**
	 * Returns the same graph with the variable labels and tags shuffled.
	 * @param in
	 * @return
	 */
	public DTGraph<Integer, Integer> permute(DTGraph<Integer, Integer> in)
	{
		List<Integer> vlabels = new ArrayList<>();
		for(int label : in.labels())
			if(label < 0)
				vlabels.add(label);
		
		List<Integer> vtags = new ArrayList<>();
		for(int tag : in.tags())
			if(tag < 0)
				vtags.add(tag);
		
		List<Integer> slabels = new ArrayList<>(vlabels);
		List<Integer> stags = new ArrayList<>(vtags);
		
		Collections.shuffle(slabels);
		Collections.shuffle(stags);
		
		Map<Integer, Integer> labelMap = new HashMap<>();
		Map<Integer, Integer> tagMap = new HashMap<>();
		
		for(int i : series(vlabels.size()))
			labelMap.put(vlabels.get(i), slabels.get(i));
		for(int i : series(vtags.size()))
			tagMap.put(vtags.get(i), stags.get(i));

//		System.out.println(labelMap);
//		System.out.println(tagMap);
		
		
		DTGraph<Integer, Integer> nw = new MapDTGraph<>();
		
		for(DTNode<Integer, Integer> node : in.nodes())
		{
			int label = node.label();
			nw.add(label < 0 ? labelMap.get(label) : label);
		}
		
		for(DTLink<Integer, Integer> link : in.links())
		{
			int tag = link.tag();
			nw.get(link.from().index()).connect(
					nw.get(link.to().index()), 
					tag < 0 ? tagMap.get(link.tag()) : tag);
		}

		return nw;
	}
	
	@Test
	public void testStructure()
	{
		DTGraph<Integer,Integer> g1 = graph1();		
		DTGraph<Integer,Integer> g1c = Nauty.canonical(g1);
		
		System.out.println(g1);
		System.out.println(g1c);
			
		DGraph<String> g1b = LightDGraph.copy(Graphs.blank(g1, ""));
		DGraph<String> g1bc = LightDGraph.copy(Graphs.blank(g1c, ""));

		Order order;
		
		order = org.nodes.algorithms.Nauty.order(g1b, Functions.natural());
		g1b = Graphs.reorder(g1b,  order);
		order = org.nodes.algorithms.Nauty.order(g1bc, Functions.natural());
		g1bc = Graphs.reorder(g1bc,  order);
		
		assertEquals(g1b, g1bc);
	}

	@Test
	public void testGraph2()
	{
		Nauty.canonical(graph2());		
	}
	
	@Test
	public void testGraph3Call()
	{		
		DTGraph<Integer, Integer> g1 = Nauty.canonical(graph3());
	}
	
	@Test
	public void testGraph3()
	{		
		DTGraph<Integer, Integer> g1 = Nauty.canonical(graph3());
		
		Order order = Order.random(g1.size());
		DTGraph<Integer, Integer> g2 = Graphs.reorder(graph3(),  order);
		g2 = permute(g2);
		
		System.out.println();
		
		g2 = Nauty.canonical(g2);

		assertEquals(g1, g2);
	}
	
	@Test
	public void testPermutedG1()
	{
		DTGraph<Integer, Integer> g1 = graph1();
		g1 = Nauty.canonical(g1);
		
		
		Order order = Order.random(g1.size());
		DTGraph<Integer, Integer> g2 = Graphs.reorder(graph1(),  order);
		g2 = permute(g2);
		g2 = Nauty.canonical(g2);

		
		assertEquals(g1, g2);
	}
		
	
	@Test
	public void testIsomorphic()
	{
		DTGraph<Integer, Integer> o = Nauty.canonical(graph1());
	
		assertTrue(o.node(0).connectedTo(o.node(1), 0));
		assertTrue(
				o.node(0).connectedTo(o.node(-2), 1) || 
				o.node(0).connectedTo(o.node(-1), 1));
		// assertTrue(o.node(0).connectedTo(o.node(-1), -2));
		// assertTrue(o.node(1).connectedTo(o.node(-1), -1));
		// assertTrue(o.node(-2).connectedTo(o.node(-1), -1));
		
	}
	
	@Test
	public void testCanonicalPermutedFixed()
	{
		testCanonicalPermuted(100, 5, 5, 1, 5, 5);
		testCanonicalPermuted(100, 5, 5, 1, 0, 0);
		testCanonicalPermuted(100, 3, 5, 4, 0, 3);
	}
	
	@Test
	public void test5Nodes()
	{
		
		
	}
	
	
	@Test
	public void testCanonicalPermutedRandom()
	{
		
		for(int rep : series(1000))
		{
			int n = Global.random().nextInt(10) + 3;
			int m = Global.random().nextInt(Math.min(36, n * n - n - n)) + n;
			int numTags = m > 1 ? Global.random().nextInt(m-1) + 1: 1;
			int nVar = Global.random().nextInt(Math.min(n, 7));
			int mVar = Global.random().nextInt(m);

			System.out.println(n +" " + m + " " + numTags + " " + nVar + " " + mVar);
			testCanonicalPermuted(50, n, m, numTags, nVar, mVar);
		}
	}

	// @Test
	public void testCanonicalPermutedSlow()
	{
		Functions.tic();
		testCanonicalPermuted(1, 11, 4, 3, 10, 0);
		System.out.println(Functions.toc() + " seconds");
	}
	
	@Test
	public void testCanonicalPermutedTiming()
	{
		int r = 100;
		Functions.tic();
		testCanonicalPermuted(r, 11, 4, 3, 8, 0);
		
		System.out.println();
		System.out.println((Functions.toc() / (double)r) + " seconds");
	}
	
	public void testCanonicalPermuted(int repeats, int n, int m, int numTags, int nVar, int mVar)
	{

		for(int i : Series.series(repeats))
		{
    		DTGraph<Integer, Integer> r1o = random(n, m, numTags, nVar, mVar);
    		DTGraph<Integer, Integer> r1 = Nauty.canonical(r1o);
    		
    		
    		Order order = Order.random(r1.size());
    		DTGraph<Integer, Integer> r2 = Graphs.reorder(r1,  order);
    		r2 = permute(r2);
    		r2 = Nauty.canonical(r2);
    
    		// System.out.println(r1o);
    		assertEquals(r1, r2);
    		
    		Functions.dot(i, repeats);
		}
	}
}
