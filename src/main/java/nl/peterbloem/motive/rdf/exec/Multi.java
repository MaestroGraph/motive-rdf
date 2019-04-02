package nl.peterbloem.motive.rdf.exec;

import static java.lang.String.format;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Pref.shorten;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.nodes.DTGraph;
import org.nodes.DTLink;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.MaxObserver;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.Datasets;
import nl.peterbloem.motive.rdf.EdgeListModel;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;
import nl.peterbloem.motive.rdf.GAMulti;
import nl.peterbloem.motive.rdf.KGraph;
import nl.peterbloem.motive.rdf.MultiParallel;
import nl.peterbloem.motive.rdf.Pref;
import nl.peterbloem.motive.rdf.SAParallel;
import nl.peterbloem.motive.rdf.SimAnnealing;
import nl.peterbloem.motive.rdf.SimAnnealingMulti;

public class Multi
{
	
	public String dataname;
	
	public int iterations;
	
	public int populationSize;
	
	public int topK;
	
	public int maxSearchTime;
	
	private KGraph data;
	
	private double nullBits;
	
	public int numThreads;
	
	private MaxObserver<GAMulti.MotifSet> observer;

	public void main()
		throws IOException, InterruptedException
	{
		// dataname = dataname.trim().toLowerCase();
		
		List<String> labels = new ArrayList<>(), tags = new ArrayList<>();
		if("dogfood".equals(dataname.toLowerCase()))
			data = Datasets.dogfood(labels, tags);
		else if ("aifb".equals(dataname.toLowerCase()))
			data = Datasets.aifb(labels, tags);
		else if("mutag".equals(dataname.toLowerCase()))
			data = Datasets.mutag(labels, tags);
		else if(new File(dataname).exists())
		{
			File dataFile = new File(dataname);
			data = KGraph.loadHDT(dataFile, labels, tags);
		} else
			throw new IllegalArgumentException(format("Dataset name %s not recognized", dataname));
		Global.info("Data loaded");
		
		Global.info("Computing baseline codelength");
		nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
		
		Global.info("Done. Searching.");
		Global.info("-- using %d separate processes.", numThreads);

		observer = new MaxObserver<>(topK, Collections.reverseOrder()); // retain the lowest scores
				
		ExecutorService exec = Executors.newFixedThreadPool(numThreads);
		for(int t : series(numThreads)) 
			exec.execute(new GARun());

		exec.shutdown();
		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				
		Global.info("Search finished.");
		
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("motifs-byscore.latex")));
		for(GAMulti.MotifSet result : observer.elements())
		{
			double factor = nullBits - result.score();
						
			out.write(String.format(" %.1f & \\makecell{", factor));
			
			for(DTGraph<Integer, Integer> motif : result.patterns())
			{
				DTGraph<String, String> m = KGraph.recover(motif, labels, tags);
    			boolean first = true;
    			for(DTLink<String, String> link : m.links())
    			{
    				if(first)
    					first = false;
    				else
    					out.write("\\\\");
    			
    				out.write("\\texttt{" + l(shorten(link.from().label())) +  " " + l(shorten(link.tag())) + " " + l(shorten(link.to().label())) + ".} ");
    			}
    			out.write("\\ldots \\\\");
			}
			
			out.write("} \\\\ \n");
		}
		
		out.close();
		
		out = new BufferedWriter(new FileWriter(new File("motifs-byscore.txt")));
		for(GAMulti.MotifSet result : observer.elements())
		{
			double factor = nullBits - result.score();
			boolean first = true;

			for(DTGraph<Integer, Integer> motif : result.patterns())
			{
				if (first)
					first = false;
				else
					out.write(" | ");
				
				DTGraph<String, String> m = KGraph.recover(motif, labels, tags);
    			for(DTLink<String, String> link : m.links())
    			{    			
    				out.write(shorten(link.from().label()) +  " " + shorten(link.tag()) + " " + shorten(link.to().label()) + ". ");
    			}
			}
			
			out.write("\n");
		}
		out.close();
	}
	
	/**
	 * Escape latex sensitive characters
	 * @param in
	 * @return
	 */
	public static String l(String in )
	{
		String res = in;
		res = res.replace("_", "\\_");
		res = res.replace("#", "\\_");
		res = res.replace("#", "\\~");

		return res;
	}
	
	private static int NUM = 0;

	private class GARun implements Runnable
	{
		private int id = NUM++;
		
		@Override
		public void run()
		{
			GAMulti search = new GAMulti(data, maxSearchTime, nullBits, populationSize, observer);

			for(int i : series(iterations))
			{
				search.iterate();
				
				if(i % 100 == 0)
					Global.info("thread %d says: %d iterations finished.", id, i);

			}

			Global.info("Thread finished searching");
		}		
	}
}
