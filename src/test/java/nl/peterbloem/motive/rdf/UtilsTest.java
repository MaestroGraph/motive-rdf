package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Functions.natural;
import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

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
    		}
		}
		
	}
	
}
