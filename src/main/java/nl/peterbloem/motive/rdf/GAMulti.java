package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static java.util.Collections.reverseOrder;
import static nl.peterbloem.kit.Functions.choose;
import static nl.peterbloem.kit.Functions.min;
import static nl.peterbloem.kit.Functions.subset;
import static nl.peterbloem.kit.Pair.p;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.GAMulti.Transition.*;
import static nl.peterbloem.motive.rdf.Triple.t;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
 * Implements a simple genetic algorithm search for motif sets: sets of motifs,
 * which together compress the graph.
 * 
 * 
 * @author Peter
 *
 */
public class GAMulti
{
	// * Number of positive motifsets encountered
	public int numPos = 0;
	public Double nullBits;
	
	public int numPos()
	{
		return numPos;
	}
	
	// maximum time allowed to search for pattern matches
	public int maxSearchTime = 5;
	// maximum size of pattern
	public static final int MAX_PATTERN_SIZE = 10;
	public static final int MAX_NUM_PATTERNS = 25;
	public static final int RESULTS = 100;
	
	private  MaxObserver<MotifSet> observer;

	public static enum  Transition {
		EXTEND, 
		MAKE_NODE_VAR,
		MAKE_LINK_VAR,
		MAKE_NODE_CONST,
		MAKE_LINK_CONST,
		RM_EDGE,
		COUPLE,
		ADD_PATTERN,
		RM_PATTERN
	};
	
	private double alpha = 0.9;
	
	private KGraph graph;
	private List<List<Integer>> degrees;

	private int populationSize;
	private ArrayList<MotifSet> population;
	
	/**
	 * Holds a collection of motifs which together are used to compress the 
	 * graph. The compression is computed on construction. 
	 * 
	 * @author Peter
	 *
	 */
	public class MotifSet implements Comparable<MotifSet>
	{
		private List<DTGraph<Integer, Integer>> motifs = new ArrayList<>();
		private List<List<List<Integer>>> matches = new ArrayList<>();
		
		double score;
		
		public MotifSet(Collection<DTGraph<Integer, Integer>> patterns)
		{
			for(DTGraph<Integer, Integer> pattern : patterns)
			{
				DTGraph<Integer, Integer> cp = Nauty.canonical(pattern, true);
				motifs.add(cp);
				matches.add(Find.find(cp, graph, maxSearchTime));
				
				List<List<List<Integer>>> pruned = MotifCode.pruneValues(motifs, matches);
				score = MotifCode.codelength(degrees, motifs, pruned, false);
				
				if(nullBits != null && (nullBits - score) > 0)
					numPos ++;
			}
		}
		
		public double score()
		{
			return score;
		}
		
		public List<DTGraph<Integer, Integer>> patterns()
		{
			return Collections.unmodifiableList(motifs);
		}
		
		public List<List<List<Integer>>> matches()
		{
			return Collections.unmodifiableList(matches);
		}
		
		public int size()
		{
			return motifs.size();
		}

		@Override
		public int compareTo(MotifSet o)
		{
			return Double.compare(score(), o.score());
		}
	}
	
	public GAMulti(KGraph graph, int maxSearchTime, 
			Double nullBits,
			int populationSize, MaxObserver<MotifSet> observer)
	{
		this.populationSize = populationSize;
		this.graph = graph;
		this.maxSearchTime = maxSearchTime;
		this.nullBits = nullBits;
		
		degrees = KGraph.degrees(graph);
	
		this.population = new ArrayList<MotifSet>(populationSize * 2); 
		
		for(int i : series(populationSize))
			this.population.add(new MotifSet(
					Arrays.asList(randomPattern())
				));
		
		if(observer == null)
			this.observer = new MaxObserver<>(RESULTS, Collections.reverseOrder()); // retain the lowest scores
		else 
			this.observer = observer;
	
		for(MotifSet set : this.population)
			observer.observe(set);
	}
	
	public MotifSet crossover(MotifSet mother, MotifSet father)
	{
		List<DTGraph<Integer, Integer>>
			mPatterns = mother.patterns(),
			fPatterns = father.patterns();	
		
		Set<DTGraph<Integer, Integer>> allSet = new LinkedHashSet<>();
		allSet.addAll(mPatterns);
		allSet.addAll(fPatterns);
		
		List<DTGraph<Integer, Integer>> all = new ArrayList<>(allSet);
		
		Collections.shuffle(all, Global.random());
		
		// * The size of the child is uniform between the size of the smallest 
		//   parent - 1 and the size of thelargets parent + 1, and always at least 1
		int min = Math.min(mother.size(), father.size());
		int max = Math.max(mother.size(), father.size());
		
		min--; max++;
		
		int childSize = Global.random().nextInt(max - min) + min;
		childSize = Math.max(1, childSize);
		
		MotifSet child = new MotifSet(new ArrayList<>(all.subList(0, childSize)));
		
		// * Apply a number of random transitions
		for(int t : series(Global.random().nextInt(child.size())))
			child = transition(child);
			
		return child;
	}

	/**
	 * Generate a random pattern consisting of a single triple
	 * @return
	 */
	private DTGraph<Integer, Integer> randomPattern()
	{
		Triple start = Functions.choose(graph.find(null, null, null));
		DTGraph<Integer, Integer> pattern = new MapDTGraph<>();
		DTNode<Integer, Integer> a = pattern.add(start.subject());
		DTNode<Integer, Integer> b = pattern.add(-1);
		a.connect(b, start.predicate());
		
		return pattern;
	}
	
	/**
	 * Perform one step of the GA algorithm.
	 * 
	 */
	public void iterate()
	{
		// * extend the population
		for(int i : series(populationSize))
		{
			MotifSet child = crossover(choose(population), choose(population));
		
			population.add(child);
			observer.observe(child);
		}
		
		// * sort by score
		Collections.sort(population);
		
		// * cull the worst half
		population = new ArrayList<>(population.subList(0, populationSize));
		
	}

	/**
	 * Apply a transition
	 * 
	 * @param pattern
	 * @param trans
	 * @return The modified pattern
	 */
	public MotifSet transition(MotifSet current)
	{
		Transition trans = Functions.choose(asList(Transition.values()));
		return transition(current, trans);
	}
	
	public MotifSet transition(MotifSet current, Transition trans)
	{
		
		List<DTGraph<Integer, Integer>> pts = new ArrayList<>(current.patterns());

		if (trans == ADD_PATTERN) 
		{				
			if(current.size() >= MAX_NUM_PATTERNS)
				return current;
			
			pts.add(randomPattern());
			
			return new MotifSet(pts);
		}
		
		if (trans == RM_PATTERN) 
		{	
			if(current.size() <= 1)
				return current;
			
			int index = Global.random().nextInt(current.size());
			pts.remove(index);
			
			return new MotifSet(pts);
		}
		
		int index = Global.random().nextInt(pts.size());
		
		DTGraph<Integer, Integer> pattern = pts.get(index), newPattern = null;
		
		List<List<Integer>> matches = current.matches().get(index);
		
		if (trans == EXTEND)
		{
			if(pattern.size() > MAX_PATTERN_SIZE)
				return current;
						
			if(matches.isEmpty())
				return current;
			
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
				return current;
			
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
			newPattern = MapDTGraph.copy(pattern);

			int s = newEdge.subject(), p = newEdge.predicate(), o = newEdge.object();
			if(! newPattern.labels().contains(s))
				newPattern.add(s);
			if(! pattern.labels().contains(o))
				newPattern.add(o);
			
			newPattern.node(s).connect(newPattern.node(o), p);
			
			assert Utils.valid(newPattern);			
		}
	
		if (trans == COUPLE) 
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
				return current;
			
			// - Pick a random candidate
		
			Pair<Integer, Integer> cand = choose(candidates);
			int a = cand.first(), b = cand.second();  
						
			assert a < 0 && b < 0;
			
			// - Make a new pattern where a is turned into b, and every variable tag
			//   higher than b is shifted
			newPattern = new MapDTGraph<Integer, Integer>();
			
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
		} 
		
		if(trans == MAKE_LINK_CONST)
		{
			// - Collect all predicate variables (make sure there's more than 1)
			Set<Integer> vars = new LinkedHashSet<>();
			for(DTLink<Integer, Integer> link : pattern.links())
				if(link.tag() < 0)
					vars.add(link.tag());
			
			if(vars.size() < 1)
				return current;
			
			// - Pick one
			int a = choose(vars); 
			
			assert a < 0;
			
			// - Choose a random match in the graph
			if(matches.isEmpty())
				return current;
			List<Integer> match = choose(matches);
			int value = match.get(-a - 1);
					
			// - Make a new pattern where a is turned into the correct value, and 
			//   all variables below it are shifted to keep everything contiguous 
			newPattern = new MapDTGraph<Integer, Integer>();
			
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
		} 
		
		if(trans == MAKE_NODE_CONST)
		{
			// - Collect all node variables (make sure there's more than 1)
			Set<Integer> vars = new LinkedHashSet<>();
			for(DTNode<Integer, Integer> node : pattern.nodes())
				if(node.label() < 0)
					vars.add(node.label());
			
			if(vars.size() < 1)
				return current;
			
			// - Pick one
			int a = choose(vars); 
			
			assert a < 0;
			
			// - Choose a random match in the graph
			if(matches.isEmpty())
				return current;
			
			List<Integer> match = choose(matches);
			int value = match.get(- a - 1);
					
			// - Make a new pattern where a is turned into the correct value, and 
			//   all variables below it are shifted to keep everything contiguous 
			newPattern = new MapDTGraph<Integer, Integer>();
			
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
			
			assert Utils.valid(newPattern);			
		} 
		
		if (trans == MAKE_NODE_VAR)
		{
			// - Find the next available variable value
			int next = (- Utils.numVarLabels(pattern)) - 1;
			List<Integer> constants = new ArrayList<>();
			
			for(DTNode<Integer, Integer> node : pattern.nodes())
				if(node.label() >= 0)
					constants.add(node.label());
			
			if(constants.isEmpty())
				return current;
			
			// - Choose a random constant node
			int target = choose(constants);
			
			// - Make a new pattern with the node replaced by a variable
			//   Shift all variable predicates to keep things contiguous
			newPattern = new MapDTGraph<Integer, Integer>();
			
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
			
			assert Utils.valid(newPattern);			
		} 
		
		if(trans == MAKE_LINK_VAR)
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
				return current;
				
			// - Choose a random constant predicate
			int target = choose(constants);
			
			// - Make a new pattern with all occurrences replaced by a variable
			newPattern = new MapDTGraph<Integer, Integer>();
			
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
		}
		
		if (trans == RM_EDGE)
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
				return current;
			
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
			
			newPattern = new MapDTGraph<Integer, Integer>();
			
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
		} 
		
		if(newPattern == null)
			return current;
		
		pts.set(index, newPattern);
		return new MotifSet(pts);
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
	
	public Collection<MotifSet> results()
	{
		return observer.elements();
	}
	
	
}
