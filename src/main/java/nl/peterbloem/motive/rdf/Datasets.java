package nl.peterbloem.motive.rdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nodes.data.Data;
import org.nodes.data.Examples;

public class Datasets
{

	public static KGraph dogfood()
	{
		return dogfood(new ArrayList<String>(), new ArrayList<String>());
	}
	
	public static KGraph dogfood(List<String> labels, List<String> tags)
	{
		ClassLoader classLoader = Examples.class.getClassLoader();
		File file = new File(classLoader.getResource("data/swdf-2012-11-28.hdt").getFile());
		
		try
		{
			return KGraph.loadHDT(file, labels, tags);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file for the Semantic Web dogfood graph from the classpath.", e);
		}
	}

}
