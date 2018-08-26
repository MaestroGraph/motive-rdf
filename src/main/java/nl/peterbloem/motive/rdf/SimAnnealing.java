package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static java.util.Collections.reverseOrder;
import static nl.peterbloem.kit.Functions.choose;
import static nl.peterbloem.kit.Functions.min;
import static nl.peterbloem.kit.Functions.subset;
import static nl.peterbloem.kit.Pair.p;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Triple.t;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.Graphs;
import org.nodes.MapDTGraph;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.MaxObserver;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.EdgeListModel.Prior;

/**
 * Implements a simple simulated annealing search for  
 * 
 * 
 * @author Peter
 *
 */
public class SimAnnealing
{
	// maximum time allowed to search for pattern matches
	public static final int MAX_SEARCH_TIME = 10;
	// maximum size of pattern
	public static final int MAX_PATTERN_SIZE = 10;
	
	public static enum  Transition {
		EXTEND, 
		MAKE_NODE_VAR,
		MAKE_LINK_VAR,
		MAKE_NODE_CONST,
		MAKE_LINK_CONST,
		RM_EDGE,
		COUPLE
	};
	
	private double alpha = 0.9;
	
	private Map<DTGraph<Integer, Integer>, Double> scores = new LinkedHashMap<>();
	
	private KGraph graph;
	List<List<Integer>> degrees;

	private DTGraph<Integer, Integer> pattern; 
	private List<List<Integer>> matches;
	public double score;
	
	public SimAnnealing(KGraph graph, double alpha)
	{
		this.graph = graph;
		this.alpha = alpha;
		
		degrees = KGraph.degrees(graph);
	
		// - the starting pattern is a random triple with the object made a variable
		Triple start = Functions.choose(graph.find(null, null, null));
	
		pattern = new MapDTGraph<>();
		DTNode<Integer, Integer> a = pattern.add(start.subject());
		DTNode<Integer, Integer> b = pattern.add(-1);
		a.connect(b, start.predicate());
		
		pattern = Nauty.canonical(pattern, true);
		matches = Find.find(pattern, graph, MAX_SEARCH_TIME);
		
		score = score(pattern, matches);
	}
	
	private double score(DTGraph<Integer, Integer> pattern, List<List<Integer>> matches)
	{
		if(!scores.containsKey(pattern))
		{			
			matches = MotifCode.prune(pattern, matches);
			scores.put(pattern, MotifCode.codelength(degrees, pattern, matches));
		}
		
		return scores.get(pattern);
	}
	
	
	public void iterate()
	{
		DTGraph<Integer, Integer> nwPattern = null;
		while(nwPattern == null)
		{		
			Transition trans = Functions.choose(asList(Transition.values()));
			System.out.println(trans);
			System.out.println("   " + pattern);

			nwPattern = transition(pattern, trans, matches);
			
			System.out.println("   " + nwPattern);
		}
		
		nwPattern = Nauty.canonical(nwPattern, true);
		System.out.println("   " + nwPattern);
		
		List<List<Integer>> nwMatches = Find.find(nwPattern, graph, MAX_SEARCH_TIME);
		if(! Find.TIMED_OUT)
			assert nwMatches.size() > 0;
		
		double nwScore = score(nwPattern, nwMatches);
		
		if(nwScore < score || Global.random().nextDouble() < alpha)
		{
			System.out.println(String.format("step. 	%.3f 	%.3f", score, nwScore));
			
			pattern = nwPattern; 
			score = nwScore;
			
			matches = nwMatches;
		} else
			System.out.println(String.format("stay. 	%.3f 	%.3f", score, nwScore));

	}

	/**
	 * 
	 * @param pattern
	 * @param trans
	 * @return True if the pattern was succesfully changed
	 */
	private DTGraph<Integer, Integer> transition(DTGraph<Integer, Integer> pattern, Transition trans, List<List<Integer>> matches)
	{
		switch (trans) {
		case EXTEND: {
			if(pattern.size() > MAX_PATTERN_SIZE)
				return null;
						
			if(matches.isEmpty())
				return null;
			List<Integer> match = choose(matches);
			
			// - choose a random node in the match, find its node in the graph
			Integer nodeVar = null;
			int node = choose(pattern.nodes()).label();
			if(node < 0)
			{
				nodeVar = node;
				node = match.get(- node - 1);
			}
			
			// - pick a random edge that is not already part of the pattern
			List<Pair<Triple, Boolean>> candidates = new ArrayList<>(graph.get(node).degree());
			for(Triple t : graph.find(node, null, null))
				candidates.add(p(t, true));
			for(Triple t : graph.find(null, null, node))
				candidates.add(p(t, false));
			
			List<Triple> triples = Utils.triples(pattern, match);
			
			for(Triple t : triples)
			{
				candidates.remove(p(t, true));
				candidates.remove(p(t, false));
			}

			if(candidates.isEmpty())
				return null;
			
			Pair<Triple, Boolean> pair = choose(candidates);
			Triple newEdge = pair.first();
			boolean forward = pair.second();
			
			if(nodeVar != null)
			{
				if(forward)
					newEdge = t(nodeVar, newEdge.predicate(), newEdge.object());
				else
					newEdge = t(newEdge.subject(), newEdge.predicate(), nodeVar);
			}
			
			// - add it to the pattern
			DTGraph<Integer, Integer> newPattern = MapDTGraph.copy(pattern);

			int s = newEdge.subject(), p = newEdge.predicate(), o = newEdge.object();
			if(! newPattern.labels().contains(s))
				newPattern.add(s);
			if(! pattern.labels().contains(o))
				newPattern.add(o);
			
			newPattern.node(s).connect(newPattern.node(o), p);
			
			System.out.println(newPattern);
			assert Utils.valid(newPattern);
			
			return newPattern;
			
		} case COUPLE: 
		{
			// - Collect all pairs of predicate variables such that at least one 
			//   match has the same result for both
			Set<Pair<Integer, Integer>> candidates = new LinkedHashSet<>();
			int vlbls = Utils.numVarLabels(pattern); 
			for(List<Integer> match : matches)
			{
				List<Integer> predMatches = match.subList(vlbls, match.size());

				for(int i : series(predMatches.size()))
					for(int j : series(i+1, predMatches.size()))
						if(predMatches.get(i) == predMatches.get(j))
							candidates.add(Pair.p(-i-1 - vlbls , -j-1 - vlbls));
			}
					
			if(candidates.isEmpty())
				return null;
			
			// - Pick a random candidate
		
			Pair<Integer, Integer> cand = choose(candidates);
			int a = cand.first(), b = cand.second();  
						
			assert a < 0 && b < 0;
			
			// - Make a new pattern where a is turned into b, and every variable tag
			//   higher than b is shifted
			DTGraph<Integer, Integer> newPattern = new MapDTGraph<Integer, Integer>();
			
			for(DTNode<Integer, Integer> node : pattern.nodes())
				newPattern.add(node.label());
			
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int tag = link.tag();
				if(tag == a)
					tag = b;
				
				if(tag < a)
					tag ++;
				
				newPattern.get(link.from().index()).connect(
					newPattern.get(link.to().index()), tag);
			}
			
			assert Utils.valid(newPattern);
			
			return newPattern; 
			
		} case MAKE_LINK_CONST:
		{
			// - Collect all predicate variables (make sure there's more than 1)
			Set<Integer> vars = new LinkedHashSet<>();
			for(DTLink<Integer, Integer> link : pattern.links())
				if(link.tag() < 0)
					vars.add(link.tag());
			
			if(vars.size() < 1)
				return null;
			
			// - Pick one
			int a = choose(vars); 
			
			assert a < 0;
			
			// - Choose a random match in the graph
			if(matches.isEmpty())
				return null;
			List<Integer> match = choose(matches);
			int value = match.get(-a - 1);
					
			// - Make a new pattern where a is turned into the correct value, and 
			//   all variables below it are shifted to keep everything contiguous 
			DTGraph<Integer, Integer> newPattern = new MapDTGraph<Integer, Integer>();
			
			for(DTNode<Integer, Integer> node : pattern.nodes())
				newPattern.add(node.label());
			
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int tag = link.tag();
				if(tag == a)
					tag = value;
				
				if(tag < a)
					tag ++;
				
				newPattern.get(link.from().index()).connect(
					newPattern.get(link.to().index()), tag);
			}
			
			assert Utils.valid(newPattern);
			
			return newPattern; 			
			
		} case MAKE_NODE_CONST:
		{
			// - Collect all node variables (make sure there's more than 1)
			Set<Integer> vars = new LinkedHashSet<>();
			for(DTNode<Integer, Integer> node : pattern.nodes())
				if(node.label() < 0)
					vars.add(node.label());
			
			if(vars.size() < 1)
				return null;
			
			// - Pick one
			int a = choose(vars); 
			
			assert a < 0;
			
			// - Choose a random match in the graph
			if(matches.isEmpty())
				return null;
			
			List<Integer> match = choose(matches);
			int value = match.get(- a - 1);
					
			// - Make a new pattern where a is turned into the correct value, and 
			//   all variables below it are shifted to keep everything contiguous 
			DTGraph<Integer, Integer> newPattern = new MapDTGraph<Integer, Integer>();
			
			for(DTNode<Integer, Integer> node : pattern.nodes())
			{
				int lbl = node.label();
				if(lbl == a)
					lbl = value;
				
				if(lbl < a)
					lbl ++;
				
				if(! newPattern.labels().contains(lbl))
					newPattern.add(lbl);
			}
			
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int from = link.from().label();
				int to   = link.to().label();
				int tag  = link.tag();
						
				if(from == a)
					from = value;
				if(to == a)
					to = value;
				
				if(tag < 0)
					tag ++;
				if(from < a)
					from ++;
				if(to < a)
					to ++;
								
				newPattern.node(from).connect(
					newPattern.node(to), tag);
			}
			
			System.out.println(newPattern);
			assert Utils.valid(newPattern);
			
			return newPattern; 			
			
		} case MAKE_NODE_VAR:
		{
			// - Find the next available variable value
			int next = (- Utils.numVarLabels(pattern)) - 1;
			List<Integer> constants = new ArrayList<>();
			
			for(DTNode<Integer, Integer> node : pattern.nodes())
				if(node.label() >= 0)
					constants.add(node.label());
			
			if(constants.isEmpty())
				return null;
			
			// - Choose a random constant node
			int target = choose(constants);
			
			// - Make a new pattern with the node replaced by a variable
			//   Shift all variable predicates to keep things contiguous
			DTGraph<Integer, Integer> newPattern = new MapDTGraph<Integer, Integer>();
			
			for(DTNode<Integer, Integer> node : pattern.nodes())
			{
				int lbl = node.label();
				if(lbl == target)
					lbl  = next;
				newPattern.add(lbl);
			}
			
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int tag = link.tag();
				
				if(tag < 0)
					tag --;
				
				newPattern.get(link.from().index()).connect(
					newPattern.get(link.to().index()), tag);
			}
			
			System.out.println(newPattern);
			assert Utils.valid(newPattern);
			
			return newPattern; 
			
		} case MAKE_LINK_VAR:
		{
			// - Find the next available variable value
			
			int next;
			if(Utils.numVarTags(pattern) == 0)
				next = Math.min(0, min(pattern.labels())) - 1;
			else
				next = min(pattern.tags()) - 1;
			
			Set<Integer> constants = new LinkedHashSet<>();
			for(DTLink<Integer, Integer> link : pattern.links())
				if(link.tag() >= 0)
					constants.add(link.tag());
			
			if(constants.isEmpty())
				return null;
				
			// - Choose a random constant predicate
			int target = choose(constants);
			
			// - Make a new pattern with all occurrences replaced by a variable
			DTGraph<Integer, Integer> newPattern = new MapDTGraph<Integer, Integer>();
			
			for(DTNode<Integer, Integer> node : pattern.nodes())
				newPattern.add(node.label());
			
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int tag = link.tag();
				
				if(tag == target)
					tag = next;
				
				newPattern.get(link.from().index()).connect(
					newPattern.get(link.to().index()), tag);
			}
			
			assert Utils.valid(newPattern);
			
			return newPattern; 
		} case RM_EDGE:
		{
			// - Figure out which edges can be safely removed without creating 
			//   a disconnected pattern
			List<Triple> triples = new ArrayList<>((int)pattern.numLinks());
			for(DTLink<Integer, Integer> link : pattern.links())
				triples.add(Triple.t(link.from().label(), link.tag(), link.to().label()));

			Iterator<Triple> it = triples.iterator();
			while(it.hasNext())
			{
				Triple t = it.next();
				
				if(!canBeRemoved(pattern, t))
					it.remove();
			}
						
			if(triples.isEmpty())
				return null;
			
			// - Pick a random one and create a new pattern with that edge 
			//   removed
			Triple link = choose(triples);
			
			DTGraph<Integer, Integer> newPattern0 = remove(pattern, link);

			// - Recreate the pattern with contiguous variables
			Set<Integer> nodeVarset = new LinkedHashSet<>();
			Set<Integer> linkVarset = new LinkedHashSet<>();

			for(DTLink<Integer, Integer> lnk : newPattern0.links())
			{
				if(lnk.from().label() < 0)
					nodeVarset.add(lnk.from().label());
				if(lnk.tag() < 0)
					linkVarset.add(lnk.tag());
				if(lnk.to().label() < 0)
					nodeVarset.add(lnk.to().label());
			}
			
			List<Integer> nodeVars = new ArrayList<>(nodeVarset);
			List<Integer> linkVars = new ArrayList<>(linkVarset);

			nodeVars.sort(reverseOrder());
			linkVars.sort(reverseOrder());
			
			List<Integer> variables = Functions.concat(nodeVars,  linkVars);
			
			DTGraph<Integer, Integer> newPattern = new MapDTGraph<Integer, Integer>();
			
			for(DTNode<Integer, Integer> node : newPattern0.nodes())
			{
				int lbl = node.label();
				if(lbl < 0)
					lbl = - (variables.indexOf(lbl) + 1);
				newPattern.add(lbl);
			}
			
			for(DTLink<Integer, Integer> lnk : newPattern0.links())
			{
				int from = lnk.from().label(),
				    tag = lnk.tag(),
				    to = lnk.to().label();
				
				if(from < 0)
					from = - (variables.indexOf(from) + 1);
				if(tag < 0)
					tag = - (variables.indexOf(tag) + 1);
				if(to < 0)
					to = - (variables.indexOf(to) + 1);
				
				newPattern.node(from).connect(newPattern.node(to), tag);
			}
			
			assert Utils.valid(newPattern);
			
			return newPattern;
			
		} default: 
		{
			return null;
		}}
	}
	
	/**
	 * 
	 * @param pattern
	 * @param link
	 * @return True if the given link can be removed without creating a disconnected graph.
	 */
	private static boolean canBeRemoved(DTGraph<Integer, Integer> pattern, Triple link)
	{		
		if(pattern.numLinks() < 2)
			return false;
		
		DTGraph<Integer, Integer> newPattern = remove(pattern, link);
		
		return Graphs.connected(newPattern);
	}

	/**
	 * Create a copy of a given graph with a given edge removed. Any nodes that 
	 * have become orphaned (i.e. have degree 0 after the removal) are removed 
	 * as well. 
	 *
	 * @param pattern
	 * @param s
	 * @param p
	 * @param o
	 * @return
	 */
	public static DTGraph<Integer, Integer> remove(DTGraph<Integer, Integer> pattern, Triple link)
	{
		int s = link.subject(), 
			p = link.predicate(), 
			o = link.object();
		
		DTGraph<Integer, Integer> newPattern = new MapDTGraph<Integer, Integer>();
		for(DTNode<Integer, Integer> node : pattern.nodes())
			newPattern.add(node.label());
		
		for(DTLink<Integer, Integer> lnk : pattern.links())
			if(
				lnk.from().label() != s ||
				lnk.tag() != p ||
				lnk.to().label() != o
			)
				newPattern.get(lnk.from().index()).connect(
						newPattern.get(lnk.to().index()), lnk.tag());
		
		// - remove orphaned nodes
		List<DTNode<Integer, Integer>> toRemove = new ArrayList<>();
		
		for(DTNode<Integer, Integer> node : newPattern.nodes())
			if(node.degree() == 0)
				toRemove.add(node);
		
		for(DTNode<Integer, Integer> node : toRemove)
			node.remove();
		
		return newPattern;
	}

}
