package nl.peterbloem.motive.rdf;

import java.util.LinkedHashMap;
import java.util.Map;

public class Pref
{

	private static final Map<String, String> prefixes = new LinkedHashMap<>();
	private static final Map<String, String> inverse = new LinkedHashMap<>();
	
	static {
		prefixes.put("swrs", "http://swrc.ontoware.org/ontology#");
		prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
		prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
		prefixes.put("wn", "http://wordnet-rdf.princeton.edu/ontology#");
		prefixes.put("lm", "http://lemon-model.net/");
		prefixes.put("gv", "http://geovocab.org/geometry#");
		prefixes.put("og", "http://www.opengis.net/rdf#");
		prefixes.put("lgd", "http://linkedgeodata.org/ontology/");
		prefixes.put("swc", "http://data.semanticweb.org/ns/swc/ontology#");
		prefixes.put("dsw", "http://data.semanticweb.org/");
		prefixes.put("vu", "http://www.cs.vu.nl/");
		prefixes.put("skos", "http://www.w3.org/2004/02/skos/core#");
		prefixes.put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
		prefixes.put("dct", "http://purl.org/dc/terms/");
		prefixes.put("ical", "http://www.w3.org/2002/12/cal/ical");
		prefixes.put("bibo", "http://purl.org/ontology/bibo/");
		prefixes.put("aifb", "http://www.aifb.uni-karlsruhe.de");
		prefixes.put("xmls", "http://www.w3.org/2001/XMLSchema/");
		prefixes.put("mtg", "http://dl-learner.org/carcinogenesis");
//		prefixes.put("lgd", "http://linkedgeodata.org/ontology/");
//		prefixes.put("lgd", "http://linkedgeodata.org/ontology/");
//		prefixes.put("lgd", "http://linkedgeodata.org/ontology/");
//		prefixes.put("lgd", "http://linkedgeodata.org/ontology/");
//		prefixes.put("lgd", "http://linkedgeodata.org/ontology/");

		for(String key : prefixes.keySet())
			inverse.put(prefixes.get(key), key);
	}

	public static String prefix(String iri)
	{
		return prefixes.get(iri);
	}
	
	public static String iri(String prefix)
	{
		return inverse.get(prefix);
	}
	
	public static String expand(String shortened)
	{
		if(! shortened.contains(":"))
			return shortened;
		
		String[] sp = shortened.split(":", 2);
		
		return iri(sp[0] + sp[1]);
	}
	
	public static String shorten(String full)
	{
		for(String key : prefixes.keySet())
			if(full.startsWith(prefixes.get(key)))
				return key + ":" + full.substring(prefixes.get(key).length()); 
			
		return full;
	}
}
