package nl.peterbloem.motive.rdf.exec;

import static java.lang.String.format;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Pref.shorten;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.nodes.DTGraph;
import org.nodes.DTLink;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.Datasets;
import nl.peterbloem.motive.rdf.EdgeListModel;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;
import nl.peterbloem.motive.rdf.KGraph;
import nl.peterbloem.motive.rdf.Pref;
import nl.peterbloem.motive.rdf.SAParallel;
import nl.peterbloem.motive.rdf.SimAnnealing;

public class Queries
{
	public static Pattern extractBGP = Pattern.compile(".*WHERE([^\\}]*).*"); //  \\{(.*)\\}");
	
	public String dataname;
	public File logfile;
	
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
		
	
		BufferedReader logIn = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(new FileInputStream(logfile))));
		
		String line = "";
		int i = 0;
		while(line != null)
		{
			line = logIn.readLine();
			i++;
			if(i % 100000 == 0)
				Global.info(i + " lines read from log file.");

			if(! line.contains("sparql?query="))
				continue;
			
			if(! line.contains("WHERE"))
				continue;
			
			Matcher m = extractBGP.matcher(line);
			
			if(m.matches())
			{
    			String bgp = m.group(1);
    			bgp = URLDecoder.decode(bgp, "UTF-8");
    			
    			System.out.println("match " + bgp);
			} else 
			{
    			line = URLDecoder.decode(line, "UTF-8");

				System.out.println("no match " + line);
			}
					
		}
	}
}
