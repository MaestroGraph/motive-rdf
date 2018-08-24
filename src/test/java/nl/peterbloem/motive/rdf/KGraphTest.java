package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Triple.t;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.FastWalkable;
import org.nodes.LightDGraph;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.data.Data;
import org.nodes.data.Examples;
import org.nodes.random.SimpleSubgraphGenerator;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.KGraph.KLink;
import nl.peterbloem.motive.rdf.KGraph.KNode;

public class KGraphTest
{

	@Test
	public void testKGraph()
	{
		DTGraph<Integer, Integer> graph = new KGraph(emptyList());
	}

	@Test
	public void testToString()
	{
		List<Triple> triples = 
				asList(t(0, 0, 1));
		
		KGraph graph = new KGraph(triples);
		
		System.out.println(graph);
	}

	@Test
	public void starTest()
	{
		
		List<Triple> triples = 
				asList(
					t(0, 0, 1),
					t(0, 0, 2),
					t(0, 0, 3),
					t(0, 0, 4),
					t(0, 0, 5)
				);
		
		KGraph graph = new KGraph(triples);
		
		System.out.println(graph);
		
		System.out.println(graph);
		System.out.println(graph.get(4).index());
		
//		e.disconnect(a);
//		
//		System.out.println(graph);
//		System.out.println(e.index());
//		
//		a.remove();
//		
//		System.out.println(graph);
//		
//		System.out.println(graph.size());
//		assertEquals(0, graph.numLinks());
	}
	
//	@Test
//	public void testRemove()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add(),
//              b = graph.add(),
//              c = graph.add(),
//              d = graph.add(),
//              e = graph.add();
//	
//		b.connect(a);
//		c.connect(a);
//		d.connect(a);
//		e.connect(a);
//		
//		System.out.println(graph.numLinks() + " " + graph.size());
//		
//		assertEquals(4, graph.numLinks());
//		assertEquals(5, graph.size());
//		
//		a.remove();
//		
//		assertEquals(0, graph.numLinks());
//		assertEquals(4, graph.size());
//		
//		for(int i : series(graph.size()))
//			assertEquals(i, graph.get(i).index());
//	}
	
//	@Test
//	public void testRemove2()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add(),
//		      b = graph.add(),
//		      c = graph.add(),
//		      d = graph.add();
//	
//		a.connect(b);
//		b.connect(c);
//		c.connect(d);
//		d.connect(a);
//				
//		b.remove();
//		
//		assertFalse(graph.get(0).connectedTo(graph.get(0)));
//		assertFalse(graph.get(0).connectedTo(graph.get(1)));
//		assertFalse(graph.get(0).connectedTo(graph.get(2)));
//		assertFalse(graph.get(1).connectedTo(graph.get(0)));
//		assertFalse(graph.get(1).connectedTo(graph.get(1)));
//		assertTrue (graph.get(1).connectedTo(graph.get(2)));
//		assertTrue (graph.get(2).connectedTo(graph.get(0)));
//		assertFalse(graph.get(2).connectedTo(graph.get(1)));
//		assertFalse(graph.get(2).connectedTo(graph.get(2)));
//		
//		System.out.println(graph);
//	}
	
	@Test
	public void testTripleFind()
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
		
		assertEquals(graph.find(null, null, null).size(), 8);
	
		assertEquals(graph.find(0, 0, 1).size(), 1);
		assertEquals(graph.find(0, 1, 1).size(), 0);
		assertEquals(graph.find(4, 1, 5).size(), 1);
		assertEquals(graph.find(4, 0, 5).size(), 1);
		
		assertEquals(graph.find(null, 0, 0).size(), 0);
		assertEquals(graph.find(null, 0, 1).size(), 1);
		assertEquals(graph.find(null, 0, 3).size(), 2);

		assertEquals(graph.find(0, null, 4).size(), 0);
		assertEquals(graph.find(0, null, 1).size(), 1);
		assertEquals(graph.find(4, null, 5).size(), 2);
	
		assertEquals(graph.find(3, 0, null).size(), 0);
		assertEquals(graph.find(3, 1, null).size(), 1);
		assertEquals(graph.find(0, 0, null).size(), 2);
		
		assertEquals(graph.find(3, null, null).size(), 1);
		assertEquals(graph.find(null, 0, null).size(), 5);
		assertEquals(graph.find(null, 1, null).size(), 3);
		assertEquals(graph.find(null, 2, null).size(), 0);
		assertEquals(graph.find(0, null, null).size(), 3);
	}
	
	@Test
	public void testConnected()
	{
		List<Triple> triples = 
				asList(
					t(0, 0, 1),
					t(0, 0, 2)
				);
		
		KGraph graph = new KGraph(triples);
	
		KNode a = graph.get(0), 
		      b = graph.get(1),
		      c = graph.get(2);
		
		assertTrue(a.connected(b));
		assertTrue(a.connectedTo(b));
		
		assertFalse(a.connected(a));
		assertFalse(a.connectedTo(a));

		assertTrue(b.connected(a));
		assertFalse(b.connectedTo(a));
		
		assertTrue(a.connected(c));
		assertTrue(a.connectedTo(c));

		assertTrue(c.connected(a));
		assertFalse(c.connectedTo(a));
		
		assertFalse(b.connected(c));
		assertFalse(b.connectedTo(c));
		
		assertFalse(c.connected(b));
		assertFalse(c.connectedTo(b));
	}
	
	@Test
	public void testLinks()
	{
		List<Triple> triples = 
				asList(
					t(0, 0, 1),
					t(0, 0, 2),
					t(0, 0, 2)
				);
		
		KGraph graph = new KGraph(triples);
	
		KNode a = graph.get(0), 
		      b = graph.get(1),
		      c = graph.get(2);
				
		assertEquals(0, a.links(a).size());
		assertEquals(1, a.links(b).size());
		assertEquals(1, b.links(a).size());
		
		// * Note that the mechanics differ from the adjecency list implementation here
		assertEquals(1, a.links(c).size());
		assertEquals(1, c.links(a).size());
	}	
	
//	@Test
//	public void testLinkRemove()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add(),
//		      b = graph.add(),
//		      c = graph.add();
//		
//		a.connect(b);
//		
//		a.connect(c);
//		a.connect(c);
//		
//		KLink link = a.links(c).iterator().next();
//		link.remove();
//		
//		assertEquals(2, graph.numLinks());
//		assertEquals(1, a.links(c).size());
//		assertTrue(link.dead());
//
//		int n = 0;
//		for(KLink l : graph.links())
//			n++;
//		assertEquals(2, n);
//	}	
	
	@Test
	public void testEquals()
	{
		List<Triple> triples = 
				asList(
					t(0, 0, 1),
					t(1, 0, 2)
				);
		
		KGraph g1 = new KGraph(triples);
		reverse(triples);
		KGraph g2 = new KGraph(triples);	
         
		assertTrue(g1.equals(g2));
			
		
		triples = 
				asList(
					t(0, 0, 1),
					t(1, 1, 2)
				);
		
		KGraph g3 = new KGraph(triples);
		
		assertFalse(g1.equals(g3));
		assertFalse(g2.equals(g3));
		assertFalse(g3.equals(g1));
		assertFalse(g3.equals(g2));
		
		triples = 
				asList(
					t(0, 0, 1),
					t(1, 0, 2),
					t(0, 0, 2)
				);
		
		KGraph g4 = new KGraph(triples);
		
		assertFalse(g1.equals(g4));
		assertFalse(g2.equals(g4));
		assertFalse(g3.equals(g4));
		assertFalse(g4.equals(g1));
		assertFalse(g4.equals(g2));
		assertFalse(g4.equals(g3));
	}
	
	@Test
	public void testNotEquals()
	{
		DGraph<String> g1 = new LightDGraph<String>();
		
		KGraph g2 = new KGraph(Collections.emptyList());
		
		assertFalse(g1.equals(g2));
		assertFalse(g2.equals(g1));	
	}
	
	@Test
	public void testImport()
			throws IOException
	{
		KGraph graph = Datasets.dogfood();
		
		System.out.println(graph.size());
		System.out.println(graph.numLinks());
	}
	
	@Test
	public void testTags()
			throws IOException
	{
		KGraph graph = Datasets.dogfood();

		Set<Integer> tags = new LinkedHashSet<Integer>();
		for(KLink link : graph.links())
			tags.add(link.tag());
		
		List<Integer> t1 = new ArrayList<>(tags);
		List<Integer> t2 = new ArrayList<>(graph.tags());
		
		Collections.sort(t1);
		Collections.sort(t2);
		
		assertEquals(t1, t2);
		
	}
	
	//@Test
	public void testImportBig()
			throws IOException
	{
		KGraphList graph = KGraphList.loadHDT(new File("/Users/Peter/Dropbox/Datasets/RDF/dbpedia/mappingbased-dbpedia.en.2015-10.hdt"));
		
		System.out.println(graph.size());
		System.out.println(graph.numLinks());

	}
	
	@Test
	public void testCopy()
	{
		
		List<Triple> triples = 
				asList(
					t(0, 0, 0),
					t(1, 0, 2),
					t(0, 0, 2),
					t(2, 0, 0)
				);
		
		
		KGraph graph = new KGraph(triples);
		    
		KNode a = graph.get(0),
		      b = graph.get(1),
		      c = graph.get(2);
		      
		{
			int numLinks = 0;
			for(KLink link : graph.links())
				numLinks++;
	
			assertEquals(4, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}

	}
	
	// @Test I'll write it when I need it.
	public void testCopy2()
	{
		DGraph<String> graph = Examples.physicians();
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(1098, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}

		graph = LightDGraph.copy(graph);
		
		{
			int numLinks = 0;
			for(Link<String> link : graph.links())
				numLinks++;
			
			assertEquals(1098, numLinks);
			assertEquals(graph.numLinks(), numLinks);
		}
	}		
	
	
//	/**
//	 * 
//	 */
//	@Test
//	public void testIndices2()
//	{		
//		KGraph graph = Datasets.dogfood();
//
//		for(int x : series(50))
//		{
//			// Note that light graphs have non-persistent nodes, so node.index() 
//			// doesn't update after removal  
//			
//			KNode node = graph.get(145);
//			assertEquals(145, node.index());
//			
//			graph.get(150).remove(); // edit causes an exception
//			
//			boolean exThrown = false;
//			try {
//				System.out.println(node.index());
//			} catch(Exception e)
//			{
//				exThrown = true;
//			}
//	
//			assertTrue(exThrown);
//			
//			// * Do some random removals
//			for(int i : Series.series(10))
//			{
//				// - remove a random node
//				graph.get(Global.random().nextInt(graph.size())).remove();
//				
//				// - remove a random link
//				KNode a = graph.get(Global.random().nextInt(graph.size()));
//				
//				if(! a.neighbors().isEmpty())
//				{
//					KNode b = Functions.choose(a.neighbors());
//					KLink link = Functions.choose(a.links(b));
//					link.remove();
//				}
//			}
//			
//			int i = 0;
//			for(KNode n : graph.nodes())
//				assertEquals(i++, n.index());
//		}
//	}
	
	@Test
	public void testNodeLinks()
	{
		KGraph graph = Datasets.dogfood();
	
		for(KNode node : graph.nodes())
		{
			Collection<KNode> nbs = node.neighbors();
						
			for(KNode neighbor : nbs)
				assertTrue(node.links(neighbor).size() > 0);
		}
	}
	
	@Test
	public void testNodeLinks2()
	{
		
		List<Triple> triples = 
				asList(
					t(0, 0, 0),
					t(1, 0, 2),
					t(2, 0, 0),
					t(0, 0, 2)
				);
		
		KGraph graph = new KGraph(triples);

//		a.connect(a);
//		b.connect(c);
//		c.connect(a);
//		a.connect(c);
//		a.connect(c);
		
		{
			KNode node = graph.get(0);
			Collection<KNode> nbs = node.neighbors();
			assertEquals(2, nbs.size());
			
			assertEquals(1, node.links(graph.get(0)).size());
			assertEquals(0, node.links(graph.get(1)).size());			
			assertEquals(2, node.links(graph.get(2)).size());
		}
		
		{
			KNode node = graph.get(1);
			Collection<KNode> nbs = node.neighbors();
			assertEquals(1, nbs.size());
			
			assertEquals(0, node.links(graph.get(0)).size());
			assertEquals(0, node.links(graph.get(1)).size());			
			assertEquals(1, node.links(graph.get(2)).size());
		}
		
		{
			KNode node = graph.get(2);
			Collection<KNode> nbs = node.neighbors();
			assertEquals(2, nbs.size());
			
			assertEquals(2, node.links(graph.get(0)).size());
			assertEquals(1, node.links(graph.get(1)).size());			
			assertEquals(0, node.links(graph.get(2)).size());
		}
	}
	
	@Test
	public void testNeighbors()
	{
		List<Triple> triples = 
				asList(
					t(0, 0, 0),
					t(1, 0, 2),
					t(2, 0, 0),
					t(0, 0, 2)
				);
		
		KGraph graph = new KGraph(triples);
		
		KNode a = graph.get(0),
		      b = graph.get(1),
		      c = graph.get(2);
		
		Set<KNode> aNbsExpected = new HashSet<KNode>(Arrays.asList(a, c));
		Set<KNode> bNbsExpected = new HashSet<KNode>(Arrays.asList(c));
		Set<KNode> cNbsExpected = new HashSet<KNode>(Arrays.asList(b, a));
		
		Set<KNode> aNbsActual = new HashSet<KNode>(a.neighbors());
		Set<KNode> bNbsActual = new HashSet<KNode>(b.neighbors());
		Set<KNode> cNbsActual = new HashSet<KNode>(c.neighbors());

		assertEquals(aNbsExpected, aNbsActual);
		assertEquals(bNbsExpected, bNbsActual);
		assertEquals(cNbsExpected, cNbsActual);
	}
	
//	@Test
//	public void testNeighborsFast()
//	{
//		KGraphList graph = Datasets.dogfood();
//	
//		assertTrue(graph instanceof FastWalkable);
//		
//		for(KNode node : graph.nodes())
//		{
//			Collection<KNode> nbs = node.neighbors();
//			
//			Collection<KNode> col = graph.neighborsFast(node);
//			Set<KNode> nbsFast = new HashSet<KNode>(col);
//			
//			assertTrue(col instanceof List<?>);
//			
//			List<Integer> nbsList = new ArrayList<Integer>();
//			for(KNode nod : nbs)
//				nbsList.add(nod.index());
//			
//			List<Integer> nbsFastList = new ArrayList<Integer>();
//			for(KNode nod : nbsFast)
//				nbsFastList.add(nod.index());
//			
//			Collections.sort(nbsList);
//			Collections.sort(nbsFastList);
//			
//			assertEquals(nbsList, nbsFastList);			
//		}
//	}
	
//	@Test
//	public void testSort()
//	{
//		KGraph data = Datasets.test();
//
//		data.sort();
//		
//		for(KNode node : data.nodes())
//		{
//			
//			assertTrue(sorted(data.neighborsFastIn(node)));
//			
//			assertTrue(sorted(data.neighborsFastOut(node)));
//
//		}
//	}

//	public static boolean sorted(List<KNode> list)
//	{
//		if(list.isEmpty())
//			return true;
//		
//		for(int i : series(list.size() - 1))
//			if(list.get(i).index() > list.get(i+1).index())
//				return false;
//		
//		return true;
//	}
}
