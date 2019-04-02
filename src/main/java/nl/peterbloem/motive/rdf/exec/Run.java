package nl.peterbloem.motive.rdf.exec;

import static java.lang.Math.floorMod;
import static java.lang.String.format;
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
			usage="Dataset (dogfood, aifb, mutag, or HDT file)")
	private static String dataset = null;
		
	@Option(
			name="--logfile",
			usage="Query log (for the queries experiment).")
	private static File logfile = null;
	
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
			name="--pop-size",
			usage="Population size (multi experiment).")
	private static int populationSize = 500;
	
	@Option(
			name="--repeats",
			usage="Number of repeats (synth-rep experiment).")
	private static int repeats = 25;
	
	@Option(
			name="--num-threads",
			usage="Number of threads running concurrently (synth-rep experiment).")
	private static int numThreads = 32;
	
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
		throws IOException, InterruptedException
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
    		sr.numThreads    = numThreads;
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
    	} else if(mode.toLowerCase().trim().equals("multi"))
    	{
    		Multi mlt = new Multi();
    		mlt.dataname = dataset;
    		mlt.iterations = iterations;
    		mlt.populationSize = populationSize;
    		mlt.topK = topK;
    		mlt.maxSearchTime = maxTime;
    		mlt.numThreads = Runtime.getRuntime().availableProcessors();
    		
    		mlt.main();
    	} else if(mode.toLowerCase().trim().equals("queries"))
        {
    		Queries qs = new Queries();
    		qs.dataname = dataset;
    		qs.logfile = logfile;
    		
    		qs.main();
        	
    	} else
    		throw new IllegalArgumentException(format("Experiment mode !! %s not recognized.", mode));	
    	
    	Global.info("Finished.");
	}

}
