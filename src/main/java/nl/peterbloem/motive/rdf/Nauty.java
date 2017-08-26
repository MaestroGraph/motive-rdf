package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Series.series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.TGraph;

import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Order;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

/**
 * Partial implementation of the Nauty algorithm specifically for patterns.
 * 
 * Example use:
 * <pre>
 * {@code
 * DTGraph<Integer, Integer> canonized = Nauty.canonical(pattern)
 * }
 * </pre
 * 
 * NB: Currently only works for connected graphs.
 * 
 * @author Peter
 *
 */
public class Nauty
{
	// * If the search takes longer than this number of miliseconds, log a message
	private static final int LOG_INTERVAL = 10000; 
	
	public static DTGraph<Integer, Integer> canonical(DTGraph<Integer, Integer> pattern)
	{
		return new Nauty(pattern).canonical;
	}
	
	public static DTGraph<Integer, Integer> canonical(DTGraph<Integer, Integer> pattern, List<Integer> values)
	{
		Nauty nauty = new Nauty(pattern);

		int numVarLabels = 0;
		for(int label : pattern.labels())
			if(label < 0)
				numVarLabels ++;
		
		List<Integer> nwValues = new ArrayList<>(values.size());
		
		for(List<Integer> cell : nauty.maxPartition.first())
			if(cell.get(0) < 0)
				nwValues.add(values.get(-cell.get(0) - 1));
		
		for(List<Integer> cell : nauty.maxPartition.second())
			if(cell.get(0) < 0)
				nwValues.add(values.get(-cell.get(0) - 1 + numVarLabels));

		values.clear();
		values.addAll(nwValues);
		
		return nauty.canonical;
	}
	
	private DTGraph<Integer, Integer> pattern;
	private Map<Integer, List<DTLink<Integer, Integer>>> tagToLink = new LinkedHashMap<>();
	private DTGraph<Integer, Integer> canonical;
	private Partition maxPartition;

	long t0 = System.currentTimeMillis();
	long tLast = t0;
		
	private Nauty(DTGraph<Integer, Integer> pattern)
	{
		this.pattern = pattern;
		
		for(DTLink<Integer, Integer> link : pattern.links())
		{
			if(! tagToLink.containsKey(link.tag()))
				tagToLink.put(link.tag(), new ArrayList<DTLink<Integer, Integer>>());
		
			tagToLink.get(link.tag()).add(link);
		}			
		
		// * Start with the unit partition
		Partition partition = unitPartition();
		
		// * The equitable refinement procedure.
		while(searchShattering(partition));
			
		// * Start the search for the maximal isomorph
		Search search = new Search(partition);
		search.search();
		
		maxPartition = search.max();
		this.canonical = reorder(maxPartition);
	}
	 
	
	/**
	 * Create the initial partition. Each non-variable node/tag gets its own color
	 * all variable nodes are lumped together in one color, all tag variables are 
	 * are lumped together in one color.
	 * @return
	 */
	public Partition unitPartition()
	{
		Comparator<List<Integer>> comp = new Comparator<List<Integer>>()
		{
			@Override
			public int compare(List<Integer> o1, List<Integer> o2)
			{
				return o1.get(0).compareTo(o2.get(0));
			}
			
		};
		
		List<List<Integer>> nodeList = new ArrayList<>();
		List<Integer> nodeCell = new ArrayList<>();
		for(Integer label : pattern.labels())
			if(label < 0)
				nodeCell.add(label);
			else
				nodeList.add(Arrays.asList(label));
		
		
		Collections.sort(nodeList, comp);
		if(! nodeCell.isEmpty())
			nodeList.add(nodeCell);
		
		List<List<Integer>> linkList = new ArrayList<>();
		List<Integer> linkCell = new ArrayList<Integer>();
		for(Integer tag : pattern.tags())
			if(tag < 0)
				linkCell.add(tag);
			else
				linkList.add(Arrays.asList(tag));
		
		Collections.sort(linkList, comp);
		if(! linkCell.isEmpty())
			linkList.add(linkCell);
		
		return new Partition(nodeList, linkList);
	}

	private boolean searchShattering(Partition partition)
	{		
		boolean a = searchShatteringTags(partition);
		boolean b = searchShatteringNodes(partition);
		
		return a || b;
	}
	
	private boolean searchShatteringTags(Partition partition)
	{
		for(int i : series(partition.first().size()))
			for(int j : series(partition.second().size()))
			{
				List<Integer> nodeCell = partition.first().get(i);
				List<Integer> linkCell = partition.second().get(j);
								
				if(tagsShatterNodes(nodeCell, linkCell))
				{
					// * Refine the node cell
					// - This edit to the list we're looping over is safe, 
					//   because we return right after
					
					partition.first().remove(i);
					partition.first().addAll(shatterNodes(nodeCell, linkCell));
					
					return true;
				}
			}
				
		return false;
	}
	
	private boolean searchShatteringNodes(Partition partition)
	{
		for(int i : series(partition.first().size()))
			for(int j : series(partition.second().size()))
			{
				List<Integer> nodeCell = partition.first().get(i);
				List<Integer> linkCell = partition.second().get(j);
				
				if(nodesShatterTags(nodeCell,  linkCell))
				{
					// * Refine the link cell
					// - This edit to the list we're looping over is safe, 
					//   because we return right after
					partition.second().remove(j);
					partition.second().addAll(shatterTags(nodeCell, linkCell));
					
					return true;
				}
			}
				
		return false;
	}
	
	private List<List<Integer>> shatterNodes(List<Integer> nodes, List<Integer> tags)
	{
		// from in/out degree to node label
		Map<Pair<Integer, Integer>, List<Integer>> byDegree = new LinkedHashMap<Pair<Integer, Integer>, List<Integer>>();
		
		Set<Integer> tagSet = new HashSet<Integer>(tags);
		
		for(Integer nodeLabel : nodes)
		{
			// * count the degrees
			int inDegree = 0, outDegree = 0;
			DTNode<Integer, Integer> node = pattern.node(nodeLabel);
			
			for(DTLink<Integer, Integer> link : node.linksIn())
				if(tagSet.contains(link.tag()))
					inDegree ++;
			
			for(DTLink<Integer, Integer> link : node.linksOut())
				if(tagSet.contains(link.tag()))
					outDegree ++;
			
			Pair<Integer, Integer> degrees = Pair.p(inDegree, outDegree);
			
			if(! byDegree.containsKey(degrees))
				byDegree.put(degrees, new ArrayList<Integer>());
				
			byDegree.get(degrees).add(nodeLabel);
		}
		
		List<Pair<Integer, Integer>> keys = new ArrayList<>(byDegree.keySet());
		Collections.sort(keys, new Pair.NaturalPairComparator<>());
		
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for(Pair<Integer, Integer> key : keys)
			result.add(byDegree.get(key));
		
		return result;
	}	
	
	private List<List<Integer>> shatterTags(List<Integer> nodes, List<Integer> tags)
	{
		// from in/out degree to node label
		Map<Pair<Integer, Integer>, List<Integer>> byDegree = new LinkedHashMap<Pair<Integer, Integer>, List<Integer>>();
		
		Set<Integer> nodeSet = new HashSet<Integer>(nodes);
		
		for(Integer tag : tags)
		{
			// * count the degrees
			int fromDegree = 0, toDegree = 0;
			
			for(DTLink<Integer, Integer> link : tagToLink.get(tag))
			{
				if(nodeSet.contains(link.from().label()))
					fromDegree ++;
				
				if(nodeSet.contains(link.to().label()))
					toDegree ++;
			}

			Pair<Integer, Integer> degrees = Pair.p(fromDegree, toDegree);
			
			if(! byDegree.containsKey(degrees))
				byDegree.put(degrees, new ArrayList<Integer>());
				
			byDegree.get(degrees).add(tag);
		}
		
		List<Pair<Integer, Integer>> keys = new ArrayList<>(byDegree.keySet());
		Collections.sort(keys, new Pair.NaturalPairComparator<>());
		
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for(Pair<Integer, Integer> key : keys)
			result.add(byDegree.get(key));
		
		return result;
	}

	/**
	 * A tag color shatters a set of nodes if the number of incoming and outgoing links
	 * with that color differs between the nodes in the set. 
	 *  
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean tagsShatterNodes(List<Integer> nodes, List<Integer> tags)
	{
		int lastInDegree = -1, lastOutDegree = -1;
		Set<Integer> tagSet = new HashSet<Integer>(tags);
		
		for(Integer nodeLabel : nodes)
		{
			int inDegree = 0, outDegree = 0;
			DTNode<Integer, Integer> node = pattern.node(nodeLabel);
			
			for(DTLink<Integer, Integer> link : node.linksIn())
				if(tagSet.contains(link.tag()))
					inDegree ++;
			
			for(DTLink<Integer, Integer> link : node.linksOut())
				if(tagSet.contains(link.tag()))
					outDegree ++;
						
			if(lastInDegree != -1)
				if(lastInDegree != inDegree || lastOutDegree != outDegree)
					return true;
			
			lastInDegree = inDegree;
			lastOutDegree = outDegree;
		}

		return false;
	}
	
	/**
	 * A node color shatters a tag color of the number of tags with that color 
	 * as a from node and the number of tags with that color as a to node differes
	 * between the members of the tag color 
	 *  
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean nodesShatterTags(List<Integer> nodes, List<Integer> tags)
	{
		
		int lastFromDegree = -1, lastToDegree = -1;
		Set<Integer> nodeSet = new HashSet<Integer>(nodes);
		
		for(Integer tag : tags)
		{
			int fromDegree = 0, toDegree = 0;
			for(DTLink<Integer, Integer> link : tagToLink.get(tag))
			{
				if(nodeSet.contains(link.from().label()))
					fromDegree ++;
				
				if(nodeSet.contains(link.to().label()))
					toDegree ++;
			}
			
			if(lastFromDegree != -1)
				if(lastFromDegree != fromDegree || lastToDegree != toDegree)
					return true;
			
			lastFromDegree = fromDegree;
			lastToDegree = toDegree;
			
		}
		
		return false;
	}
	
	/**
	 * Return a re-ordered version of the pattern, based on the current labels.
	 * 
	 * @param pattern
	 */
	public DTGraph<Integer, Integer> reorder(Partition partition)
	{
		List<Triple> triples = toString(partition);
		
		DTGraph<Integer, Integer> out = new MapDTGraph<>();
		
		for(Triple t : triples)
		{
			DTNode<Integer, Integer> subject = out.node(t.subject);
			DTNode<Integer, Integer> object = out.node(t.object);
			
			if(subject == null)
				subject = out.add(t.subject);
			if(object == null)
				object = out.add(t.object);
			
			subject.connect(object, t.predicate);
		}
		
		return out;
	}
	
	public DTGraph<Integer, Integer> canonical()
	{
		return canonical;
	}

	private static class Partition extends Pair<List<List<Integer>>, List<List<Integer>>>
	{
		public Partition(List<List<Integer>> one, List<List<Integer>> two)
		{
			super(one, two);
		}

		private static final long serialVersionUID = -3988329747475470825L;
	}
	
	/**
	 * This object encapsulates the information in a single search.
	 * 
	 * @author Peter
	 *
	 */
	private class Search
	{
		private Deque<SNode> buffer = new LinkedList<SNode>();
		
		private SNode max = null;
		private List<Triple> maxString;

		public Search(Partition startPartition)
		{
			// ** Set up the search stack
			buffer.add(new SNode(startPartition));
		}
		
		public void search()
		{
			while(! buffer.isEmpty())
			{
				SNode current = buffer.poll();
				
				List<SNode> children = current.children();
				if(children.isEmpty())
					observe(current);
				
				for(SNode child : children)
					buffer.addFirst(child);
			}
		}
		
		private void observe(SNode node)
		{						
			List<Triple> nodeString = Nauty.this.toString(node.partition());
			
			if(max == null || compare(nodeString, maxString) > 0)
			{
				max = node;
				maxString = nodeString;
			}
			
			if(System.currentTimeMillis() - tLast > LOG_INTERVAL)
			{
				Global.log().warning(((System.currentTimeMillis() - t0)/1000.0) + " seconds taken. Current pattern: " + pattern);
				tLast = System.currentTimeMillis();
			}
		}
	
		public Partition max()
		{
			return max.partition();
		}
	}
	
	private static class SNode
	{
		private Partition partition;

		public SNode(Partition partition)
		{
			super();
			this.partition = partition;
		}
		
		public Partition partition()
		{
			return partition;
		}

		public List<SNode> children()
		{
			// - A child node is made by choosing a cell in one of the two lists
			//   of the partition with more than one member, an splitting off one 
			//   member 
			
			List<SNode> children = new ArrayList<SNode>(partition.first().size() + partition.second().size() + 1);
			
			// * loop over the node cells
			for(int cellIndex : series(partition.first().size()))
			{
				List<Integer> cell = partition.first().get(cellIndex);
				
				if(cell.size() > 1)
					for(int nodeIndex : series(cell.size()))
					{
						List<Integer> rest = new ArrayList<>(cell);
						List<Integer> single = Arrays.asList(rest.remove(nodeIndex));
						
						// * Careful... We're shallow copying the cells. We must 
						//   make sure never to modify a cell.
						List<List<Integer>> newPartitionFirst = new ArrayList<>(partition.first());
						
						newPartitionFirst.remove(cellIndex);
						newPartitionFirst.add(cellIndex, single);
						newPartitionFirst.add(cellIndex + 1, rest);
						
						children.add(new SNode(new Partition(newPartitionFirst, partition.second()) ));
					}
			}
			
			// * loop over the tag cells
			for(int cellIndex : series(partition.second().size()))
			{
				List<Integer> cell = partition.second().get(cellIndex);
				
				if(cell.size() > 1)
					for(int tagIndex : series(cell.size()))
					{
						List<Integer> rest = new ArrayList<>(cell);
						List<Integer> single = Arrays.asList(rest.remove(tagIndex));
						
						// * Careful... We're shallow copying the cells. We must 
						//   make sure never to modify a cell.
						List<List<Integer>> newPartitionSecond = new ArrayList<>(partition.second());
						
						newPartitionSecond.remove(cellIndex);
						newPartitionSecond.add(cellIndex, single);
						newPartitionSecond.add(cellIndex + 1, rest);
						
						children.add(new SNode(new Partition(partition.first(), newPartitionSecond) ));
					}
			}
			
			return children;
		}
	}
	
	/**
	 * Converts a trivial partition to a string representing the graph's 
	 * structure (without labels) in a particular format.
	 *  
	 * @param partition
	 * @return
	 */
	private List<Triple> toString(Partition partition)
	{	
		List<Triple> triples = new ArrayList<Triple>((int)pattern.numLinks());
		
		int negLabel = -1, negTag = -1; // next available negative label

		Map<Integer, Integer> newLabels = new HashMap<>();
		Map<Integer, Integer> newTags = new HashMap<>();
		
		for(List<Integer> cell : partition.first())
		{
			assert(cell.size() == 1);

			int label = cell.get(0);
			if(label >= 0)
				newLabels.put(label, label);
			else
				newLabels.put(label, negLabel --);
		}
		
		for(List<Integer> cell : partition.second())
		{
			assert(cell.size() == 1);

			int tag = cell.get(0);
			if(tag >= 0)
				newTags.put(tag, tag);
			else
				newTags.put(tag, negTag --);
		}
		
		for(List<Integer> cell : partition.first())
		{
			DTNode<Integer, Integer> current = pattern.node(cell.get(0));
			
			List<Pair<Integer, Integer>> neighbors = new ArrayList<>(current.neighbors().size());
			for(DTLink<Integer, Integer> link : current.linksOut())
			{
				int tag = link.tag();
				int to  = link.to().label();
				neighbors.add(Pair.p(newTags.get(tag), newLabels.get(to)));
			}
			
			Collections.sort(neighbors, new Pair.NaturalPairComparator<>());
			for(Pair<Integer, Integer> pair : neighbors)
				triples.add(new Triple(newLabels.get(cell.get(0)), pair.first(), pair.second()));
		}
				
		return triples;
	}
	
	
	private class Triple {
		int subject, predicate, object;

		public Triple(int subject, int predicate, int object)
		{
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
		}

		private int compareTo(Triple o)
		{
			int c = Integer.compare(subject, o.subject);
			if(c != 0)
				return c;
			
			c = Integer.compare(predicate, o.predicate);
			if(c != 0)
				return c;
			
			return Integer.compare(object, o.object);		
		}

		@Override
		public String toString()
		{
			return subject + " " + predicate + " " + object;
		}
	}
	
	public static int compare(List<Triple> a, List<Triple> b)
	{
		if(a.size() != b.size())
			return Integer.compare(a.size(), b.size());
		
		for(int i : series(a.size()))
		{
			int c = a.get(i).compareTo(b.get(i));
			if(c != 0)
				return c;
		}
		
		return 0;
	}
}
