package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Collection;
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
import nl.peterbloem.motive.rdf.SimAnnealingMulti.MotifSet;

public class MultiParallel
{
	private static int RETAIN = 100;
	
	private int numPos = 0;
	private Double nullBits = null;
	
	private int itsFinished = 0;
	private int numThreads; 
	private int maxTime; 
	private double alpha;
	
	// * how many motifs to register per thread
	// TODO: make parameter
	private int register = 1000;
	
	private KGraph graph;
	
	private List<MultiRun> annealers = new ArrayList<>();
	private int perThread;
	
	private MaxObserver<SimAnnealingMulti.MotifSet> observer;
	
	public MultiParallel(KGraph graph, int totalIterations, double alpha, int maxTime)
	{
		this(graph, totalIterations, alpha, maxTime, Runtime.getRuntime().availableProcessors());
	}
	
	public MultiParallel(KGraph graph, int totalIterations, double alpha, int maxTime, DTGraph<Integer, Integer> start)
	{	
		this(graph, totalIterations, alpha, maxTime, Runtime.getRuntime().availableProcessors(), start, null);
	}
	
	public MultiParallel(KGraph graph, int totalIterations, double alpha, int maxTime, DTGraph<Integer, Integer> start, Double nullBits)
	{	
		this(graph, totalIterations, alpha, maxTime, Runtime.getRuntime().availableProcessors(), start, nullBits);
	}

	public MultiParallel(KGraph graph, int totalIterations, double alpha, int maxTime, int numThreads)
	{
		this(graph, totalIterations, alpha, maxTime, numThreads, null, null);
	}
	
	public MultiParallel(
			KGraph graph, int totalIterations, double alpha, 
			int maxTime, int numThreads, DTGraph<Integer, Integer> start, 
			Double nullBits)
	{
		this.nullBits = nullBits;
		
		this.graph = graph;
		this.alpha = alpha;
		this.maxTime = maxTime;
		this.perThread = totalIterations/numThreads;
		this.observer = new MaxObserver<>(RETAIN, Collections.reverseOrder());
				
		Global.info("Using %d separate processes.", numThreads);
		
		ExecutorService exec = Executors.newFixedThreadPool(numThreads);
		for(int t : series(numThreads)) 
			exec.execute(new MultiRun());

		exec.shutdown();
		
		while(! exec.isTerminated());
		
		if(nullBits != null)
			Global.info("Encountered %d positive patterns.", numPos);
	}
	
	private static int TOTAL = 0;
	private class MultiRun implements Runnable
	{
		private int id = TOTAL++;
		
		@Override
		public void run()
		{
			SimAnnealingMulti search = 
					new SimAnnealingMulti(
							graph, alpha, maxTime, null, 
							nullBits, observer);
			
			for (int i : series(perThread))
			{
				search.iterate();
				
				if(++ itsFinished % 500 == 0)
					Global.info("thread %d: %d iterations finished.", id, itsFinished);
			}

			Global.info("Thread finished searching");
		}		
	}
	
	public Collection<SimAnnealingMulti.MotifSet> results()
	{
		return observer.elements();
	}
	
		
	
}
