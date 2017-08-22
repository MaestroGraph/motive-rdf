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
 * @author Peter
 *
 */
public class Nauty
{

	public static DTGraph<Integer, Integer> canonical(DTGraph<Integer, Integer> pattern)
	{
		return new Nauty(pattern).canonical;
	}
	
	private DTGraph<Integer, Integer> pattern;
	private Map<Integer, List<DTLink<Integer, Integer>>> tagToLink = new LinkedHashMap<>();
	private DTGraph<Integer, Integer> canonical;
		
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
		
		Partition max = search.max();
		this.canonical = reorder(max);
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
	public boolean tagsShatterNodes(List<Integer> nodes, List<Integer> tags)
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
				if(lastInDegree != inDegree && lastOutDegree != outDegree)
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
	public boolean nodesShatterTags(List<Integer> nodes, List<Integer> tags)
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
				if(lastFromDegree != fromDegree && lastFromDegree != fromDegree)
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
		List<Integer> labels = new ArrayList<Integer>(pattern.labels());
		Collections.sort(labels);
		
		DTGraph<Integer, Integer> out = new MapDTGraph<>();
		
		for(Integer label : labels)
			out.add(label);
		
		for(DTLink<Integer, Integer> link : pattern.links())
		{
			DTNode<Integer, Integer> from = out.node(link.from().label());
			DTNode<Integer, Integer> to   = out.node(link.to().label());
			from.connect(to, link.tag());
		}
		
		return out;
	}
	
	public DTGraph<Integer, Integer> canonical()
	{
		return canonical();
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
		private String maxString;

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
			String nodeString = Nauty.this.toString(node.partition());
			
			if(max == null || nodeString.compareTo(maxString) > 0)
			{
				max = node;
				maxString = nodeString;
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
	private String toString(Partition partition)
	{				
		StringBuffer buffer = new StringBuffer();
		
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

			buffer.append(" "+cell.get(0)+" ");
		
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
				buffer.append(pair.first()).append(' ').append(pair.second()).append(' ');
			
			buffer.append(',');
		}
		
		return buffer.toString();
	}
	
}
