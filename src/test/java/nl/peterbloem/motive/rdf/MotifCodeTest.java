package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;

import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class MotifCodeTest
{

	@Test
	public void testSynth()
	{
		int MIDDLE = 10000;
		KGraph graph = Datasets.test(MIDDLE);
		
		{
    		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
    		DTNode<Integer, Integer> n0 = pattern.add(0),
    				                 n1 = pattern.add(1),
    				                 v = pattern.add(-1);
    		
    		v.connect(n0, 0);
    		v.connect(n1, 1);
    
    		List<List<Integer>> matches = Find.find(pattern, graph);
    		assertEquals(MIDDLE, matches.size());
    		
    		List<List<Integer>> degrees = KGraph.degrees(graph);

    		double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
    		System.out.println(nullBits);
    		
    		matches = MotifCode.prune(pattern, matches);
    		System.out.println(MotifCode.codelength(degrees, pattern, matches));
		}
	}

}
