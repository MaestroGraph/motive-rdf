package nl.peterbloem.motive.rdf;

import static org.junit.Assert.*;

import org.junit.Test;

import EDU.oswego.cs.dl.util.concurrent.misc.SynchronizationTimer;

public class DatasetsTest
{

	@Test
	public void test2()
	{
		KGraph data = Datasets.test2(5);
		
		System.out.println(data.size()  + " " + data.numLinks());

		for(Triple t : data.find(null, 0, null))
			System.out.println(t);

		System.out.println();
		for(Triple t : data.find(null, 1, null))
			System.out.println(t);
		
		System.out.println();
		for(Triple t : data.find(null, 2, null))
			System.out.println(t);
	}
	
	@Test
	public void testDogfood()
	{
		KGraph data = Datasets.dogfood();
		
		System.out.println("##       dataset: dogfood");
		System.out.println("    nr. of nodes: " + data.size() );
		System.out.println("    nr. of links: " + data.numLinks());
		System.out.println("nr. of relations: " + data.tags().size());
	}
	
	@Test
	public void testAIFB()
	{
		KGraph data = Datasets.aifb();
		
		System.out.println("##       dataset: AIFB");
		System.out.println("    nr. of nodes: " + data.size() );
		System.out.println("    nr. of links: " + data.numLinks());
		System.out.println("nr. of relations: " + data.tags().size());
	}
	
	@Test
	public void testMutag()
	{
		KGraph data = Datasets.mutag();
		
		System.out.println("##       dataset: Mutag");
		System.out.println("    nr. of nodes: " + data.size() );
		System.out.println("    nr. of links: " + data.numLinks());
		System.out.println("nr. of relations: " + data.tags().size());
	}

}
