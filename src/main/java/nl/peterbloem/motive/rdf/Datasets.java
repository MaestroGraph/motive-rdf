package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nodes.data.Data;

import nl.peterbloem.kit.FileIO;
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
		return getGraph("data/swdf-2012-11-28.hdt", labels, tags);
	}
	
	public static KGraph getGraph(String res, List<String> labels, List<String> tags)
	{
		ClassLoader classLoader = Datasets.class.getClassLoader();
		
		File file = new File(classLoader.getResource(res).getFile());
		InputStream instr = null;
		
		if(!file.exists())
		{
			file = null;
			instr = classLoader.getResourceAsStream(res);
		}
				
		try
		{
			if(instr == null)
				return KGraph.loadHDT(file, labels, tags);
			else 
				return KGraph.loadHDT(instr, labels, tags);
		} catch (IOException e)
		{
			throw new RuntimeException("Could not load the file or JAR resource for the Semantic Web dogfood graph from the classpath.", e);
		}
	}
	
	public static KGraph aifb()
	{
		return aifb(new ArrayList<String>(), new ArrayList<String>());
	}
	
	public static KGraph aifb(List<String> labels, List<String> tags)
	{
		return getGraph("data/aifb.complete.hdt", labels, tags);
	}
	
	public static KGraph mutag()
	{
		return mutag(new ArrayList<String>(), new ArrayList<String>());
	}
	
	public static KGraph mutag(List<String> labels, List<String> tags)
	{
		return getGraph("data/mutag.complete.hdt", labels, tags);
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
	public static KGraph test2(int middle)
	{
		return test2(middle, new ArrayList<String>(), new ArrayList<String>());
	}
	
	public static KGraph test2(int middle, List<String> labels, List<String> tags)
	{
		
		List<Triple> triples = new ArrayList<Triple>();
		int yes = 0, no = 1; 
	
		List<Integer> n2s = new ArrayList<>(series(middle+2, 2*middle+2));
		Collections.shuffle(n2s);
		
		for(int i : series(middle))
		{
			int node1 = i + 2;
			int node2 = n2s.get(i);
//			node.connect(Global.random().nextBoolean() ? yes : no, 0);
//			node.connect(Global.random().nextBoolean() ? yes : no, 1);
//			node.connect(Global.random().nextBoolean() ? yes : no, 2);
			
			triples.add(Triple.t(node1, 0, yes));
			triples.add(Triple.t(node1, 2, node2));
			triples.add(Triple.t(node2, 1, no));
		}
		
		KGraph g = new KGraph(triples);
		
		for(KNode node : g.nodes())
			labels.add(""+node.label());
		
		for(int tag : g.tags())
			tags.add(""+tag);
		
		return g;
	}

}
