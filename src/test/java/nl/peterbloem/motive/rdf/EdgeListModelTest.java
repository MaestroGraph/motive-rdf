package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nodes.models.DSequenceEstimator.D;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class EdgeListModelTest
{

	@Test
	public void testCase1()
	{
		List<List<Integer>> degrees = asList(asList(0, 1, 2), asList(2, 1, 0), asList(1, 2));
		
		double bits = EdgeListModel.codelength(degrees, Prior.NONE);
		
		System.out.println(bits + " " + Functions.exp2(bits));
		// assertTrue(bits > 0.0);
		
		bits = org.nodes.models.EdgeListModel.directed(asList(2, 1, 0), asList(0, 1, 2), org.nodes.models.DegreeSequenceModel.Prior.NONE);
		
		System.out.println(bits + " " + Functions.exp2(bits));
	}

}
