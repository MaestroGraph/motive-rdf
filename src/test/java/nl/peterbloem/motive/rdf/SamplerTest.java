//package nl.peterbloem.motive.rdf;
//
//import static nl.peterbloem.kit.Series.series;
//import static nl.peterbloem.motive.rdf.EdgeListModel.codelength;
//import static org.junit.Assert.*;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import org.junit.Test;
//import org.nodes.DGraph;
//import org.nodes.DTGraph;
//import org.nodes.LightDGraph;
//
//import nl.peterbloem.kit.Functions;
//import nl.peterbloem.kit.Global;
//import nl.peterbloem.kit.IntegerModel;
//import nl.peterbloem.kit.OnlineModel;
//import nl.peterbloem.kit.Pair;
//import nl.peterbloem.kit.PitmanYorModel;
//import nl.peterbloem.kit.Series;
//import nl.peterbloem.motive.rdf.EdgeListModel.Prior;
//import nl.peterbloem.motive.rdf.KGraphList.KLink;
//import nl.peterbloem.motive.rdf.KGraphList.KNode;
//
//public class SamplerTest
//{
//
//	@Test
//	public void testTriples()
//	{
//		DTGraph<Integer, Integer> g1 = NautyTest.graph1();
//	
//		List<Integer> values = Arrays.asList(11, 12, 13, 14);
//		
//		List<Triple> expected = Arrays.asList(
//				new Triple(1, 13, 11), new Triple(12, 13, 11), 
//				new Triple(0, 0, 1), new Triple(0, 1, 12), 
//				new Triple(0, 14, 11));
//		
//		assertEquals(expected, Utils.triples(g1, values));
//	
//	}
//	
//	@Test 
//	public void testCL()
//	{
//		KGraphList data = Datasets.dogfood();
//
//		System.out.println(codelength(data, Prior.COMPLETE));
//		for(int i : Series.series(100))
//		{
//			List<KLink> links = Functions.list(data.links());
//			int ind = Global.random().nextInt(links.size());
//			KLink link = links.remove(ind);
//			link.remove();
//			
//			System.out.println(codelength(data, Prior.COMPLETE));
//		}
//	}
//	
//	@Test 
//	public void testRun()
//	{
//		KGraphList data = Datasets.dogfood();
//		
//		List<List<Integer>> degrees = KGraphList.degrees(data);
//
//		double nullBits = EdgeListModel.codelength(degrees, Prior.COMPLETE);
//		
//		System.out.println("Size under null model (ML Bound) " +  nullBits);
//		
//		Sampler sampler = new Sampler(data, 1000000, 100000, 3, 5, 5, 5);
//		
//		for(DTGraph<Integer, Integer> pattern : sampler.patterns().subList(0, 25))
//		{
//			System.out.println(
//					sampler.instances(pattern).size() + " " +
//					sampler.instances(pattern).get(0) + " " + pattern);
//			
//			double motifBits = MotifCode.codelength(degrees, pattern, sampler.instances(pattern));
//			// System.out.println(motifBits);
//			System.out.println(" compression factor: " + (nullBits - motifBits));
//
//		}
//	}
//	
//	
//	@Test 
//	public void testRun2()
//	{
//		KGraphList data = Datasets.test();
//		
//		List<List<Integer>> degrees = KGraphList.degrees(data);
//		double nullBits = EdgeListModel.codelength(degrees, Prior.COMPLETE);
//		
//		System.out.println("Size under null model (ML Bound) " +  nullBits);
//		
//		Sampler sampler = new Sampler(data, 50000, 10000, 2, 4, 5, 1);
//		
//		for(DTGraph<Integer, Integer> pattern : sampler.patterns().subList(0, 25))
//		{
//			System.out.println(
//					sampler.instances(pattern).size() + " " +
//					sampler.instances(pattern).get(0) + " " + pattern);
//			
//			double motifBits = MotifCode.codelength(degrees, pattern, sampler.instances(pattern));
//			// System.out.println(motifBits);
//			System.out.println(" compression factor: " + (nullBits - motifBits));
//
//		}
//	}
//
//}
