package nl.peterbloem.motive.rdf;

import org.nodes.DTGraph;

public class Utils
{

	public Utils()
	{
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param pattern
	 * @return
	 */
	public static int numVarLabels(DTGraph<Integer, Integer> pattern)
	{
		int numVarLabels = 0;
		for(int label : pattern.labels())
			if(label < 0)
				numVarLabels ++;
		return numVarLabels;
	}

}
