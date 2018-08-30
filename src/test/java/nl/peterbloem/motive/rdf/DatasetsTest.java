package nl.peterbloem.motive.rdf;

import static org.junit.Assert.*;

import org.junit.Test;

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

}
