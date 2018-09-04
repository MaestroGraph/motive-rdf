package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nodes.DTGraph;
import org.rdfhdt.hdt.iterator.SequentialSearchIteratorTripleID;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.MaxObserver;
import nl.peterbloem.kit.Series;

public class SAParallel
{
	
	private int itsFinished = 0;
	private int numThreads; 
	private int maxTime; 
	private double alpha;
	
	private KGraph graph;
	
	private List<SARun> annealers = new ArrayList<>();
	private int perThread;
	
	private Map<DTGraph<Integer, Integer>, Double>  scores = new LinkedHashMap<>();
	private Map<DTGraph<Integer, Integer>, Integer> frequencies = new LinkedHashMap<>();
	
	public SAParallel(KGraph graph, int totalIterations, double alpha, int maxTime)
	{
		this(graph, totalIterations, alpha, maxTime, Runtime.getRuntime().availableProcessors());
	}
	
	public SAParallel(KGraph graph,int totalIterations, double alpha, int maxTime, int numThreads)
	{
		this.graph = graph;
		this.alpha = alpha;
		this.maxTime = maxTime;
		this.perThread = totalIterations/numThreads;
		
		Global.info("Using %d separate processes.", numThreads);
		
		ExecutorService exec = Executors.newFixedThreadPool(numThreads);
		for(int t : series(numThreads)) 
			exec.execute(new SARun());

		exec.shutdown();
		
		while(! exec.isTerminated());
	}
	
	private class SARun implements Runnable
	{
		@Override
		public void run()
		{
			SimAnnealing search = new SimAnnealing(graph, alpha, maxTime);
			
			for (int i : series(perThread))
			{
				search.iterate();
				
				if(++ itsFinished % 500 == 0)
					Global.info("%d iterations finished.", itsFinished);
			}
			
			Global.info("Finished searching");
			
			register(search);
			
		}		
	}
	
	private synchronized void register(SimAnnealing search)
	{
		for(DTGraph<Integer, Integer> motif : search.scores().keySet())
		{
			double score = search.score(motif);
			int freq = search.frequency(motif);
			
			if(! scores.containsKey(motif) || scores.get(motif) > score)
			{
				scores.put(motif, score);
				frequencies.put(motif, freq);
			}	
		}
	}


	public List<DTGraph<Integer, Integer>> byScore(int k)
	{
		List<DTGraph<Integer, Integer>> motifs = new ArrayList<>(scores.keySet());
		
		Comparator<DTGraph<Integer, Integer>> comp = new Comparator<DTGraph<Integer, Integer>>()
		{
			@Override
			public int compare(DTGraph<Integer, Integer> m1, DTGraph<Integer, Integer> m2)
			{
				return Double.compare(scores.get(m1), scores.get(m2));
			}	
		};
		
		List<DTGraph<Integer, Integer>> result = MaxObserver.quickSelect(k, motifs, comp, false);
		Collections.sort(result, comp);
		
		return result;
	}
	
	public double score(DTGraph<Integer, Integer> motif)
	{
		return scores.get(motif);
	}
	
	public List<DTGraph<Integer, Integer>> byFrequency(int k)
	{
		List<DTGraph<Integer, Integer>> motifs = new ArrayList<>(scores.keySet());
		
		Comparator<DTGraph<Integer, Integer>> comp = new Comparator<DTGraph<Integer, Integer>>()
		{
			@Override
			public int compare(DTGraph<Integer, Integer> m1, DTGraph<Integer, Integer> m2)
			{
				return - Double.compare(frequencies.get(m1), frequencies.get(m2));
			}
		};
		
		List<DTGraph<Integer, Integer>> result = MaxObserver.quickSelect(k, motifs, comp, false);
		Collections.sort(result, comp);
		
		return result;
	}

	public int frequency(DTGraph<Integer, Integer> motif)
	{
		return frequencies.get(motif);
	}
		
	
}
