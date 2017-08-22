package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.Graphs;
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
		                        n2 = graph.add(2),
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
	
	public DTGraph<Integer, Integer> random(int n, int m, int numTags, int nVar, int mVar)
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
		 
		 return result;
	}
	
	@Test
	public void testCall()
	{
		System.out.println(Nauty.canonical(graph1()));		
		
		for(int i : Series.series(100))
			Nauty.canonical(random(15, 25, 5, 5, 15));
	}

	@Test
	public void testGraph2()
	{
		Nauty.canonical(graph2());		
	}
		
	
	@Test
	public void testIsomorphic()
	{
		DTGraph<Integer, Integer> o = Nauty.canonical(graph1());
		
		System.out.println(o);		

		assertTrue(o.node(0).connectedTo(o.node(1), 0));
		assertTrue(o.node(0).connectedTo(o.node(-2), 1));
		assertTrue(o.node(0).connectedTo(o.node(-1), -2));
		assertTrue(o.node(1).connectedTo(o.node(-1), -1));
		assertTrue(o.node(-2).connectedTo(o.node(-1), -1));
		
	}
	
	@Test
	public void testCanonicalPermutedFixed()
	{
		// testCanonicalPermuted2(100, 5, 5, 1, 5, 5);
		// testCanonicalPermuted2(100, 5, 5, 1, 0, 0);
		testCanonicalPermuted(100, 3, 5, 4, 0, 3);

	}
	
	@Test
	public void test5Nodes()
	{
		
		
	}
	
	@Test
	public void testCanonicalPermutedRandom()
	{
		
		for(int rep : series(100))
		{
			int n = Global.random().nextInt(10) + 3;
			int m = Global.random().nextInt(Math.min(36, n * n - n)) + 1;
			int numTags = m > 1 ? Global.random().nextInt(m-1) + 1: 1;
			int nVar = Global.random().nextInt(Math.min(n, 7));
			int mVar = Global.random().nextInt(m);

			System.out.println(n +" " + m + " " + numTags + " " + nVar + " " + mVar);
			testCanonicalPermuted(50, n, m, numTags, nVar, mVar);
		}
	}

	@Test
	public void testCanonicalPermutedSlow()
	{
		testCanonicalPermuted(100, 11, 4, 3, 10, 0);			
	}
	
	public void testCanonicalPermuted(int repeats, int n, int m, int numTags, int nVar, int mVar)
	{

		for(int i : Series.series(repeats))
		{
    		DTGraph<Integer, Integer> r1 = random(n, m, numTags, nVar, mVar);
    		r1 = Nauty.canonical(r1);
    		
    		
    		Order order = Order.random(r1.size());
    		DTGraph<Integer, Integer> r2 = Graphs.reorder(r1,  order);
    		r2 = Nauty.canonical(r2);
    
    		
    		assertEquals(r1, r2);
    		
    		Functions.dot(i, repeats);
		}
	}
}
