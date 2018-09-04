package nl.peterbloem.motive.rdf.exec;

import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Triple.t;

import java.awt.geom.GeneralPath;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.EdgeListModel;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;
import nl.peterbloem.motive.rdf.Find;
import nl.peterbloem.motive.rdf.KGraph;
import nl.peterbloem.motive.rdf.MotifCode;
import nl.peterbloem.motive.rdf.Triple;
import nl.peterbloem.motive.rdf.Utils;

public class SynthRep
{
	public int maxSearchTime = 120;
	public static int finished = 0;
	public static int totalThreads = 0;
	
	public int repeats = 20;

	// TODO: Dimension based on AIFB, Mutag and dogfood
	public int[] sizes = new int[]{8285, 23664, 7611};
	public int[] numsLinks = new int[]{29226, 74567, 242256};
	public int[] numsRelations = new int[]{47, 24, 170};

	public int maxInstances = 50;
				
	public int motifMinSize = 3;
	public int motifMaxSize = 8;
	
	public Vector<List<? extends Number>> results = new Vector<>();
	
	/**
	 * This experiment create a random graph with k instances of a random motif 
	 * inserted and checks whether the motif creates is recognized under the 
	 * motif code as compressing 
	 * @throws IOException 
	 */
	public void main() throws IOException
	{
		Global.info("Starting.");
		Global.randomSeed();
		
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		for(int i : series(numsLinks.length))
			for(int inst : series(maxInstances))
				for(int rep : series(repeats))
				{
					exec.execute(new Run(sizes[i], numsLinks[i], inst, numsRelations[i]));
					totalThreads++;
				}
		
		exec.shutdown();
		while(! exec.isTerminated());
		System.out.println("All threads finished");
		
		Writer out = new BufferedWriter(new FileWriter(new File("synthrep.csv")));
		for(List<? extends Number> line : results)
		{
			boolean first = true;
			for(Number num : line)
			{
				if(first)
					first = false;
				else
					out.write(", ");
				
				out.write(num.toString());
			}
			
			out.write("\n");
		}
		
		out.close();
		
		Global.info("Finished.");
	}
	
	/**
	 * Sample a random motif.
	 * 
	 * @param numNodes Nr of nodes in the knowledge graph  
	 * @param numRelations
	 * @param size
	 */
	public static DTGraph<Integer, Integer> generatePattern(int size, int graphNodes, int graphRelations)
	{
		DTGraph<Integer, Integer> pattern = null;
		
		while(pattern == null || ! Graphs.connected(pattern))
		{
    		int numLinks = rInt(size, size*size-size);
    		
    		int numVNodes = rInt(0, size);
    		int numVLinks = rInt(0, numLinks);
    		
    		// * Sample the motif
    		DGraph<?> topology = RandomGraphs.randomDirectedFast(size, numLinks);
    		List<Integer> vNodes = Functions.sampleInts(numVNodes, size);
    		List<Integer> vLinks = Functions.sampleInts(numVLinks, numLinks);
    		int nextVNode = -1, nextVLink = - numVNodes - 1;
    		
    		pattern = new MapDTGraph<>();
    		for(int i : series(size))
    		{
    			// figure out the node label
    			int label;
    			if(vNodes.contains(i))
    				label = nextVNode --;
    			else 
    				label = Global.random().nextInt(graphNodes);
    			pattern.add(label);
    		}
    		
    		int i = 0;
    		for(DLink<?> link : topology.links())
    		{
    			int from = link.from().index(), to = link.to().index();
    			int tag;
    			if(vLinks.contains(i))
    				tag = nextVLink --;
    			else
    				tag = Global.random().nextInt(graphRelations);
    			
    			pattern.get(from).connect(pattern.get(to), tag);
    			
    			i++;
    		}
		}
		
		return pattern;
	}
	
	public static int rInt(int from, int to)
	{
		return Global.random().nextInt(to-from) + from;
	}

	public class Run implements Runnable 
	{

		private int size;
		private int numLinks;
		private int numInstances;
		private int numRelations;
		
		public Run(int size, int numLinks, int numInstances, int numRelations)
		{
			this.size = size;
			this.numLinks = numLinks;
			this.numInstances = numInstances;
			this.numRelations = numRelations;
		}

		@Override
		public void run()
		{
			DTGraph<Integer, Integer> pattern = generatePattern(rInt(motifMinSize, motifMaxSize), size, numLinks);
						
			DGraph<?> topology = RandomGraphs.randomDirectedFast(size, numLinks);
			List<Triple> triples = new ArrayList<>();
			
			for(DLink<?> link : topology.links())
				triples.add(t(
						link.from().index(), 
						Global.random().nextInt(numRelations), 
						link.to().index()));
			
			for(int j : series(numInstances))
			{
				List<Integer> smp = nl.peterbloem.kit.Functions.sampleInts(Utils.numVarLabels(pattern), size);			
				for(DTLink<Integer, Integer> link : pattern.links())
				{
					int s = link.from().label(),
						p = link.tag(),
						o = link.to().label();
					
					if(s < 0)
						s = smp.get(-s - 1);
					if(p < 0)
						p = Global.random().nextInt(numRelations); // slight cheat because we know there are unique per triple
					if(o < 0)
						o = smp.get(-o - 1);
					
					triples.add(Triple.t(s, p, o));
				}
			}
		
			KGraph data = new KGraph(triples);
			List<List<Integer>> degrees = KGraph.degrees(data);
			
			double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
			
			List<List<Integer>> matches;
			
			if(maxSearchTime < 1)
				matches = Find.find(pattern, data);
			else
				matches = Find.find(pattern, data, maxSearchTime);
			
			int numMatches = matches.size();
			matches = MotifCode.prune(pattern, matches);
			int numMatchesPruned = matches.size();
			
			double motifBits = MotifCode.codelength(degrees, pattern, matches);
			
			results.add(Arrays.asList(
					size, numLinks, numRelations, numInstances, nullBits, motifBits,
					numMatches, numMatchesPruned));
			
			Global.info("thread finished. %d out of %d", ++ finished, totalThreads);
		}
		
	}
}
