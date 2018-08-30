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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DTGraph;
import org.nodes.DTLink;
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
import nl.peterbloem.motive.rdf.SimAnnealing;
import nl.peterbloem.motive.rdf.Triple;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

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
	public int n = 5000;
	/**
	 * Size of the random graph in links
	 */
	public int m = 10000;
	
	public int numRelations = 100;
	
	public int motifSize = 3;
	public int motifLinks = 3;
	
	public int motifVNodes = 2;
	public int motifVLinks = 0;
	
	/**
	 *  The number of instances to inject (multiple values)
	 */
	// public List<Integer> numsInstances = Arrays.asList(0, 10, 100);
	public int numInstances = 100;
	
	public double alpha = 0.7;
			
	public int iterations = 5000;
//	/**
//	 *  Number of times to repeat each epxperiment
//	 */
//	public int runs = 10;
	
	public void main() throws IOException
	{		
		// * Sample the motif
		DGraph<?> topology = RandomGraphs.randomDirectedFast(motifSize, motifLinks);
		List<Integer> vNodes = Functions.sampleInts(motifVNodes, motifSize);
		List<Integer> vLinks = Functions.sampleInts(motifVLinks, motifSize);
		int nextVNode = -1, nextVLink = -motifVNodes -1;
		
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
		
		// * Sample the data
		topology = RandomGraphs.randomDirectedFast(n, m);
		
		List<Triple> triples = new ArrayList<>();
		
		for(DLink<?> link : topology.links())
			triples.add(t(
					link.from().index(), 
					Global.random().nextInt(numRelations), 
					link.to().index()));
		
		for(int j : series(numInstances))
		{
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int s = link.from().label(),
					p = link.tag(),
					o = link.to().label();
				
				// instantiate any variables with random choices
				if(s < 0)
					s = Global.random().nextInt(n);
				if(p < 0)
					p = Global.random().nextInt(numRelations);
				if(o < 0)
					o = Global.random().nextInt(n);
				
				triples.add(Triple.t(s, p, o));
			}
		}
				
		KGraph data = new KGraph(triples);
		
		Global.info(String.format("Sampled data. (%d, %d)", data.size(), data.numLinks()) );
		
		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
		Global.info("Codelength under null model " + nullBits);
		
		List<List<Integer>> matches = Find.find(pattern, data);
		Global.info("Pattern matches: " + matches.size());
		double patternBits = MotifCode.codelength(KGraph.degrees(data), pattern, matches);
		Global.info("logfactor with target pattern " + (nullBits - patternBits));
		
		SimAnnealing search = new SimAnnealing(data, alpha);
		
		for (int iteration : series(iterations))
		{
			Functions.dot(iteration, iterations);
			search.iterate();
		}
		
		System.out.println("TOP 30 motifs by score");
		for(DTGraph<Integer, Integer> motif : search.byScore(30))
		{
			System.out.println(String.format("score: %.3f bits ", nullBits - search.score(motif) ));
			System.out.println("motif: " + motif);
		}
		
		System.out.println("TOP 30 motifs by frequency");
		for(DTGraph<Integer, Integer> motif : search.byFrequency(30))
		{
			System.out.println(String.format("score: %6d bits ", search.frequency(motif)));
			System.out.println("motif: " + motif);
		}
			
	}
}
