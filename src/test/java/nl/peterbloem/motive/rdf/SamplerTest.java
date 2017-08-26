package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.DGraph;
import org.nodes.DTGraph;
import org.nodes.LightDGraph;

import nl.peterbloem.kit.IntegerModel;
import nl.peterbloem.kit.OnlineModel;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.PitmanYorModel;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;
import nl.peterbloem.motive.rdf.KGraph.KLink;
import nl.peterbloem.motive.rdf.KGraph.KNode;

public class SamplerTest
{

	@Test
	public void testTriples()
	{
		DTGraph<Integer, Integer> g1 = NautyTest.graph1();
	
		List<Integer> values = Arrays.asList(11, 12, 13, 14);
		
		List<Triple> expected = Arrays.asList(
				new Triple(1, 13, 11), new Triple(12, 13, 11), 
				new Triple(0, 0, 1), new Triple(0, 1, 12), 
				new Triple(0, 14, 11));
		
		assertEquals(expected, Sampler.triples(g1, values));
	
	}
	
	@Test 
	public void testRun()
	{
		KGraph data = Datasets.dogfood();
		System.out.println(data.get(46900));
		System.out.println(data.get(46900).inDegree() + " " + data.get(46900).outDegree());
			
		System.out.println(data.get(46900).linksOut());
		System.out.println(data.get(46900).linksIn());

		
		List<List<Integer>> degrees = KGraph.degrees(data);

		double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
		
		System.out.println("Size under null model (ML Bound) " +  nullBits);
		
		Sampler sampler = new Sampler(data, 1000000, 3, 5, 5, 5);
		
		for(DTGraph<Integer, Integer> pattern : sampler.patterns().subList(0, Math.min(sampler.patterns().size(), 25)))
		{
			System.out.println(
					sampler.instances(pattern).size() + " " +
					sampler.instances(pattern).get(0) + " " + pattern);
			
			double motifBits = MotifCode.codelength(degrees, pattern, sampler.instances(pattern));
			System.out.println(motifBits);
			System.out.println("compression factor: " + (nullBits - motifBits));

		}
	}

}
