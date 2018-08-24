package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nodes.data.Data;
import org.nodes.data.Examples;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.KGraph.KNode;

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
	
	public static KGraph test()
	{
		return test(150, new ArrayList<String>(), new ArrayList<String>());
	}
	
	public static KGraph test(int middle)
	{
		return test(middle, new ArrayList<String>(), new ArrayList<String>());
	}
	
	public static KGraph test(int middle, List<String> labels, List<String> tags)
	{
		
		List<Triple> triples = new ArrayList<Triple>(middle + 2);
		int yes = 0, no = 1; 
		
		for(int node : series(2, middle + 2))
		{
			
//			node.connect(Global.random().nextBoolean() ? yes : no, 0);
//			node.connect(Global.random().nextBoolean() ? yes : no, 1);
//			node.connect(Global.random().nextBoolean() ? yes : no, 2);
			
			triples.add(Triple.t(node, 0, yes));
			triples.add(Triple.t(node, 1, no));
		}
		
		KGraph g = new KGraph(triples);
		
		for(KNode node : g.nodes())
			labels.add(""+node.label());
		
		for(int tag : g.tags())
			tags.add(""+tag);
		
		return g;
	}

}
