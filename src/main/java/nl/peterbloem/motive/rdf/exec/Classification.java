package nl.peterbloem.motive.rdf.exec;

import static java.lang.String.format;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Pref.shorten;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTLink;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.Datasets;
import nl.peterbloem.motive.rdf.EdgeListModel;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;
import nl.peterbloem.motive.rdf.KGraph;
import nl.peterbloem.motive.rdf.MultiParallel;
import nl.peterbloem.motive.rdf.Pref;
import nl.peterbloem.motive.rdf.SAParallel;
import nl.peterbloem.motive.rdf.SimAnnealing;
import nl.peterbloem.motive.rdf.SimAnnealingMulti;
import nl.peterbloem.motive.rdf.Triple;


/**
 * Classification experiment: 
 * 
 * Simplify a graph by retaining only the instances of the top-k motifs. Then 
 * classify it by RGCN (implemented in python).
 * 
 * This part of the code implements the graph simplification. It reads a graph 
 * together with a list of target nodes, and outputs the following graphs.
 * - Simplified graphs for instance of the top 1, 2, ..., k motifs by compression. 
 *   Target nodes are always included.
 * - Simplified graphs for instance of the top 1, 2, ..., k motifs by frequency. 
 * - The complete graph in integer-format.
 * - The 1, 2 and 3 neighborhood graphs of the target nodes (used as a baseline)
 * 
 * @author Peter
 *
 */
public class Classification
{
	
	public String dataname;
	
	public File classificationsFile;
	
	public int iterations;
	
	public double alpha;
	
	public int topK;
	
	public int maxSearchTime;

	public void main()
		throws IOException
	{
		KGraph data;
		List<String> labels = new ArrayList<>(), tags = new ArrayList<>();

		File dataFile = new File(dataname);
		data = KGraph.loadHDT(dataFile, labels, tags);
		
		Global.info("Graph loaded");

		Map<String, String> cls = tsv(classificationsFile);
		
		Global.info("Classifications loaded");

		Global.info("Computing baseline codelength");
		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
		
		Global.info("Done. Searching.");
		MultiParallel search = new MultiParallel(data, iterations, alpha, maxSearchTime);

		Global.info("Search finished.");
		
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("motifs-byscore.latex")));
		for(SimAnnealingMulti.MotifSet result : search.results())
		{
			double factor = nullBits - result.score();
						
			out.write(String.format(" %.1f & \\makecell{", factor));
			
			for(DTGraph<Integer, Integer> motif : result.patterns())
			{
				DTGraph<String, String> m = data.recover(motif, labels, tags);
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
	}
	
	/**
	 * Writes out the subgraph of the given knowledge graph (in ntriples) that 
	 * corresponds to all found instances of the given motifs. 
	 * 
	 * 
	 * @param graph
	 * @param labels
	 * @param tags
	 * @param motifs
	 * @param outFile
	 * @throws IOException
	 */
	public static void writeSubgraph(KGraph graph, List<String> labels, List<String> tags, 
			List<DTGraph<Integer, Integer>> motifs, File outFile)
		throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		
		Set<Triple> seen = new LinkedHashSet<>();
		
		// * Find the instances
			
		
		
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
	
	public static Map<String, String> tsv(File file)
			throws IOException
	{
		Map<String, String> map = new LinkedHashMap<String, String>();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		reader.readLine(); // skip titles
		String line = reader.readLine();
		
		while(line != null)
		{
			String[] split = line.split("\\s");
			map.put(split[1], split[2]);
			
			line = reader.readLine();
		}
		
		return map;
	}
	
}
