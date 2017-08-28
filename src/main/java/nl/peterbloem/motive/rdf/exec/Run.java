package nl.peterbloem.motive.rdf.exec;

import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Utils.numVarLabels;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.nodes.DTGraph;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.Datasets;
import nl.peterbloem.motive.rdf.EdgeListModel;
import nl.peterbloem.motive.rdf.KGraph;
import nl.peterbloem.motive.rdf.MotifCode;
import nl.peterbloem.motive.rdf.Sampler;
import nl.peterbloem.motive.rdf.Utils;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

public class Run
{
	@Option(name="--file", usage="Input file: a graph in edge-list encoding (2 tab separated integers per line). Multiple edges and self-loops are ignored. If type is class, this should be an RDF file.")
	private static File file;	
	
	@Option(
			name="--samples",
			usage="Number of samples to take.")
	private static int samples = 5000000;
	
	@Option(
			name="--prune-every",
			usage="Pruning interval.")
	private static int pruneEvery = 100000;
	
	@Option(
			name="--min-size",
			usage="Minimum motif size")
	private static int minSize = 3;
	
	@Option(
			name="--max-size",
			usage="Minimum motif size")
	private static int maxSize = 6;
	
	@Option(
			name="--retain",
			usage="Nr of motifs retained (by frequency)")
	private static int retain = 100;
	
	@Option(
			name="--max-vars",
			usage="Maximum number of variables in a single motif")
	private static int maxVars = 6;
	
	@Option(name="--fast-py", usage="Use fast PY model (don't optimize the prameters). Much faster computation of compression factors, less effective compresssion.")
	private static boolean fastPY = false;

	@Option(name="--help", usage="Print usage information.", aliases={"-h"}, help=true)
	private static boolean help = false;
	
	public static void main(String[] args)
		throws IOException
	{
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
    	
    	if(help)
    	{
	        parser.printUsage(System.out);
	        
	        System.exit(0);	
    	}
		
		List<String> nodes = new ArrayList<>();
		List<String> relations = new ArrayList<>();
		
		KGraph data = KGraph.loadHDT(file, nodes, relations);
		
		List<List<Integer>> degrees = KGraph.degrees(data);

		double nullBits = EdgeListModel.codelength(degrees, Prior.ML);
		
		Global.log().info("Size under null model (ML Bound) " +  nullBits);
		
		Sampler sampler = new Sampler(data, samples, pruneEvery, minSize, maxSize, maxVars);
		
		Global.log().info("Finished sampling, " + sampler.patterns().size() + " patterns found.");
		
		List<DTGraph<Integer, Integer>> patterns = new ArrayList<>(sampler.patterns(retain));
		List<Double> codelengths = new ArrayList<>(patterns.size());
		for(DTGraph<Integer, Integer> pattern : patterns)
		{
			codelengths.add(MotifCode.codelength(degrees, pattern, sampler.instances(pattern), fastPY));
			Global.log().info(" compression factor: " + (nullBits - codelengths.get(codelengths.size()-1)) );
		}
		
		Functions.sort(patterns, codelengths, Functions.natural());
		
		Global.log().info("Finished computing codelengths.");

		FileWriter motifList = new FileWriter(new File("motifs.csv"));
		
		int i = 0;
		for(DTGraph<Integer, Integer> pattern : patterns)
		{
			Global.log().info("Pattern: " + i + " " + pattern);
			Global.log().info(" code length        : " + codelengths.get(i));
			Global.log().info(" compression factor : " + (nullBits - codelengths.get(i)));
			
			List<List<Integer>> instances = sampler.instances(pattern);
			
			String bgp = KGraph.bgp(KGraph.recover(pattern, nodes, relations));
			motifList.write(i + ", " + instances.size() + ", " + (nullBits - codelengths.get(i)) + ", " + bgp + " \n");
			
			FileWriter values = 
					new FileWriter(new File(String.format("instances.%05d.csv", i)));
			
			for(List<Integer> vals : instances)
			{				
				int nvl = numVarLabels(pattern);
				
				List<Integer> ns = vals.subList(0, nvl);
				List<Integer> ts = vals.subList(nvl, vals.size());
				
				List<String> strs = new ArrayList<>(vals.size());
				strs.addAll(KGraph.recover(ns, nodes));
				strs.addAll(KGraph.recover(ts, relations));
								
				for(int j : series(strs.size()))
				{
					if(j != 0)
						values.write(", ");
					values.write(strs.get(j));
				}
				values.write("\n");
			}
			values.close();
			
			i++;
		}
		
		motifList.close();
	}

}
