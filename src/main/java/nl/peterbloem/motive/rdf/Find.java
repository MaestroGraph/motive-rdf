package nl.peterbloem.motive.rdf;

import static java.util.Arrays.asList;
import static java.util.Collections.reverseOrder;
import static java.util.Collections.unmodifiableSet;
import static nl.peterbloem.kit.Pair.p;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Triple.Part;
import static nl.peterbloem.motive.rdf.Triple.Part.OBJECT;
import static nl.peterbloem.motive.rdf.Triple.Part.PREDICATE;
import static nl.peterbloem.motive.rdf.Triple.Part.SUBJECT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DTGraph;
import org.nodes.DTLink;

import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;
import nl.peterbloem.motive.rdf.KGraph.KNode;

public abstract class Find
{	
	private Find() {}

	/**
	 * Finds all matches for the given BGP pattern. 
	 * @param pattern
	 * @return
	 */
	public static List<List<Integer>> find(DTGraph<Integer, Integer> pattern, KGraph graph)
	{				
		Candidates candidates = new Candidates(pattern, graph);
				
		candidates.prune();
		
		List<List<Integer>> matches = new ArrayList<>();
		findInner(candidates, 0, matches, null);
				
		return matches;
	}

	/**
	 * 
	 * @param pattern
	 * @param graph
	 * @param maxTime Maximum time in seconds. If time runs out, the matches found so far are returned.
	 * @return
	 */
	public static List<List<Integer>> find(DTGraph<Integer, Integer> pattern, KGraph graph, int maxTime)
	{				
		TIMED_OUT = true;
		
		Candidates candidates = new Candidates(pattern, graph);
		
		List<List<Integer>> matches = new ArrayList<>();
		findInner(candidates, 0, matches, System.nanoTime() + (long)1e9 * (long)maxTime);
				
		return matches;
	}
	
	// Whether the last search times out
	public static boolean TIMED_OUT = false;
	
	private static void findInner(Candidates candidates, int depth, List<List<Integer>> matches, Long stopTime)
	{	
//		if(System.nanoTime() > stopTime)
//			return;
		
		assert depth <= candidates.variables().size();
				
		if(candidates.isFailed())
			return;
		
		if(candidates.isFinished())
		{
			if(candidates.isMatch())
				matches.add(candidates.match());
			
			return;
		}
		
//		boolean print = false;
//		
//		if(candidates.candidates(-4).size() == 1 & candidates.candidates(-4).iterator().next() == 0)
//			if(candidates.candidates(-5).size() == 1 & candidates.candidates(-5).iterator().next() == 0)
//			{
//				System.out.println("__" +candidates.candidates);
//				print = true;
//			}
		
		int variable = candidates.variablesRemaining().get(0);
		// - this function sorts the remaining variables on the fly, by number of candidates 
		
		for(int c : candidates.candidates(variable))
			if(! alreadyClaimed(c, variable, candidates))
			{	
    			Candidates nwCandidates = candidates.copy();

    			nwCandidates.setSingleton(variable, c);
    			
    			// * prune the candidate set
    			//   (This may lead to a failure, which will be detected just after recursion)
    			nwCandidates.prune();
    			
    			findInner(nwCandidates, depth+1, matches, stopTime);
    			if(stopTime != null && System.nanoTime() > stopTime)
    			{
    				TIMED_OUT = true;
    				return;
    			}
			}
	}
	
	/**
	 * Check if the assignment of candidate c to variable at depth is a duplicate
	 * 
	 * Note that one relation can be claimed by multiple relation variables. We 
	 * only enforce that two variable _nodes_ aren't claiming the same value.
	 * 
	 * @param c
	 * @param depth
	 * @param candidates
	 * @return
	 */
	private static boolean alreadyClaimed(int c, int var, Candidates candidates)
	{				
		if(candidates.isRelation(var))
			return false;
		
		for(int otherVar : candidates.variables())
			if(otherVar != var && ! candidates.isRelation(otherVar))
				if(candidates.candidates(otherVar).size() == 1)
					if(candidates.candidates(otherVar).contains(c))
						return true;
			
		
		return false;
	}

	/**
	 * This class maintains a list of candidate nodes/predicates for each 
	 * variable in the pattern.  
	 *  
	 * @author Peter
	 *
	 */
	private static class Candidates 
	{
		private Map<Integer, Boolean> isRelation = new LinkedHashMap<>();		
		private Map<Integer, Set<Integer>> candidates = new LinkedHashMap<>();
		private List<Integer> keys;
		
		private DTGraph<Integer, Integer> pattern;
		private KGraph graph; 
		
		private Candidates()
		{
		}
		
		public Candidates(DTGraph<Integer, Integer> pattern, KGraph graph)
		{
			this.pattern = pattern;
			this.graph = graph;
			
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int s = link.from().label(), p = link.tag(), o = link.to().label();
								
				if (s < 0 && ! candidates.containsKey(s))
				{
					candidates.put(s, all(true));
					isRelation.put(s, false);
				}
				
				if (p < 0 && ! candidates.containsKey(p))
				{
					candidates.put(p, all(false));
					isRelation.put(p, true);
				}
				
				if (o < 0 && ! candidates.containsKey(o))
				{
					candidates.put(o, all(true));
					isRelation.put(o, false);
				}
				
				List<Set<Integer>> sets = toSets(graph.find(
						s < 0 ? null : s, 
						p < 0 ? null : p, 
						o < 0 ? null : o));
				
				if (s < 0)
					candidates.put(s, intersect(candidates.get(s), sets.get(0)));
				
				if (p < 0)
					candidates.put(p, intersect(candidates.get(p), sets.get(1)));
				
				if (o < 0)
					candidates.put(o, intersect(candidates.get(o), sets.get(2)));

			}
			
			assert candidates.keySet().equals(isRelation.keySet());
						
			init();
		}
		
		private void init()
		{
			keys = new ArrayList<>(candidates.keySet());
			
			assert pattern != null;
			assert graph != null;
		}
		
		/** 
		 * Deep copy
		 * @return
		 */
		public Candidates copy()
		{
			Candidates result = new Candidates();
			
			for(Map.Entry<Integer, Set<Integer>> entry : this.candidates.entrySet())
				result.candidates.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
			
			result.isRelation = new LinkedHashMap<>(this.isRelation);
			
			result.graph = graph;
			result.pattern = pattern;
			
			result.init();
			return result;
		}
		
		public Set<Integer> candidates(int variable)
		{
			return unmodifiableSet(candidates.get(variable)); 
		}
		
		public boolean isRelation(int variable)
		{
			return isRelation.get(variable); 
		}
		
		public void setSingleton(int variable, int candidate)
		{
			assert(candidates.containsKey(variable));
			
			Set<Integer> nw = new LinkedHashSet<>();
			nw.add(candidate);
			candidates.put(variable, nw);
			
			if(! isRelation(variable))
				for(int other : variables())
					if(other != variable && ! isRelation(other))
						candidates.get(other).remove(candidate);
		}
		
		/**
		 * True if each variable contains one candidate, and the result matches 
		 * the graph.
		 * 
		 * @return
		 */
		public boolean isMatch()
		{
			if(isFailed())
				return false;
			
			if(! isSingleton())
				return false;
			
			// * Check whether all links in completed pattern exist and each maps 
			//   to a unique triple 
			Set<Triple> triples = new LinkedHashSet<>();
			for(DTLink<Integer, Integer> link : pattern.links())
			{
				int s = link.from().label(), p = link.tag(), o = link.to().label();
				
				if (s < 0)
					s = candidates.get(s).iterator().next();
				if (p < 0)
					p = candidates.get(p).iterator().next();
				if (o < 0)
					o = candidates.get(o).iterator().next();
				
				Set<Triple> res = graph.find(s, p, o);
				if(res.isEmpty())
				{
					return false;
				} else
				{
					assert res.size() == 1;
					Triple triple = res.iterator().next();
					if(triples.contains(triple))
					{
						return false; // triple already matched (we can probably
						              // speed things up by eliminating these
						              // earlier)
					}
					
					triples.add(triple);
				}
					
			}
			
			return true;
		}

		/**
		 * True if all variables have only one candidate (so this might be a match)
		 */
		public boolean isSingleton()
		{
			for(int key : candidates.keySet())
				if(candidates.get(key).size() != 1)
					return false;
			
			return true;
		}
		
		/**
		 * True if all variables have 1 or no candidates.
		 * 
		 * @return
		 */
		public boolean isFinished()
		{
			for(int key : candidates.keySet())
				if(candidates.get(key).size() > 1)
					return false;
			
			return true;
		}
		
		/**
		 * True if one or more variables have no candidates 
		 * 
		 * @return
		 */
		public boolean isFailed()
		{
			for(int key : candidates.keySet())
			{
				if(candidates.get(key).isEmpty())
					return true;
				
				// * check if this is a singleton candidate whose value is also 
				//   claimed by another  
//				if(candidates.get(key).size() == 1 && ! isRelation(key))
//					for(int other : candidates.keySet())
//						if(other != key && candidates.get(other).size() == 1 && ! isRelation(other))
//							if(candidates.get(other).equals(candidates.get(key)))
//								return false;
			}
			
			return false;
		}

		public List<Integer> variables()
		{
			return Collections.unmodifiableList(keys);
		}
		
		/**
		 * Provides a list of the variables (those with more than 1 candidate), 
		 * sorted by number of candidates
		 *   
		 * @return
		 */
		public List<Integer> variablesRemaining()
		{
			
			List<Integer> res = new ArrayList<>(keys.size());
			for(int key : keys)
				if(candidates.get(key).size() > 1)
					res.add(key);
			
			res.sort((Integer k1, Integer k2) -> 
				Integer.compare(candidates.get(k1).size(), candidates.get(k2).size()) );

			return res;
		}


		/**
		 * Return the match of this candidate set.
		 * @return
		 * @throws RuntimeException If this candidate set does not represent a match.
		 */
		public List<Integer> match()
		{
			assert isMatch();
			
			List<Integer> keys = new ArrayList<>(candidates.keySet());
			
			Collections.sort(keys, reverseOrder());
			List<Integer> result = new ArrayList<>(keys.size());	
			
			for(int key : keys)
			{
				Set<Integer> cands = candidates.get(key);
				
				if(cands.size() != 1)
					throw new RuntimeException("Number of candidates for variable " + key + " is not 1, but " + cands.size()+".");
				
				result.add(cands.iterator().next());
			}
			
			return result;
		}
		
		private Set<Integer> all(boolean nodes)
		{
			Set<Integer> result = new LinkedHashSet<>();
			
			if(nodes)
    			for(KNode node : graph.nodes())
    				result.add(node.label());
			else
				for(int tag : graph.tags())
    				result.add(tag);
			
			return result;
		}
		
		public void prune()
		{
			boolean changed = true;
			while (changed)
			{
				changed = false;
				
				for(DTLink<Integer, Integer> link : pattern.links())
				{
					int s = link.from().label(), p = link.tag(), o = link.to().label();

					if(s < 0 && p < 0 && o < 0)
					{
						changed |= prune3Var(s, p, o, SUBJECT);
						changed |= prune3Var(s, p, o, PREDICATE);
						changed |= prune3Var(s, p, o, OBJECT);
						
					} else if (s < 0 && p < 0)
					{
						changed |= prune2Var(s, p, o, Part.SUBJECT, Part.PREDICATE);
						changed |= prune2Var(s, p, o, Part.PREDICATE, Part.SUBJECT);
						
					} else if (s < 0 && o < 0)
					{
						changed |= prune2Var(s, p, o, Part.SUBJECT, Part.OBJECT);
						changed |= prune2Var(s, p, o, Part.OBJECT, Part.SUBJECT);	
						
					} else if (p < 0 && o < 0)
					{	
						changed |= prune2Var(s, p, o, Part.PREDICATE, Part.OBJECT);
						changed |= prune2Var(s, p, o, Part.OBJECT, Part.PREDICATE);
						
					} else if (s < 0)
					{
						changed |= prune1Var(s, p, o, Part.SUBJECT);
					} else if (p < 0)
					{
						changed |= prune1Var(s, p, o, Part.PREDICATE);
					} else if (o < 0)
					{
						changed |= prune1Var(s, p, o, Part.OBJECT);
					} else 
					{
						changed |= pruneTriple(s, p, o);
					} 
					
					// * If a value occurs as the single candidate for two nodes,
					//   remove it from one (triggering a failure later)
					for(int key : keys)
						if(candidates.get(key).size() == 1 && ! isRelation(key))
							for(int other : candidates.keySet())
								if(other != key && candidates.get(other).size() == 1 && ! isRelation(other))
									if(candidates.get(other).equals(candidates.get(key)))
										candidates.get(key).clear();
					
					// * If we've found a reason the pattern can't be satisfied.
					//   (the search algorithm will end the branch at the next 
					//   recursion)
					if(isFailed())
						return;
				}
			}
		}
		
		/** 
		 * Prune a fully grounded triple
		 * @param s
		 * @param p
		 * @param o
		 * @return
		 */
		private boolean pruneTriple(int s, int p, int o)
		{
			if(graph.find(s, p, o).isEmpty())
			{
				// Set any candidate to empty to trigger failure
				if(! candidates.isEmpty())
					candidates.values().iterator().next().clear();
			}
			
			return false;
		}
		
		private int s(int s, int p, int o, Part part)
		{
			if(part == Part.SUBJECT)
				return s;
			if(part == Part.PREDICATE)
				return p;
			return o;
		}
		
		private int[] o(int s, int p, int o, Part part)
		{
			if(part == Part.SUBJECT)
				return new int [] {p, o};
			if(part == Part.PREDICATE)
				return new int [] {s, o};
			return new int [] {s, p};
		}
		
		private Part[] o(Part part)
		{
			if(part == SUBJECT)
				return new Part [] {PREDICATE, OBJECT};
			if(part == PREDICATE)
				return new Part [] {SUBJECT, OBJECT};
			if(part == OBJECT)
				return new Part [] {SUBJECT, PREDICATE};
			
			return null;
		}

		private Set<Triple> query(int s, int p, int o, Part var)  
		{		
			if(var == Part.SUBJECT)
				return graph.find(null, p, o);
			if(var == Part.PREDICATE)
				return graph.find(s, null, o);
			if(var == OBJECT)
				return graph.find(s, p, null);
			
			return null;
		}
		
		private Set<Triple> query(int s, int p, int o, Part from, Part to, int fc)  
		{		
			if(from == Part.SUBJECT)
				s = fc;
			if(from == Part.PREDICATE)
				p = fc;
			if(from == Part.OBJECT)
				o = fc;
			
			if(to == Part.SUBJECT)
				return graph.find(null, p, o);
			if(to == Part.PREDICATE)
				return graph.find(s, null, o);
			if(to == Part.OBJECT)
				return graph.find(s, p, null);
			
			return null;
		}
		
		
		private Set<Triple> query(int s, int p, int o, Part nonvar, int fc)  
		{		
			if(nonvar == Part.SUBJECT)
				return graph.find(fc, null, null);
			if(nonvar == Part.PREDICATE)
				return graph.find(null, fc, null);
			if(nonvar == Part.OBJECT)
				return graph.find(null, null, fc);
			
			return null;
		}

		private boolean prune1Var(int s, int p, int o, Part var)
		{
			int v = s(s, p, o, var);
			
			Set<Integer> oldC = candidates.get(v);
			
			Set<Integer> newC = new LinkedHashSet<>();
			for(Triple triple : query(s, p, o, var))
				newC.add(triple.get(var));
			
			newC = intersect(oldC, newC);
			
			if( newC.size() != oldC.size() || (! oldC.equals(newC)) )
			{
				candidates.put(v, newC);
				return true;
			}
				
			return false;
		}

		private boolean prune2Var(int s, int p, int o, Part from, Part to)
		{			
			
			int f = s(s, p, o, from),
			    t = s(s, p, o, to);
			
			Set<Integer> fOld = candidates.get(f),
    		             tOld = candidates.get(t);
    		
    		Set<Integer> fToRemove = new LinkedHashSet<>();
    		Set<Integer> tToRetain = new LinkedHashSet<>();
    		
    		for(int fc : candidates.get(f))
    		{
    			Set<Integer> neighbs = new LinkedHashSet<>();
    			for(Triple triple : query(s, p, o, from, to, fc))
    				neighbs.add(triple.get(to));
    			
    			neighbs = intersect(neighbs, tOld);
    			
    			if(neighbs.isEmpty())
    				fToRemove.add(fc);
    			
    			tToRetain.addAll(neighbs);
    		}
    		
    		candidates.put(f, minus(candidates.get(f), fToRemove));
    		candidates.put(t, intersect(candidates.get(t), tToRetain));
    	
//    		// * Check if pattern can still be satisfied
//    		if(candidates.get(f).isEmpty() || candidates.get(t).isEmpty())
//    			fail = true;
    		
    		return (! fOld.equals(candidates.get(f))) || (! tOld.equals(candidates.get(t)));		
    	}
		
		private boolean prune3Var(int s, int p, int o, Part from)
		{
			int    f = s(s, p, o, from);
			int[]  t = o(s, p, o, from);
			Part[] tp = o(from);
			
			Set<Integer> fOld = candidates.get(f),
		                 t0Old = candidates.get(t[0]),
		                 t1Old = candidates.get(t[1]);

    		Set<Integer> fToRemove = new LinkedHashSet<>();
    		Set<Integer> t0ToRetain = new LinkedHashSet<>();
    		Set<Integer> t1ToRetain = new LinkedHashSet<>();
    		
			for(int fc : candidates.get(f))
    		{
    			Set<Integer> neighbs0 = new LinkedHashSet<>();
    			Set<Integer> neighbs1 = new LinkedHashSet<>();

    			for(Triple triple : query(s, p, o, from, fc))
    			{
    				int[] values = o(triple.subject(), triple.predicate(), triple.object(), from);
    				
    				if(t0Old.contains(values[0]) && t1Old.contains(values[1]) )
    				{
    				
    					neighbs0.add(triple.get(tp[0]));
    					neighbs1.add(triple.get(tp[1]));
    				}
    			}
    			
    			neighbs0 = intersect(neighbs0, t0Old);
    			neighbs1 = intersect(neighbs1, t1Old);

    			if(neighbs0.isEmpty() || neighbs1.isEmpty())
    				fToRemove.add(fc);
			
    			t0ToRetain.addAll(neighbs0);
    			t1ToRetain.addAll(neighbs1);
    		}
			
    		candidates.put(f, minus(candidates.get(f), fToRemove));
    		candidates.put(t[0], intersect(candidates.get(t[0]), t0ToRetain));
    		candidates.put(t[1], intersect(candidates.get(t[1]), t1ToRetain));
    		
    		return (! fOld.equals(candidates.get(f))) || (! t0Old.equals(candidates.get(t[0]))) ||  (! t1Old.equals(candidates.get(t[1])));	
		}
		
		public String toString()
		{
			return candidates.toString();
		}
	}
	
	private static <T> Set<T> intersect(Set<T> a, Set<T> b)
	{
		Set<T> small, large;
		if(a.size() < b.size())
		{
			small = a;
			large = b;
		} else 
		{
			small = b;
			large = a;
		}
		
		Set<T> res = new LinkedHashSet<T>(small);
		res.retainAll(large);
		
		return res;
	}
	
	private static <T> Set<T> minus(Set<T> a, Set<T> b)
	{
		Set<T> res = new LinkedHashSet<T>(a);
		res.removeAll(b);
		return res;
	}
	
	private static List<Set<Integer>> toSets(Collection<Triple> triples)
	{
		Set<Integer> s = new LinkedHashSet<>(),
		             p = new LinkedHashSet<>(),
		             o = new LinkedHashSet<>();
		
		for(Triple triple : triples)
		{
			s.add(triple.subject());
			p.add(triple.predicate());
			o.add(triple.object());
		}
		
		return asList(s, p, o);
	}
}
