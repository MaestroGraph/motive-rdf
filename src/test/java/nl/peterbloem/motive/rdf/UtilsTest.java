package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Functions.natural;
import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public class UtilsTest
{

	
	@Test
	public void reTest()
	{
		int n = 10;
		for(int i : series(1000000))
			n %= Utils.randExp(Global.random().nextInt(25) + 1);
		System.out.println(n);
	}
	
	@Test
	public void bellTest()
	{
		assertEquals(1,    Utils.bell(0));
		assertEquals(1,    Utils.bell(1));
		assertEquals(2,    Utils.bell(2));
		assertEquals(5,    Utils.bell(3));
		assertEquals(15,   Utils.bell(4));
		assertEquals(52,   Utils.bell(5));
		assertEquals(203,  Utils.bell(6));
		assertEquals(877,  Utils.bell(7));
		assertEquals(4140, Utils.bell(8));
	}

	@Test
	public void partitionTest()
	{
		for(int size : Series.series(2, 6))
		{
			System.out.println(size);
    		List<Integer> set = Series.series(size);
    		
    		FrequencyModel<Map<Integer, Integer>> fm = new FrequencyModel<>();
    		
    		for(int i : series(1000000))
    			fm.add(Utils.partition(set, natural()));
    		
    		assertEquals(Utils.bell(set.size()), (int)fm.distinct());
    		
    		Double last = null;
    		for(Map<Integer, Integer> token : fm.tokens())
    		{
    			double prob = fm.probability(token);
    			if(last != null)
    				assertEquals(prob, last, 0.1);
    			
    			last = prob;
    		}
		}
		
	}
	
	
	@Test
	public void maskTest()
	{
		List<Integer> list = Arrays.asList(0, 1, 1, 1, 0, 0, 0, 1);
		List<Integer> mask = Arrays.asList(1, 2, 3, 4);
		List<Integer> exp  = Arrays.asList(0, 1, 2, 3, 0, 0, 0, 4);
		
		Utils.applyMask(list, 1, mask);
		
		assertEquals(exp, list);
	}
	
	@Test
	public void getMaskTest()
	{
		Map<Integer, Integer> partition = new HashMap<>();
		partition.put(0, 1);
		partition.put(1, 1);
		partition.put(2, 0);
		partition.put(3, 2);
				
		assertEquals(Arrays.asList(-10, -10, -3, -11), Utils.getMask(partition, -3, -10));
	}
	
	@Test
	public void patternTest()
	{
		DTGraph<Integer, Integer> p;
		List<Integer> v;
		
		p = new MapDTGraph<Integer, Integer>();
		
		p.add(0);
		p.add(1);
		p.add(-1);
		
		p.node(0).connect(p.node(1), 1);
		p.node(1).connect(p.node(-1), 1);
		p.node(-1).connect(p.node(0), 1);
		
		v = new ArrayList<>(asList(11));
		
		assertEquals(2, patternTest(p, v));
		
		p = new MapDTGraph<Integer, Integer>();
		
		p.add(-1);
		p.add(-2);
		p.add(-3);
		
		p.node(-1).connect(p.node(-2), 1);
		p.node(-1).connect(p.node(-3), 2);
		p.node(-1).connect(p.node(-3), 3);
		
		v = new ArrayList<>(asList(11, 22, 33));
		
		assertEquals(10, patternTest(p, v));

		p = new MapDTGraph<Integer, Integer>();
		
		p.add(1);
		p.add(2);
		p.add(3);
		
		p.node(1).connect(p.node(2), -1);
		p.node(1).connect(p.node(3), -1);
		p.node(1).connect(p.node(3), -2);
		
		v = new ArrayList<>(asList(100, 200));
		
		assertEquals(2, patternTest(p, v));
		
		p = new MapDTGraph<Integer, Integer>();
		
		p.add(-1);
		p.add(-2);
		p.add(-3);
		
		p.node(-1).connect(p.node(-2), -1);
		p.node(-1).connect(p.node(-3), -1);
		p.node(-1).connect(p.node(-3), -2);
		
		v = new ArrayList<>(asList(11, 22, 33, 100, 200));

		assertEquals(20, patternTest(p, v));
	}

		
	public int patternTest(DTGraph<Integer, Integer> p, List<Integer> v)
	{		
		FrequencyModel<DTGraph<Integer, Integer>> fm = new FrequencyModel<>();
		
		Set<DTGraph<Integer, Integer>> set = new HashSet<>();

		for(int i : series(100))
		{
			List<Integer> values = new ArrayList<>(v);

			DTGraph<Integer, Integer> sample = Utils.partition(p, values);

	
			set.add(sample);
			fm.add(sample);
			
			Set<String> vars = new LinkedHashSet<>();
			int minLabel = 0, minTag = 0;
			
			for(DTNode<Integer, Integer> node : sample.nodes())
				if(node.label() < 0)
				{
					vars.add(node.label()+"n");
					minLabel = Math.min(minLabel, node.label());
				}
			
			for(DTLink<Integer, Integer> link : sample.links())
				if(link.tag() < 0)
				{
					vars.add(link.tag() + "t");
					minTag = Math.min(minTag, link.tag());
				}
			
			assertEquals(vars.size(), - (minLabel + minTag));
			assertEquals(values.size(), vars.size());
		}

		Double last = null;
		for(DTGraph<Integer, Integer> token : fm.tokens())
		{
			double prob = fm.probability(token);
			if(last != null)
				assertEquals(prob, prob, 0.1);
			
			last = prob;
		}
		
		return (int)fm.distinct();
		
	}
	
}
