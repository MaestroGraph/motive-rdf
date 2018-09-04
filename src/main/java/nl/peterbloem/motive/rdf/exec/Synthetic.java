package nl.peterbloem.motive.rdf.exec;

import static nl.peterbloem.kit.Pair.p;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Triple.t;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.data.Data;
import org.nodes.models.ERSimpleModel;
import org.nodes.random.RandomGraphs;

import nl.peterbloem.kit.FileIO;
import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.MotifSearchModel;
import nl.peterbloem.motive.UPlainMotifExtractor;
import nl.peterbloem.motive.rdf.EdgeListModel;
import nl.peterbloem.motive.rdf.Find;
import nl.peterbloem.motive.rdf.KGraph;
import nl.peterbloem.motive.rdf.MotifCode;
import nl.peterbloem.motive.rdf.SAParallel;
import nl.peterbloem.motive.rdf.SimAnnealing;
import nl.peterbloem.motive.rdf.Triple;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;
import nl.peterbloem.motive.rdf.KGraph.KLink;
import nl.peterbloem.motive.rdf.KGraph.KNode;

/**
 * Sample a random graph, insert some motifs, and see if they can be recovered.
 * 
 * @author Peter
 *
 */
public class Synthetic
{
	/**
	 *  Size of the random graph in nodes
	 */
	public int n = 23644;
	/**
	 * Size of the random graph in links
	 */
	public int m = 74567;
	
	public int numRelations = 24;
	
	public int motifSize = 3;
	public int motifLinks = 3;
	
	public int motifVNodes = 3;
	public int motifVLinks = 0;
	
	public int maxTime = 10;
	
	/**
	 *  The number of instances to inject (multiple values)
	 */
	// public List<Integer> numsInstances = Arrays.asList(0, 10, 100);
	public int[] numInstances = new int[]{0, 10, 100};
	public int focus = 1; // which element from numInstances to sort by
	
	public double alpha = 0.5;
	
	/**
	 * How many motifs to write to the CSV files  
	 */
	public int toCSV = 200;
			
	public int iterations = 10000000; // 1000000;
	
	public int topK = 50000;
	
	public Map<DTGraph<Integer, Integer>, List<Double>> scores = new LinkedHashMap<>();
	public Map<DTGraph<Integer, Integer>, List<Integer>> frequencies = new LinkedHashMap<>();
			
	public void main() throws IOException
	{		
		// * Sample the motif
		DGraph<?> topology = RandomGraphs.randomDirectedFast(motifSize, motifLinks);
		List<Integer> vNodes = Functions.sampleInts(motifVNodes, motifSize);
		List<Integer> vLinks = Functions.sampleInts(motifVLinks, motifSize);
		int nextVNode = -1, nextVLink = - motifVNodes -1;
		
		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
		for(int i : series(motifSize))
		{
			// figure out the node label
			int label;
			if(vNodes.contains(i))
				label = nextVNode --;
			else 
				label = Global.random().nextInt(n);
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
				tag = Global.random().nextInt(numRelations);
			
			pattern.get(from).connect(pattern.get(to), tag);
			
			i++;
		}
		
		Global.info("Sampled pattern: " + pattern);
		
		for(int ins : series(numInstances.length))
		{
			int numI = numInstances[ins];
			
			Global.info("Starting run for %d instances.", numI);
			System.out.println();
			
    		// * Sample the data
    		topology = RandomGraphs.randomDirectedFast(n, m);
    		
    		List<Triple> triples = new ArrayList<>();
    		
    		for(DLink<?> link : topology.links())
    			triples.add(t(
    					link.from().index(), 
    					Global.random().nextInt(numRelations), 
    					link.to().index()));
    		
    		// - Insert motif instances into the graph
    		for(int j : series(numI))
    		{
    			List<Integer> smp = nl.peterbloem.kit.Functions.sampleInts(motifSize, n);			
    			for(DTLink<Integer, Integer> link : pattern.links())
    			{
    				int s = link.from().label(),
    					p = link.tag(),
    					o = link.to().label();
    				
    				// instantiate any variables with random choices
    				if(s < 0)
    					s = smp.get(-s - 1);
    				if(p < 0)
    					p = Global.random().nextInt(numRelations);
    				if(o < 0)
    					o = smp.get(-o - 1);
    				
    				triples.add(Triple.t(s, p, o));
    			}
    		}
    				
    		KGraph data = new KGraph(triples);
    		
//    		Global.info(String.format("Sampled data. (%d, %d)", data.size(), data.numLinks()) );
    		
    		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
    		
//        		List<List<Integer>> matches = Find.find(pattern, data);    		
//        		matches = MotifCode.prune(pattern, matches);
//        
//        		double patternBits = MotifCode.codelength(KGraph.degrees(data), pattern, matches);

    		SAParallel search = new SAParallel(data, iterations, alpha, maxTime);
    		
    		for(DTGraph<Integer, Integer> motif : search.byScore(topK))
    			observe(motif, ins, nullBits - search.score(motif), search.frequency(motif));

		}
		
		// * Output the results
		
		// - Sort the motifs by score for the 10 instances experiment
		List<DTGraph<Integer, Integer>> motifs = new ArrayList<>(scores.keySet());
		
		Collections.sort(motifs, new Comparator<DTGraph<Integer, Integer>>(){

			@Override
			public int compare(DTGraph<Integer, Integer> m1, DTGraph<Integer, Integer> m2)
			{
				Double score1 = scores.get(m1).get(focus);
				Double score2 = scores.get(m2).get(focus);
				
				if(score1 == null) score1 = Double.NEGATIVE_INFINITY;
				if(score2 == null) score2 = Double.NEGATIVE_INFINITY;
				
				return - Double.compare(score1, score2);
			}
		});
		
		// - Print score and frequency for each experiment (6 columns)
		{
    		BufferedWriter out = new BufferedWriter(new FileWriter("scores.csv"));
    		for(DTGraph<Integer, Integer> motif : motifs.subList(0, toCSV))
    		{
    			boolean first = true;
    			
    			for(int ins : series(numInstances.length))
    			{
    				Double score     = scores.get(motif).get(ins);
    				Integer frequency = frequencies.get(motif).get(ins);
    				
    				if(score == null)     score = Double.NaN;
    				if(frequency == null) frequency = Integer.MIN_VALUE;
    				
    				if(first)
    					first = false;
    				else
    					out.write(", ");
    				
    				out.write(score + ", " + frequency);
    			}
    			
    			out.write("\n");
    		}
    		
    		out.close();
		}
		
		// - Print the dot representation of the graphs
		{
    		BufferedWriter out = new BufferedWriter(new FileWriter("motifs.csv"));
    		for(DTGraph<Integer, Integer> motif : motifs.subList(0, toCSV))
    		{
    			out.write(makeString(motif));
    			out.write("\n");
    		}
    		
    		out.close();
		}
	}

	private void observe(DTGraph<Integer, Integer> motif, int i, double score, int frequency)
	{
		if(! scores.containsKey(motif))
		{
			List<Double> s = new ArrayList<>();
			for(int n : numInstances)
				s.add(null);
			scores.put(motif, s);
			
		}
		if(! frequencies.containsKey(motif))
		{			
			List<Integer> s = new ArrayList<>();
			for(int n : numInstances)
				s.add(null);

			frequencies.put(motif, s);
		}
		
		scores.get(motif).set(i, score);
		frequencies.get(motif).set(i, frequency);	
	}
	
	/**
	 * Returns a representation of the graph in Dot language format. Adds some 
	 * styling information specific to this experiment
	 */
	public static String makeString(DTGraph<Integer, Integer> graph)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("digraph {");
		
		boolean first = true;
		for(DTNode<Integer, Integer> node : graph.nodes())
		{
			if(first) 
				first = false;
			else 
				sb.append("; ");

			sb.append(node.label() + " [color=white]");
		}
		
		for(DTLink<Integer, Integer> link : graph.links())
		{
			if(first) 
				first = false;
			else 
				sb.append("; ");
			
			sb.append(link);
		}
		
		
		sb.append("}");
		
		return sb.toString();
	}
	
}
