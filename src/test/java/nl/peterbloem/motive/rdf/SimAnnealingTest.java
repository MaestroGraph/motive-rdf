package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;

import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class SimAnnealingTest
{

	@Test
	public void testSynth()
	{
		int MIDDLE = 100;
		KGraph graph = Datasets.test(MIDDLE);
		
		double nullBits = EdgeListModel.codelength(KGraph.degrees(graph), Prior.ML);
		System.out.println("null model: " + nullBits);
		
		{
			SimAnnealing sim = new SimAnnealing(graph, 0.4);
			
			for(int i : series(1000))
			{
				System.out.println(String.format("iteration %d", i));
				sim.iterate();
			}
		}
	}
	
	@Test
	public void testDogfood()
	{
		KGraph graph = Datasets.dogfood();
		
		double nullBits = EdgeListModel.codelength(KGraph.degrees(graph), Prior.ML);
		System.out.println("null model: " + nullBits);
		
		{
			SimAnnealing sim = new SimAnnealing(graph, 0.4);
			
			for(int i : series(100000))
			{
				System.out.println(String.format("iteration %d", i));
				sim.iterate();
			}
		}
	}
}
