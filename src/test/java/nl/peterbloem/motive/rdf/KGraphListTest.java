//package nl.peterbloem.motive.rdf;
//
//import static nl.peterbloem.kit.Series.series;
//import static org.junit.Assert.*;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashSet;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.junit.Test;
//import org.nodes.DGraph;
//import org.nodes.DLink;
//import org.nodes.DNode;
//import org.nodes.DTGraph;
//import org.nodes.FastWalkable;
//import org.nodes.LightDGraph;
//import org.nodes.Link;
//import org.nodes.MapDTGraph;
//import org.nodes.Node;
//import org.nodes.data.Data;
//import org.nodes.data.Examples;
//import org.nodes.random.SimpleSubgraphGenerator;
//
//import nl.peterbloem.kit.Functions;
//import nl.peterbloem.kit.Global;
//import nl.peterbloem.kit.Series;
//import nl.peterbloem.motive.rdf.KGraphList.KLink;
//import nl.peterbloem.motive.rdf.KGraphList.KNode;
//
//public class KGraphListTest
//{
//
//	@Test
//	public void testKGraph()
//	{
//		DTGraph<Integer, Integer> graph = new KGraphList();
//	}
//
//	@Test
//	public void testToString()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add(),
//		      b = graph.add();
//		graph.add();
//	
//		a.connect(b);
//		
//		System.out.println(graph);
//	}
//
//	@Test
//	public void starTest()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add(),
//		      b = graph.add(),
//		      c = graph.add(),
//		      d = graph.add(),
//		      e = graph.add();
//	
//		b.connect(a);
//		c.connect(a);
//		d.connect(a);
//		e.connect(a);
//		
//		System.out.println(graph);
//		System.out.println(e.index());
//		
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
//	}
//	
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
//	
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
//	
//	@Test
//	public void testConnected()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add(),
//		      b = graph.add(),
//		      c = graph.add();
//	
//		a.connect(b);
//		a.connect(c);
//		
//		assertTrue(a.connected(b));
//		assertTrue(a.connectedTo(b));
//		
//		assertFalse(a.connected(a));
//		assertFalse(a.connectedTo(a));
//
//		assertTrue(b.connected(a));
//		assertFalse(b.connectedTo(a));
//		
//		assertTrue(a.connected(c));
//		assertTrue(a.connectedTo(c));
//
//		assertTrue(c.connected(a));
//		assertFalse(c.connectedTo(a));
//		
//		assertFalse(b.connected(c));
//		assertFalse(b.connectedTo(c));
//		
//		assertFalse(c.connected(b));
//		assertFalse(c.connectedTo(b));
//	}
//	
//	@Test
//	public void testLinks()
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
//		assertEquals(0, a.links(a).size());
//		assertEquals(1, a.links(b).size());
//		assertEquals(1, b.links(a).size());
//		assertEquals(2, a.links(c).size());
//		assertEquals(2, c.links(a).size());
//	}	
//	
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
//	
//	@Test
//	public void testEquals()
//	{
//		KGraphList g1 = new KGraphList();
//		g1.add();
//		g1.add();
//		g1.add();
//		
//		g1.node(0).connect(g1.node(1));
//		g1.node(1).connect(g1.node(2));
//		
//		KGraphList g2 = new KGraphList();
//		g2.add();
//		g2.add();
//		g2.add();
//		 
//		g2.node(0).connect(g2.node(1));                    
//		g2.node(1).connect(g2.node(2));		
//         
//		assertTrue(g1.equals(g2));
//		
//		g2.node(0).connect(g2.node(2));
//	
//		assertFalse(g1.equals(g2));
//	}
//	
//	@Test
//	public void testNotEquals()
//	{
//		DGraph<String> g1 = new LightDGraph<String>();
//		
//		KGraphList g2 = new KGraphList();
//		
//		assertFalse(g1.equals(g2));
//		assertFalse(g2.equals(g1));	
//	}
//	
//	@Test
//	public void testImport()
//			throws IOException
//	{
//		KGraphList graph = Datasets.dogfood();
//		
//		System.out.println(graph.size());
//		System.out.println(graph.numLinks());
//	}
//	
//	@Test
//	public void testTags()
//			throws IOException
//	{
//		KGraphList graph = Datasets.dogfood();
//
//		Set<Integer> tags = new LinkedHashSet<Integer>();
//		for(KLink link : graph.links())
//			tags.add(link.tag());
//		
//		List<Integer> t1 = new ArrayList<>(tags);
//		List<Integer> t2 = new ArrayList<>(graph.tags());
//		
//		Collections.sort(t1);
//		Collections.sort(t2);
//		
//		assertEquals(t1, t2);
//		
//	}
//	
//	//@Test
//	public void testImportBig()
//			throws IOException
//	{
//		KGraphList graph = KGraphList.loadHDT(new File("/Users/Peter/Dropbox/Datasets/RDF/dbpedia/mappingbased-dbpedia.en.2015-10.hdt"));
//		
//		System.out.println(graph.size());
//		System.out.println(graph.numLinks());
//
//	}
//	
//	@Test
//	public void testCopy()
//	{
//		KGraphList graph = new KGraphList();
//		    
//		KNode a = graph.add();
//		KNode b = graph.add();
//		KNode c = graph.add();
//
//		a.connect(a);
//		b.connect(c);
//		c.connect(a);
//		a.connect(c);
//		a.connect(c);
//		
//		{
//			int numLinks = 0;
//			for(KLink link : graph.links())
//				numLinks++;
//	
//			assertEquals(5, numLinks);
//			assertEquals(graph.numLinks(), numLinks);
//		}
//
////		KGraph graph = KGraph.copy(graph);
////
////		{
////			int numLinks = 0;
////			for(Link<String> link : graph.links())
////				numLinks++;
////	
////			assertEquals(5, numLinks);
////			assertEquals(graph.numLinks(), numLinks);
////		}
//	}
//	
//	// @Test I'll write it when I need it.
//	public void testCopy2()
//	{
//		DGraph<String> graph = Examples.physicians();
//		{
//			int numLinks = 0;
//			for(Link<String> link : graph.links())
//				numLinks++;
//			
//			assertEquals(1098, numLinks);
//			assertEquals(graph.numLinks(), numLinks);
//		}
//
//		graph = LightDGraph.copy(graph);
//		
//		{
//			int numLinks = 0;
//			for(Link<String> link : graph.links())
//				numLinks++;
//			
//			assertEquals(1098, numLinks);
//			assertEquals(graph.numLinks(), numLinks);
//		}
//	}		
//	
//	@Test
//	public void testNumLinks2()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add();
//		KNode b = graph.add();
//		KNode c = graph.add();
//
//		a.connect(a);
//		b.connect(c);
//		c.connect(a);
//		a.connect(c);
//		a.connect(c);
//		
//		int numLinks = 0;
//		for(KLink link : graph.links())
//			numLinks++;
//		
//		assertEquals(5, numLinks);
//		assertEquals(graph.numLinks(), numLinks);
//	}
//	
//	/**
//	 * 
//	 */
//	@Test
//	public void testIndices2()
//	{		
//		KGraphList graph = Datasets.dogfood();
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
//	
//	@Test
//	public void testNodeLinks()
//	{
//		KGraphList graph = Datasets.dogfood();
//	
//		for(KNode node : graph.nodes())
//		{
//			Collection<KNode> nbs = node.neighbors();
//						
//			for(KNode neighbor : nbs)
//				assertTrue(node.links(neighbor).size() > 0);
//		}
//	}
//	
//	@Test
//	public void testNodeLinks2()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add();
//		KNode b = graph.add();
//		KNode c = graph.add();
//
//		a.connect(a);
//		b.connect(c);
//		c.connect(a);
//		a.connect(c);
//		a.connect(c);
//		
//		{
//			KNode node = graph.get(0);
//			Collection<KNode> nbs = node.neighbors();
//			assertEquals(2, nbs.size());
//			
//			assertEquals(1, node.links(graph.get(0)).size());
//			assertEquals(0, node.links(graph.get(1)).size());			
//			assertEquals(3, node.links(graph.get(2)).size());
//		}
//		
//		{
//			KNode node = graph.get(1);
//			Collection<KNode> nbs = node.neighbors();
//			assertEquals(1, nbs.size());
//			
//			assertEquals(0, node.links(graph.get(0)).size());
//			assertEquals(0, node.links(graph.get(1)).size());			
//			assertEquals(1, node.links(graph.get(2)).size());
//		}
//		
//		{
//			KNode node = graph.get(2);
//			Collection<KNode> nbs = node.neighbors();
//			assertEquals(2, nbs.size());
//			
//			assertEquals(3, node.links(graph.get(0)).size());
//			assertEquals(1, node.links(graph.get(1)).size());			
//			assertEquals(0, node.links(graph.get(2)).size());
//		}
//	}
//	
//	@Test
//	public void testNeighbors()
//	{
//		KGraphList graph = new KGraphList();
//		
//		KNode a = graph.add();
//		KNode b = graph.add();
//		KNode c = graph.add();
//
//		a.connect(a);
//		b.connect(c);
//		c.connect(a);
//		a.connect(c);
//		a.connect(c);
//		
//		Set<KNode> aNbsExpected = new HashSet<KNode>(Arrays.asList(a, c));
//		Set<KNode> bNbsExpected = new HashSet<KNode>(Arrays.asList(c));
//		Set<KNode> cNbsExpected = new HashSet<KNode>(Arrays.asList(b, a));
//		
//		Set<KNode> aNbsActual = new HashSet<KNode>(a.neighbors());
//		Set<KNode> bNbsActual = new HashSet<KNode>(b.neighbors());
//		Set<KNode> cNbsActual = new HashSet<KNode>(c.neighbors());
//
//		assertEquals(aNbsExpected, aNbsActual);
//		assertEquals(bNbsExpected, bNbsActual);
//		assertEquals(cNbsExpected, cNbsActual);
//	}
//	
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
//	
//	@Test
//	public void testSort()
//	{
//		KGraphList data = Datasets.test();
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
//
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
//}
