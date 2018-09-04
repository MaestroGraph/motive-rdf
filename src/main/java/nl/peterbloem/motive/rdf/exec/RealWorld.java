package nl.peterbloem.motive.rdf.exec;

import static java.lang.String.format;
import static nl.peterbloem.kit.Series.series;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.nodes.DTGraph;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.Datasets;
import nl.peterbloem.motive.rdf.KGraph;
import nl.peterbloem.motive.rdf.SAParallel;
import nl.peterbloem.motive.rdf.SimAnnealing;

public class RealWorld
{
	
	public String dataname;
	
	public int iterations;
	
	public double alpha;
	
	public int topK;
	
	public int maxSearchTime;

	public void main()
	{
		dataname = dataname.trim().toLowerCase();
		
		KGraph data;
		List<String> labels = new ArrayList<>(), tags = new ArrayList<>();
		if("dogfood".equals(dataname))
			data = Datasets.dogfood(labels, tags);
		else if ("aifb".equals(dataname))
			data = Datasets.aifb(labels, tags);
		else if("mutag".equals(dataname))
			data = Datasets.mutag(labels, tags);
		else
			throw new IllegalArgumentException(format("Dataset name %s not recognized"));
		
		SAParallel search = new SAParallel(data, iterations, alpha, maxSearchTime);

		Global.info("Search finished.");
		
		for(DTGraph<Integer, Integer> motif : search.byScore(topK))
		{
			
			DTGraph<String, String> m = data.recover(motif, labels, tags);
			System.out.println(m + ", " + search.score(motif) + ", "  + search.frequency(motif) );
		}
		
	}
	
	

}
