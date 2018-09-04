package nl.peterbloem.motive.rdf.exec;

import static java.lang.Math.floorMod;
import static nl.peterbloem.kit.Functions.exp2;
import static nl.peterbloem.kit.Functions.tic;
import static nl.peterbloem.kit.Functions.toc;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.MotifCode.pruneValues;
import static nl.peterbloem.motive.rdf.Utils.numVarLabels;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.nodes.DTGraph;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;

import cern.colt.Arrays;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.Datasets;
import nl.peterbloem.motive.rdf.EdgeListModel;
import nl.peterbloem.motive.rdf.KGraphList;
import nl.peterbloem.motive.rdf.MotifCode;
import nl.peterbloem.motive.rdf.Nauty;
import nl.peterbloem.motive.rdf.Utils;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class Run
{
	@Option(
			name="--data",
			usage="Dataset (dogfood, aifb, mutag)")
	private static String dataset = null;
		
//	@Option(name="--file", usage="Input file: an HDT file.")
//	private static File file;	
	
	@Option(
			name="--experiment",
			usage="Experiment type. Options: synthetic (sample a random graph), synth-rep (repeated experiment on random graphs), ...")
	private static String mode = "synth-rep";
	
	@Option(
			name="--samples",
			usage="Number of samples to take.")
	private static int samples = 5000000;
	
	@Option(
			name="--repeats",
			usage="Number of repeats (synth-rep experiment).")
	private static int repeats = 250;
	
	@Option(
			name="--max-instance",
			usage="Maximum number of instances added to the graph (synth-rep experiment).")
	private static int maxInstances = 25;
	
	@Option(
			name="--max-time",
			usage="Maximum time allowed for a pattern search.")
	private static int maxTime = 25;
	
	@Option(
			name="--min-size",
			usage="Minimum motif size")
	private static int minSize = 3;
	
	@Option(
			name="--max-size",
			usage="Maximum motif size")
	private static int maxSize = 6;
		
	@Option(
			name="--alpha",
			usage="Search parameter alpha")
	private static double alpha = 0.5;
	
	@Option(
			name="--iterations",
			usage="Nr. of search iterations")
	private static int iterations = 10000000;
	
	@Option(
			name="--topk",
			usage="Nr. of motifs to extract.")
	private static int topK = 50000;
	
	@Option(
			name="--to-csv",
			usage="Nr. of motifs to write.")
	private static int toCSV = 50;
	
	@Option(name="--fast-py", usage="Use fast PY model (don't optimize the parameters). Much faster computation of compression factors, less effective compresssion.")
	private static boolean fastPY = false;

	@Option(name="--help", usage="Print usage information.", aliases={"-h"}, help=true)
	private static boolean help = false;
	
	public static void main(String[] args)
		throws IOException
	{
		Global.info("Starting");
		Global.randomSeed();
		
		Run run = new Run();
		
		// * Parse the command-line arguments
    	CmdLineParser parser = new CmdLineParser(run);
    	try
		{
			parser.parseArgument(args);
		} catch (CmdLineException e)
		{
	    	System.err.println(e.getMessage());
	        System.err.println("java -jar motive.jar [options...]");
	        parser.printUsage(System.err);
	        
	        System.exit(1);	
	    }

    	Global.info(Arrays.toString(args));
    	
    	if(help)
    	{
	        parser.printUsage(System.out);
	        
	        System.exit(0);	
    	}
		
    	if(mode.toLowerCase().trim().equals("synthetic"))
    	{
    		Synthetic syn = new Synthetic();
    		syn.alpha = alpha;
    		syn.toCSV = toCSV;
    		syn.topK  = topK;
    		syn.iterations = iterations;
    		syn.maxTime = maxTime;
    		
    		syn.main();
    	} else if(mode.toLowerCase().trim().equals("synth-rep"))
    	{
    		SynthRep sr = new SynthRep();
    		sr.maxInstances  = maxInstances;
    		sr.maxSearchTime = maxTime;
    		sr.motifMaxSize  = maxSize;
    		sr.motifMinSize  = minSize;
    		sr.repeats       = repeats;
    		
    		sr.main();
    		
    	} else if(mode.toLowerCase().trim().equals("real-world"))
    	{
    		RealWorld rw = new RealWorld();
    		rw.dataname = dataset;
    		rw.iterations = iterations;
    		rw.alpha = alpha;
    		rw.topK = topK;
    		rw.maxSearchTime = maxTime;
    		
    		rw.main();
    		
    	} else
    		throw new IllegalArgumentException("Experiment mode %s not recognized.");	
    	
    	Global.info("Finished.");
	}

	
//	public static void multipleMotif(List<String> nodes, List<String> relations, KGraphList data) throws IOException
//	{
//		List<List<Integer>> degrees = KGraphList.degrees(data);
//
//		double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
//		
//		Global.log().info("Size under null model (ML Bound) " +  nullBits);
//		
//		Sampler sampler = new Sampler(data, samples, pruneEvery, minSize, maxSize, maxVars);
//		
//		Global.log().info("Finished sampling, " + sampler.patterns().size() + " patterns found.");
//		
//		List<DTGraph<Integer, Integer>> patterns = new ArrayList<>(sampler.patterns(retain));
//
//		List<Double> factors = new ArrayList<>(patterns.size());
//		
//		for(DTGraph<Integer, Integer> pattern : patterns)
//		{
//			double factor = nullBits - MotifCode.codelength(degrees, pattern, sampler.instances(pattern), fastPY);
//			factors.add(factor);
//			Global.log().info(" compression factor: " + factor);
//		}
//		
//		Global.log().info("Finished computing codelengths.");
//
//		List<List<List<Integer>>> values = new ArrayList<>(patterns.size());
//		for(DTGraph<Integer, Integer> pattern : patterns)
//			values.add(sampler.instances(pattern));
//	
//		Functions.sort(factors, Collections.reverseOrder(), patterns, values);
//
//		// * MH Algorithm
//		
//		// - an ordering of the patterns.
//		List<Integer> sol = new ArrayList<>(series(patterns.size()));
//		
//		//  - limit: the first 'lim' patterns are included
//		//    (the start state has all patterns with a positive comp. factor included)
//		int lim = 0;
//		for(double cl : factors)
//			if(cl > 0.0)
//				lim ++;
//			else
//				break;
//		
//		Global.log().info("Found "+lim+" patterns with a positive compression.");
//		
//		List<DTGraph<Integer, Integer>> patSelected = indexList(patterns, sol.subList(0, lim));
//		List<List<List<Integer>>> valSelected = pruneValues(patSelected, indexList(values, sol.subList(0, lim)));
//
//		double codelength = MotifCode.codelength(degrees, patSelected, valSelected, fastPY);
//		double factor = nullBits - codelength;
//		
//		double bestFactor = factor;
//		List<Integer> bestSol = new ArrayList<>(sol);
//		int bestLim = lim;
//		
//		tic();
//		for(int i : series(ITERATIONS))
//		{
//			// - Transition the solution
//			int limNew = lim;
//			limNew += Global.random().nextInt(3) - 1;
//			limNew = floorMod(limNew, patterns.size() + 1);
//			
//			// - we mod with size + 1, because patterns.size() is the maximum 
//			//   possible value
//			List<Integer> solNew = new ArrayList<Integer>(sol);
//			int swapA = Global.random().nextInt(patterns.size());
//			int swapB = (swapA + 1) % patterns.size();
//			
//			int tmp = solNew.get(swapA);
//			solNew.set(swapA, solNew.get(swapB));
//			solNew.set(swapB, tmp);
//			
//			double ratio, factorNew;
//			
//			if(limNew == lim && swapA >= limNew && swapB >= limNew) 
//			{   // if the swap happens in the ignored part, we know the ratio already
//				ratio = 1.0;
//				factorNew = factor; 	
//			} else 
//			{
//    			patSelected = indexList(patterns, solNew.subList(0, limNew));
//    			valSelected = pruneValues(patSelected, indexList(values, solNew.subList(0, limNew)));
//    			
//    			double codelengthNew =  MotifCode.codelength(degrees, patSelected, valSelected, fastPY);
//
//    			factorNew = nullBits - codelengthNew;
//    			ratio = factorNew / factor;	
//    		}
//			
//			if(i % 100 == 0)
//			{
//				Global.log().info("it " + i + ": ratio " + ratio + ", lim " + lim + ", current " + factor + ", new " + factorNew  + ", best " + bestFactor);
//			}
//			
//			if(Global.random().nextDouble() < ratio) // transition
//			{
//				factor = factorNew;
//				lim = limNew;
//				sol = new ArrayList<>(solNew);
//			}
//
//			// * remember the best solution observed
//			if(factor > bestFactor)
//			{
//				bestFactor = factor;
//				bestLim = lim;
//				bestSol = new ArrayList<>(sol);
//			}
//		}
//		
//		Global.log().info("Search finished");
//
//		patSelected = indexList(patterns, bestSol.subList(0, bestLim));
//		valSelected = pruneValues(patSelected, indexList(values, bestSol.subList(0, bestLim)));
//		
//		FileWriter motifList = new FileWriter(new File("motifs_multi.csv"));
//		
//		for(int i : series(patSelected.size()))
//		{
//			DTGraph<Integer, Integer> pattern = patSelected.get(i);
//			List<List<Integer>> instances = valSelected.get(i);
//			
//			String bgp = KGraphList.bgp(KGraphList.recover(pattern, nodes, relations));
//			motifList.write(i + ", " + instances.size() + ", " + bgp + " \n");
//			
//			FileWriter valueOut = 
//					new FileWriter(new File(String.format("instances_multi.%03d.csv", i)));
//			
//			for(List<Integer> vals : instances)
//			{				
//				int nvl = numVarLabels(pattern);
//				
//				List<Integer> ns = vals.subList(0, nvl);
//				List<Integer> ts = vals.subList(nvl, vals.size());
//				
//				List<String> strs = new ArrayList<>(vals.size());
//				
//				try {
//					strs.addAll(KGraphList.recover(ns, nodes));
//					strs.addAll(KGraphList.recover(ts, relations));
//				} catch(Exception e)
//				{
//					System.out.println("caught " + e.getMessage());
//					
//					System.out.println(vals);
//					System.out.println(pattern);
//					System.out.println(nvl);
//				}
//								
//				for(int j : series(strs.size()))
//				{
//					if(j != 0)
//						valueOut.write(", ");
//					valueOut.write(strs.get(j));
//				}
//				valueOut.write("\n");
//			}
//			valueOut.close();
//			
//			i++;
//		}
//		
//		motifList.close();
//		
//		Global.log().info("Experiment finished.");
//	}
//
//	public static <L> List<L> indexList(final List<L> superList, final List<Integer> indices)
//	{
//		return new AbstractList<L>(){
//
//			@Override
//			public L get(int index)
//			{
//				return superList.get(indices.get(index));
//			}
//
//			@Override
//			public int size()
//			{
//				return indices.size();
//			}
//			
//		};
//	}
}
