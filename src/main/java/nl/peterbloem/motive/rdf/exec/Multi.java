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
import java.util.List;

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

public class Multi
{
	
	public String dataname;
	
	public int iterations;
	
	public double alpha;
	
	public int topK;
	
	public int maxSearchTime;

	public void main()
		throws IOException
	{
		dataname = dataname.trim().toLowerCase();
		
		KGraph data;
		List<String> labels = new ArrayList<>(), tags = new ArrayList<>();
		if("dogfood".equals(dataname))
			data = Datasets.dogfood(labels, tags);
		else if ("aifb".equals(dataname))
			data = Datasets.aifb(labels, tags);
		else if("mutag".equals(dataname))
			data = Datasets.mutag(labels, tags);
		else
			throw new IllegalArgumentException(format("Dataset name %s not recognized"));
		
		double nullBits = EdgeListModel.codelength(KGraph.degrees(data), Prior.ML);
		
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
}
