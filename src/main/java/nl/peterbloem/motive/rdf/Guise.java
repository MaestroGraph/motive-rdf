//package nl.peterbloem.motive.rdf;
//
//import static nl.peterbloem.kit.Series.series;
//
//import java.util.AbstractList;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashSet;
//import java.util.LinkedHashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//
//import org.nodes.Graphs;
//import org.nodes.random.SimpleSubgraphGenerator;
//
//import nl.peterbloem.kit.AbstractGenerator;
//import nl.peterbloem.kit.Global;
//import nl.peterbloem.kit.Series;
//import nl.peterbloem.motive.rdf.KGraphList.KNode;
//
//public class Guise extends AbstractGenerator<List<Integer>>
//{
//	private KGraphList data;
//	
//	private int size;
//
//	private List<Integer> current;
//	private int currentDegree;
//
//	private int skipSize;
//	
//	public Guise(KGraphList data, int size, int skipSize)
//	{
//		assert(skipSize > 0);
//		
//		data.sort();
//		
//		this.data = data;
//		this.size = size;
//		this.skipSize = skipSize;
//		
//		current = new SimpleSubgraphGenerator(data, size).generate();
//		currentDegree = count(current, data);
//	}
//
//	public void step()
//	{
//		int k = Global.random().nextInt(currentDegree);
//		List<Integer> next = select(k, current, data); 
//		
//		
//		int nextDegree = count(next, data); 
//		
//		double acceptance = currentDegree / (double) nextDegree;
//		
//		if(Global.random().nextDouble() < acceptance)
//		{
//			current = next;
//			currentDegree = nextDegree;
//		}
//	}
//	
//	@Override
//	public List<Integer> generate()
//	{
//		for(int i : series(skipSize))
//			step();
//		
//		return Collections.unmodifiableList(current);
//	}
//
//
//	/**
//	 * Count the number of neighbors in the subgraph-graph of the current 
//	 * selection of nodes.
//	 * 
//	 * (the degree of node 'current' in the Guise graph)
//	 *   
//	 * @param current
//	 * @return
//	 */
//	public static int count(List<Integer> current, KGraphList data)
//	{
//		assert(current.size() == new HashSet<>(current).size());
//		
//		int sum = 0;
//		for(int iRem : Series.series(current.size()))
//		{
//			List<Integer> minus = minus(current, iRem);
//			
//			if(Graphs.connected(minus, data))
//			{
//				List<List<KNode>> adjLists = new ArrayList<>(minus.size());
//				
//				for(int index : minus)
//				{
//					adjLists.add(data.neighborsFastIn(data.get(index)));
//					adjLists.add(data.neighborsFastOut(data.get(index)));
//
//				}
//				
//				List<KNode> union = union(adjLists, new Comparator<KNode>(){
//					public int compare(KNode x, KNode y)
//					{ return Integer.compare(x.index(), y.index());
//					}});
//					
//				sum += union.size() - current.size();
//			}
//		}
//		
//		return sum;
//	}
//
//	/**
//	 * Select the n-th neighbor of the 'current' node.
//	 *   
//	 * @param current
//	 * @return
//	 */
//	public static List<Integer> select(int k, List<Integer> current, KGraphList data)
//	{
//		assert(current.size() == new HashSet<>(current).size());
//		
//		int sum = 0;
//		for(int iRem : Series.series(current.size()))
//		{
//			List<Integer> minus = minus(current, iRem);
//			
//			if(Graphs.connected(minus, data))
//			{
//				List<List<KNode>> adjLists = new ArrayList<>(minus.size());
//				
//				for(int index : minus)
//				{
//					adjLists.add(data.neighborsFastIn(data.get(index)));
//					adjLists.add(data.neighborsFastOut(data.get(index)));
//
//				}
//				
//				List<KNode> union = union(adjLists, new Comparator<KNode>(){
//					public int compare(KNode x, KNode y)
//					{ return Integer.compare(x.index(), y.index());
//					}});
//				
//				int increment = union.size() - current.size();
//								
//				if(k >= sum + increment)
//					sum += increment;
//				else
//				{
//					union.removeAll(data.nodes(current));
//					int addition = union.get(k - sum).index();
//					
//					List<Integer> result = new ArrayList<>(minus);
//					result.add(addition);
//					return result;
//				}
//			}
//		}
//		
//		return null;
//	}
//
//	/**
//	 * Produces the intersection of an arbitrary number of sorted lists. Equality is 
//	 * determined by the comparator.
//	 * 
//	 * Result is not backed by the inputs.
//	 * 
//	 * @param lists
//	 * @param c
//	 * @return
//	 */
//	public static <L> List<L> intersect(List<List<L>> lists, Comparator<L> c)
//	{
//		if(lists.isEmpty())
//			return Collections.emptyList();
//		
//		List<L> result = new ArrayList<>();
//		
//		List<Integer> indices = new ArrayList<>();
//		for(List<L> list : lists)
//		{ 
//			if(list.isEmpty())
//				return Collections.emptyList();
//			indices.add(0);
//		}
//		
//		boolean done = false;
//		while(! done)
//		{
//			// - loop over all list[index] to get the max value, and to check 
//			//   if they're all equal
//			
//			boolean equal = true;
//			L max = null, last = null;
//			
//			for(int h : series(lists.size()))
//			{
//				L cur = lists.get(h).get(indices.get(h));
//				max = max(max, cur, c);
//				if(last != null && ! eq(cur, last, c))
//					equal = false;
//				
//				last = cur;
//			}
//			
//			if(equal)
//			{
//				result.add(last);
//				
//				for(int h : series(lists.size()))
//				{
//					indices.set(h, indices.get(h) + 1);
//					if(indices.get(h) >= lists.get(h).size())
//					{
//						// as soon as one index hist the end of the list we're done. 
//						done = true;
//						continue; 
//					}
//				}
//			} else
//			{
//				for(int h : series(lists.size()))
//					if(lt(lists.get(h).get(indices.get(h)), max,c))
//					{
//						indices.set(h, indices.get(h) + 1);
//						if(indices.get(h) >= lists.get(h).size())
//						{
//							// as soon as one index hits the end of the list we're done. 
//							done = true;
//							continue; 
//						}
//					}
//			}
//		}
//		
//		return result;
//	}
//	
//	/**
//	 * Produces the union of an arbitrary number of sorted lists. Equality is 
//	 * determined by the comparator.
//	 * 
//	 * Lists should not contain null.
//	 * 
//	 * @param lists
//	 * @param c
//	 * @return
//	 */
//	public static <L> List<L> union(List<List<L>> lists, Comparator<L> c)
//	{
//		List<L> result = new ArrayList<>();
//		
//		List<Integer> indices = new ArrayList<>();
//		for(List<L> adj : lists)
//			indices.add(0);
//		
//		boolean done = false;
//		while(! done)
//		{
//			// - loop over all list[index] to get the max value, and to check 
//			//   if they're all equal
//			
//			L min = null;
//			
//			for(int h : series(lists.size()))
//			{
//				L cur = indices.get(h) < lists.get(h).size() ? lists.get(h).get(indices.get(h)) : null;
//				min = min(min, cur, c);
//			}
//			
//			if(min == null)
//			{
//				done = true;
//				continue;
//			}
//			
//			result.add(min);
//			
//			// * increment every index pointing to an element equal to min until 
//			//   it points to something different
//			for(int h : series(lists.size()))
//			{					
//				L cur = indices.get(h) < lists.get(h).size() ? lists.get(h).get(indices.get(h)) : null;
//				while(cur != null && eq(cur, min, c))
//				{
//					indices.set(h, indices.get(h) + 1);
//					cur = indices.get(h) < lists.get(h).size() ? lists.get(h).get(indices.get(h)) : null;
//				}		
//			}
//			
//		}
//		
//		return result;
//	}
//
//	private static <L> L max (L one, L two, Comparator<L> comp)
//	{
//		if(one == null)
//			return two;
//		if(two == null)
//			return one;
//		
//		return comp.compare(one, two) >= 0 ? one : two;
//	}
//	
//	private static <L> L min (L one, L two, Comparator<L> comp)
//	{
//		if(one == null)
//			return two;
//		if(two == null)
//			return one;
//		
//		return comp.compare(one, two) <= 0 ? one : two;
//	}
//	
//	private static <L> boolean lt(L arg, L mark, Comparator<L> comp)
//	{
//		return comp.compare(arg, mark) < 0;
//	}
//	
//	private static <L> boolean eq(L arg, L mark, Comparator<L> comp)
//	{
//		if(arg == null || mark == null)
//			return false;
//		return comp.compare(arg, mark) == 0;
//	}
//
//	/**
//	 * Returns a list containing all elements of the current one, except the 
//	 * one at index i.
//	 * 
//	 * The returned list is backed by the argument.
//	 *  
//	 * @param current
//	 * @param iRem
//	 * @return
//	 */
//	public static <T> List<T> minus(final List<T> current, final int iRem)
//	{
//		return new AbstractList<T>(){
//
//			@Override
//			public T get(int index)
//			{
//				return index < iRem ? current.get(index) : current.get(index + 1);
//			}
//
//			@Override
//			public int size()
//			{
//				return current.size() - 1;
//			}
//		
//		};
//	}
//}
