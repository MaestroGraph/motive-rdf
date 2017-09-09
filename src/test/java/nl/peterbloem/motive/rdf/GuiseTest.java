package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.KGraphTest.sorted;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.collections.impl.lazy.parallel.set.sorted.SynchronizedParallelSortedSetIterable;
import org.junit.Test;
import org.nodes.Graph;
import org.nodes.MapDTGraph;
import org.nodes.random.SimpleSubgraphGenerator;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.KGraph.KNode;

public class GuiseTest
{

	@Test
	public void testUnion()
	{
		List<List<Integer>> lists = new ArrayList<>(3);
		
		lists.add(asList(1, 1, 3, 4, 8, 9, 10, 11, 12));
		lists.add(asList(1, 2, 4, 9, 11, 13));
		lists.add(asList(3, 4, 7, 9, 11, 18));
		
		assertEquals(asList(1, 2, 3, 4, 7, 8, 9, 10, 11, 12, 13, 18), Guise.union(lists, Functions.natural()));
	}
	
	@Test
	public void testUnion2()
	{
		for(int i : Series.series(50000))	
		{
    		List<List<Integer>> lists = new ArrayList<>(3);
    		
    		int n = Global.random().nextInt(500);
    		
    		for(int j : series(n))
    		{
    			int k = Global.random().nextInt(50);
    			List<Integer> ints = Functions.sampleInts(k, 50);
    			Collections.sort(ints);
    			lists.add(ints);
    		}
    		
    		List<Integer> result = Guise.union(lists, Functions.natural());
    		
    		Set<Integer> union = new HashSet<>();
    		for(List<Integer> list : lists)
    			union.addAll(list);
    		List<Integer> naive = new ArrayList<>(union);
    		Collections.sort(naive);
    		
    		assertEquals(naive, result);
		}
	}
	
	@Test
	public void testUnion3()
	{
		KGraph data = Datasets.test();
		data.sort();

		for(int size : series(2, 8))
		{
			System.out.println(size);
    		SimpleSubgraphGenerator gen = new SimpleSubgraphGenerator(data, size);
    		
    		for(int i : series(100))
    		{
    			List<Integer> current = gen.generate();
    			
    			List<List<KNode>> adjLists = new ArrayList<>(current.size());
    			
    			for(int index : current)
    			{
    				adjLists.add(data.neighborsFastIn(data.get(index)));
    				adjLists.add(data.neighborsFastOut(data.get(index)));
    				
    				assertTrue(sorted(data.neighborsFastIn(data.get(index))));
    				assertTrue(sorted(data.neighborsFastOut(data.get(index))));
    			}
    			
    			Comparator<KNode> comp = new Comparator<KNode>(){
    				public int compare(KNode x, KNode y)
    				{ return Integer.compare(x.index(), y.index());
    				}}; 
    			
    			List<KNode> union = Guise.union(adjLists, comp);
    			
        		Set<KNode> set = new HashSet<>();
        		for(List<KNode> list : adjLists)
        			set.addAll(list);
        		List<KNode> naive = new ArrayList<>(set);
        		Collections.sort(naive, comp);
        		
        		try {
        			assertEquals(naive, union);
        		} catch(AssertionError e)
        		{
        			for(List<KNode> list : adjLists)
        				System.out.println(list);
        			throw e;
        		}
    		}
		}
	}
	
	@Test
	public void testUnion4()
	{
		List<List<Integer>> lists = new ArrayList<>();
		
		lists.add(asList(15));
		lists.add(asList());
		lists.add(asList(14, 14, 20, 43));
		lists.add(asList(43, 45, 58, 46, 14, 59, 58));
		
		for(List<Integer> list :lists)
			Collections.sort(list);
			
		List<Integer> union = Guise.union(lists, Functions.natural());
				
		Set<Integer> set = new HashSet<>();
		for(List<Integer> list :lists)
			set.addAll(list);
		List<Integer> naive = new ArrayList<>(set);
		Collections.sort(naive);

		assertEquals(naive, union);
	}
	
	@Test
	public void testIntersect()
	{
		List<List<Integer>> lists = new ArrayList<>(3);
		
		lists.add(asList(1, 1, 3, 4, 8, 9, 10, 11, 12));
		lists.add(asList(1, 2, 4, 9, 11, 13));
		lists.add(asList(3, 3, 4, 7, 9, 11, 18));
		
		assertEquals(asList(4, 9, 11), Guise.intersect(lists, Functions.natural()));
	}
	
	@Test
	public void testIntersect2()
	{
		List<List<Integer>> lists = new ArrayList<>(3);
		
		int n = Global.random().nextInt(500);
		
		for(int i : series(n))
		{
			int k = Global.random().nextInt(50);
			List<Integer> ints = Functions.sampleInts(k, 50);
			Collections.sort(ints);
			lists.add(ints);
		}
		
		List<Integer> result = Guise.intersect(lists, Functions.natural());
		
		Set<Integer> union = new HashSet<>(lists.get(0));
		for(List<Integer> list : lists)
			union.retainAll(list);
		List<Integer> naive = new ArrayList<>(union);
		Collections.sort(naive);
		
		assertEquals(naive, result);
	}

	
	@Test
	public void testCount()
	{
		KGraph g = new KGraph();
		
		g.add();
		g.add();
		g.add();
		g.add();
		g.add();
		
		g.add();
		g.add();
		g.add();
		g.add();
		g.add();
		
		g.get(0).connect(g.get(1));
		g.get(1).connect(g.get(2));
		g.get(2).connect(g.get(3));
		g.get(3).connect(g.get(4));
		g.get(4).connect(g.get(0));

		g.get(5).connect(g.get(6));
		g.get(6).connect(g.get(7));
		g.get(7).connect(g.get(8));
		g.get(8).connect(g.get(9));
		g.get(9).connect(g.get(5));
		
		g.get(0).connect(g.get(5));
		
		g.sort();
		
		assertEquals(3, Guise.count(asList(0, 1, 2), g));
		assertEquals(3, Guise.count(asList(5, 6, 7, 8), g));
		assertEquals(4, Guise.count(asList(5, 6, 7, 8, 9), g));
	}
	
	@Test
	public void testSelect()
	{
		KGraph g = new KGraph();
		
		g.add();
		g.add();
		g.add();
		g.add();
		g.add();
		
		g.add();
		g.add();
		g.add();
		g.add();
		g.add();
		
		g.get(0).connect(g.get(1));
		g.get(1).connect(g.get(2));
		g.get(2).connect(g.get(3));
		g.get(3).connect(g.get(4));
		g.get(4).connect(g.get(0));

		g.get(5).connect(g.get(6));
		g.get(6).connect(g.get(7));
		g.get(7).connect(g.get(8));
		g.get(8).connect(g.get(9));
		g.get(9).connect(g.get(5));
		
		g.get(0).connect(g.get(5));
		
		g.sort();

		for(int k : series(4))
			System.out.println(Guise.select(k, asList(5, 6, 7, 8, 9), g));
		
		for(int k : series(3))
			System.out.println(Guise.select(k, asList(0, 1, 2), g));
	}
	
	@Test
	public void generatorTest()
	{
		
		KGraph data = Datasets.test();
		
		Guise gen = new Guise(data, 3, 10);
		
		for(int i : series(1000))
		{
			List<Integer> cur = gen.generate(); 
			if(cur.contains(0) && cur.contains(1))
				System.out.println(cur);
		}
		
		SimpleSubgraphGenerator gen2 = new SimpleSubgraphGenerator(data, 3);
		
		for(int i : series(1000))
		{
			List<Integer> cur = gen2.generate(); 
			if(cur.contains(0) && cur.contains(1))
				System.out.println(cur);
		}
	}
}
