package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Functions.log2Factorial;
import static nl.peterbloem.kit.Functions.prefix;

import java.util.List;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.OnlineModel;
import nl.peterbloem.kit.PitmanYorModel;

public class EdgeListModel
{
	public static enum Prior {NONE, ML, COMPLETE, COMPLETE_FAST}; 

	
	public static double codelength(KGraph data, Prior prior)
	{
		return codelength(KGraph.degrees(data), prior);
	}
		
	public static double codelength(List<List<Integer>> degrees, Prior priorType)
	{	
		double prior = prior(degrees, priorType);
		
		// number of links in the graph
		
		long m = 0;
		for(int degree : degrees.get(0))
			m += degree;
		
		double bits = 0.0;
		bits += 2.0 * Functions.log2Factorial(m);
		
		for(int inDegree : degrees.get(0))
			bits -= log2Factorial(inDegree);
		for(int outDegree : degrees.get(1))
			bits -= log2Factorial(outDegree);
		for(int relDegree : degrees.get(2))
			bits -= log2Factorial(relDegree);

		return bits + prior;
	}
	
	public static double prior(List<List<Integer>> degrees, Prior prior)
	{
		if(prior == Prior.NONE)
			return 0.0;
		
		if(prior == Prior.ML)
			return OnlineModel.storeSequenceML(degrees.get(0)) + 
			       OnlineModel.storeSequenceML(degrees.get(1)) +
			       OnlineModel.storeSequenceML(degrees.get(2));
		
		if(prior == Prior.COMPLETE)
		{
			// int maxIn = Functions.max(degrees.get(0));
			// int maxOut = Functions.max(degrees.get(1));
			// int maxTag = Functions.max(degrees.get(2));
			
//			return prefix(degrees.get(0).size()) + prefix(degrees.get(2).size()) + 
//				   prefix(maxIn) + prefix(maxOut) + prefix(maxTag) +
//			       OnlineModel.storeIntegers(degrees.get(0)) + 
//			       OnlineModel.storeIntegers(degrees.get(1)) + 
//			       OnlineModel.storeIntegers(degrees.get(2));
			
			return prefix(degrees.get(0).size()) + prefix(degrees.get(2).size()) + 
				       PitmanYorModel.storeIntegersOpt(degrees.get(0)) + 
				       PitmanYorModel.storeIntegersOpt(degrees.get(1)) + 
				       PitmanYorModel.storeIntegersOpt(degrees.get(2));
		}
		
		if(prior == Prior.COMPLETE_FAST)
		{
			return prefix(degrees.get(0).size()) + prefix(degrees.get(2).size()) + 
				       PitmanYorModel.storeIntegers(degrees.get(0)) + 
				       PitmanYorModel.storeIntegers(degrees.get(1)) + 
				       PitmanYorModel.storeIntegers(degrees.get(2));
		}
		
		throw new IllegalStateException("Prior mode unkown: " + prior);
	}	
	
}
