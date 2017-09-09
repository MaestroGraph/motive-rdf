package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Functions.concat;
import static nl.peterbloem.kit.Functions.dot;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Pref.shorten;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.nodes.DGraph;
import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.FastWalkable;
import org.nodes.Graph;
import org.nodes.LightDGraph;
import org.nodes.LightDTGraph;
import org.nodes.Link;
import org.nodes.MapDTGraph;
import org.nodes.Node;
import org.nodes.TLink;
import org.nodes.TNode;
import org.nodes.UGraph;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.dictionary.DictionarySection;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

/**
 * A simplified object representing a knowledgegraph. Node labels are their 
 * indices.
 * 
 * NOTES:
 * <ul>
 * <li>
 * Removing a node will cause all labels (above it) to change. Removing a 
 * node or link may cause tags to change. This makes removals potentially very
 * costly.</li>
 * <li>
 * For performance reasons, multiple links are allowed in this data-structure.
 * They are not allowed in the knowledge graph models we use, so users should ensure
 * manually that their graph does not contain multiple links.
 * </li>
 * </ul> 
 * 
 * @author Peter
 *
 */
public class KGraph implements 
		DTGraph<Integer, Integer>, 
		FastWalkable<Integer, KGraph.KNode>
{
	// * the initial capacity reserved for neighbors
	public static final int NEIGHBOR_CAPACITY = 5;

	private List<List<Integer>> in;	
	private List<List<Integer>> out;

	private List<List<Integer>> inTags;
	private List<List<Integer>> outTags;

	private long numLinks = 0;
	private long modCount = 0;	
	
	// * changes for any edit which causes the node indices to change 
	//   (currently just removal). If this happens, all existing Node and Link 
	//   objects lose persistence 
	private long nodeModCount = 0;

	private int hash;
	private Long hashMod = null;

	private boolean sorted = false;
	
	private int maxTag = -1;
	
	public KGraph()
	{
		this(16);
	}
	
	public KGraph(int capacity)
	{
		in = new ArrayList<List<Integer>>(capacity); 
		out = new ArrayList<List<Integer>>(capacity);

		inTags = new ArrayList<List<Integer>>(capacity); 
		outTags = new ArrayList<List<Integer>>(capacity); 
	}

	public int size()
	{
		return in.size();
	}

	public long numLinks()
	{
		return numLinks;
	}

	public Set<Integer> tags()
	{
		return new SeriesSet(maxTag + 1);
	}

	public KNode node(Integer label)
	{
		if(label < 0 || label >= size()) 
			return null;
		
		return new KNode(label);
	}

	public Collection<KNode> nodes(Integer label)
	{
		return Arrays.asList(node(label));
	}

	public List<KNode> nodes()
	{
		return new NodeList(Series.series(size()));
	}

	public KNode get(int i)
	{
		return node(i);
	}

	public Iterable<KLink> links()
	{
		return new LinkCollection();		
	}
	
	private class LinkCollection extends AbstractCollection<KLink>
	{
		@Override
		public Iterator<KLink> iterator()
		{
			return new LLIterator();
		}

		@Override
		public int size()
		{
			return (int) numLinks;
		}

		private class LLIterator implements Iterator<KLink>
		{
			private static final int BUFFER_LIMIT = 5;
			private long graphState = state();
			
			// *Buffer contains pair of integers representing 
			private Deque<KLink> buffer = new LinkedList<KLink>();
			
			// * Next node 
			int next = 0;

			private void check()
			{
				if(graphState != state())
					throw new ConcurrentModificationException("Graph has been modified.");
			}

			@Override
			public boolean hasNext()
			{
				check();
				read();

				return ! buffer.isEmpty();
			}

			@Override
			public KLink next()
			{
				check();
				read();

				if(buffer.isEmpty())
					throw new NoSuchElementException();

				return buffer.pop();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Method not supported");
			}

			private void read()
			{
				if(next >= KGraph.this.size())
					return;

				while(buffer.size() < BUFFER_LIMIT && next < KGraph.this.size())
				{
					int from = next;

					List<Integer> neighbors = out.get(from);
					List<Integer> tags      = outTags.get(from);
					
					for (int i : series(neighbors.size()))
						buffer.add(new KLink(from, neighbors.get(i), tags.get(i)));

					next++;
				}			
			}
		}	
	}

	/**
	 * Add a node. It immediately receives the next available integer label 
	 * @return
	 */
	public KNode add()
	{
		return add(size());
	}
	
	/**
	 * This method is only present for 
	 */
	public KNode add(Integer label)
	{
		if(label != size())
			throw new IllegalArgumentException("Can only add node with the next available integer as a label (ie. size()). Consider using add() instead.");
		
		in.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));
		out.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));

		inTags.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));
		outTags.add(new ArrayList<Integer>(NEIGHBOR_CAPACITY));

		sorted = false;
		return new KNode(in.size() - 1);
	}

	public Set<Integer> labels()
	{
		return new SeriesSet(size());
	}

	public boolean connected(Integer from, Integer to)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public long state()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@SuppressWarnings("unchecked")
	/**
	 * A KGraph can be equal to another DTGraph<Integer, Integer>.
	 */
	public Class<? extends DTGraph<?, ?>> level()
	{
		Object obj = DTGraph.class;
		return (Class<? extends DTGraph<Integer, Integer>>) obj;	
	}
	
	public class KNode implements DTNode<Integer, Integer>
	{
		private Integer index;
		// The modCount of the graph for which this node is safe to use
		private final long nodeModState = nodeModCount;
		private boolean dead = false;
		
		public KNode(int index)
		{
			this.index = index;
		}

		@Override
		public Integer label()
		{
			check();
			return index;
		}


		@Override
		/**
		 * remove this node from the graph. This is an expensive operation
		 */
		public void remove()
		{
			check();
		
			int linksRemoved = inDegree() + outDegree();
			linksRemoved -= linksOut(this).size();
			
			numLinks -= linksRemoved;
			
			for (int i : series(in.size())) 
			{
				List<Integer> neighbors = in.get(i);
				List<Integer> tags      = inTags.get(i);
				
				assert(neighbors.size() == tags.size());
								
				// walk backwards through the list for safe removal
				for (int j : series(neighbors.size()-1, -1)) 
					if (neighbors.get(j).equals(this.index))
					{
						neighbors.remove((int) j);
						tags.remove((int) j);
					}

				assert(neighbors.size() == tags.size());

			}

			for (int i : series(out.size())) 
			{
				List<Integer> neighbors = out.get(i);
				List<Integer> tags      = outTags.get(i);
				
				for (int j : series(neighbors.size()-1, -1)) 
					if (neighbors.get(j).equals(this.index)) 
					{
						neighbors.remove((int) j);
						tags.remove((int) j);	
					}
			}

			// * move through all neighbor lists and decrement every index that 
			//   is higher than the one we just removed.  
			for(List<Integer> list : in)
				for(int i : series(list.size()))
				{
					Integer value = list.get(i);
					if(value > index)
						list.set(i, value - 1);
				}
			for(List<Integer> list : out)
				for(int i : series(list.size()))
				{
					Integer value = list.get(i);
					if(value > index)
						list.set(i, value - 1);
				}			
			
			
			in.remove((int)index);
			out.remove((int)index);

			inTags.remove((int)index);
			outTags.remove((int)index);

			dead = true;
			modCount++;
			nodeModCount++;

			sorted = false;
		}

		private void check()
		{
			if(dead)
				throw new IllegalStateException("Node is dead (index was "+index+")");

			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation.");
		}

		@Override
		public boolean dead()
		{
			return dead;
		}

		@Override
		public int degree()
		{
			check();
			return inDegree() + outDegree();
		}

		@Override
		public Collection<KNode> neighbors()
		{
			check();

			Set<Integer> indices = new LinkedHashSet<Integer>();
			
			indices.addAll(in.get(this.index));
			indices.addAll(out.get(this.index));

			return new NodeList(new ArrayList<Integer>(indices));
		}

		@Override
		public KNode neighbor(Integer label)
		{
			check();
			KNode other = new KNode(label);
			if(this.connected(other))
				return other;
			return null;
		}

		@Override
		public Collection<KNode> neighbors(Integer label)
		{
			check();
			KNode other = neighbor(label);
		
			if(other == null)
				return null;
			return Arrays.asList(other);
		}

		@Override
		public Collection<KNode> out()
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(outDegree());

			for(int i : out.get(this.index))
				indices.add(i);

			return new NodeList(indices);
		}

		@Override
		public Collection<? extends KNode> out(Integer label)
		{
			check();
			KNode other = new KNode(label);
			if(this.connectedTo(other))
				return Arrays.asList(other);
			
			return null;
		}

		@Override
		public Collection<? extends KNode> in()
		{
			check();
			List<Integer> indices = new ArrayList<Integer>(inDegree());

			for(int index : in.get(this.index))
				indices.add(index);

			return new NodeList(indices);
		}

		@Override
		public Collection<? extends KNode> in(Integer label)
		{
			check();

			KNode other = new KNode(label);
			if(other.connectedTo(this))
				return Arrays.asList(other);
			
			return null;
		}


		@Override
		/**
		 * Tag 0 is assumed
		 * @param to
		 * @return
		 */
		public KLink connect(Node<Integer> to)
		{
			return connect((TNode<Integer,Integer>) to, 0);
		}	

		@Override
		public KLink connect(TNode<Integer, Integer> other, Integer tag) 
		{
			assert(tag != null);
			if(tag < 0)
				throw new IllegalArgumentException("Negative tags not allowed. Input was " + tag);
			
			if(tag > maxTag + 1)
				throw new IllegalArgumentException("Tag ("+tag+") too large. Tags must be contigous integers, the largest tag allowd is one larger than the largest tag currently in the graph ("+maxTag+")");
				
			maxTag = Math.max(tag, maxTag);

			
			check();
			int fromIndex = index, toIndex = other.index();

			out.get(fromIndex).add(toIndex);
			in.get(toIndex).add(fromIndex);

			outTags.get(fromIndex).add(tag);
			inTags.get(toIndex).add(tag);
			

			modCount++;			
			numLinks++;

			sorted = false;
			
			int indexInOut = out.get(fromIndex).size()-1;

			return new KLink(fromIndex, toIndex, outTags.get(fromIndex).get(indexInOut));
		}

		@Override
		public void disconnect(Node<Integer> other)
		{
			check();
			
			if(other.graph() != this.graph())
				throw new IllegalArgumentException("Argument node belongs to diffewrent graph.");
			
			int mine = index, his = other.index();
			int links = 0;

			List<Integer> nb = out.get(mine);
			List<Integer> nbT = outTags.get(mine);

			for (int i : series(nb.size() - 1, -1)) {
				if (nb.get(i) == his) {
					nbT.remove(i);
					nb.remove(i);
					links++;
				}
			}

			nb = out.get(his);
			nbT = outTags.get(his);

			for (int i : series(nb.size() - 1, -1)) {
				if (nb.get(i) == mine) {
					nbT.remove(i);
					nb.remove(i);
					links++;
				}
			}

			nb = in.get(mine);
			nbT = inTags.get(mine);

			for (int i : series(nb.size() - 1, -1)) {
				if (nb.get(i) == his) {
					nbT.remove(i);
					nb.remove(i);
				}
			}

			nb = in.get(his);
			nbT = inTags.get(his);

			for (int i : series(nb.size() - 1, -1)) {
				if (nb.get(i) == mine) {
					nbT.remove(i);
					nb.remove(i);
				}
			}

			numLinks -= links;			
			modCount++;

			sorted = false;
		}

		@Override
		public boolean connected(Node<Integer> other)
		{
			if(other.graph()  != this.graph())
				return false;

			KNode o = (KNode) other;

			return this.connectedTo(o) || o.connectedTo(this);
		}

		@Override
		public boolean connected(TNode<Integer, Integer> other, Integer tag) 
		{
			if(other.graph()  != this.graph())
				return false;

			KNode o = (KNode) other;

			return this.connectedTo(o, tag) || o.connectedTo(this, tag);
		}

		@Override
		public boolean connectedTo(DNode<Integer> to)
		{
			if(to.graph()  != this.graph())
				return false;
			
			int mine = index, his = to.index();

			if(out.get(mine).contains(his))
				return true;

			return false;
		}

		@Override
		public boolean connectedTo(TNode<Integer, Integer> other, Integer tag) 
		{
			if(other.graph()  != this.graph())
				return false;
			
			int mine = index;
			Integer his = other.index();

			List<Integer> nb = out.get(mine);
			List<Integer> nbT = outTags.get(mine);

			for (int i : series(nb.size())) 
				if (nb.get(i) == his && nbT.get(i) == tag) 
					return true;
			
			return false;
		}

		@Override
		public KGraph graph()
		{
			check();
			return KGraph.this;
		}

		@Override
		public int index()
		{
			check();
			return index;
		}

		@Override
		public int inDegree()
		{
			check();
			return in.get(index).size();
		}

		@Override
		public int outDegree()
		{
			check();
			return out.get(index).size();
		}

		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + ((index == null) ? 0 : index.hashCode());
			return result;
		}

		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KNode other = (KNode) obj;
			if (graph() != other.graph()) // They don't come from the same graph object (checking for graph equality here is really, really, really slow)
				return false;
			if (dead != other.dead)
				return false;
			if (index == null)
			{
				if (other.index != null)
					return false;
			} else if (!index.equals(other.index))
				return false;
			return true;
		}

		public String toString()
		{
			return "n" + index();
		}

		@Override
		public List<KLink> links()
		{
			List<KLink> list = new ArrayList<KLink>(degree());
			
			List<Integer> neighbors = out.get((int) index);
			List<Integer> tags = outTags.get((int) index);

			for(int i : series(neighbors.size()))
				list.add(new KLink(index, neighbors.get(i), tags.get(index)));

			List<Integer> nb = in.get((int) index);
			for(int i = 0; i < nb.size(); i++)
				if((int) nb.get(i) != (int) index) // No double reflexive links
					list.add(new KLink((int) nb.get(i), index, i));	

			return list;
		}

		@Override
		public List<KLink> linksOut()
		{
			List<KLink> list = new ArrayList<KLink>(outDegree());
			
			List<Integer> nb = out.get(index);
			List<Integer> tags = outTags.get(index);
			
			for(int i = 0; i < nb.size(); i++)
				list.add(new KLink(index, nb.get(i), tags.get(i)));

			return list;
		}

		@Override
		public List<KLink> linksIn()
		{
			List<KLink> list = new ArrayList<KLink>(inDegree());
			
			List<Integer> nb = in.get(index);
			List<Integer> tags = inTags.get(index);
			
			for(int i = 0; i < nb.size(); i++)
				list.add(new KLink(index, nb.get(i), tags.get(i)));

			return list;
		}

		@Override
		public Collection<KLink> links(Node<Integer> other)
		{
			List<KLink> list = new ArrayList<KLink>(degree());

			int o = other.index();
			
			List<Integer> nb = out.get(index);
			List<Integer> tags = outTags.get(index);

			for (int i = 0; i < nb.size(); i++) 
				if (nb.get(i) == o) 
					list.add(new KLink(index, nb.get(i), tags.get(i)));

			nb = in.get(index);
			tags = in.get(index);

			for (int i = 0; i < nb.size(); i++) 
				if (nb.get(i) != index && nb.get(i) == o) 
					list.add(new KLink(nb.get(i), index, tags.get(i)));
			
			return list;
		}

		@Override
		public Collection<KLink> linksOut(DNode<Integer> other)
		{
			List<KLink> list = new ArrayList<KLink>(outDegree());

			int o = other.index();
			List<Integer> nb = out.get(index);
			List<Integer> tags = outTags.get(index);

			for (int i = 0; i < nb.size(); i++) 
				if (nb.get(i) == o) 
					list.add(new KLink(index, nb.get(i), tags.get(i)));

			return list;
		}

		@Override
		public Collection<KLink> linksIn(DNode<Integer> other)
		{
			check();
			List<KLink> list = new ArrayList<KLink>(inDegree());

			int o = other.index();
			List<Integer> nb   = in.get(index);
			List<Integer> tags = inTags.get(index);

			for (int i = 0; i < nb.size(); i++) 
				if ((int) nb.get(i) == o) 
					list.add(new KLink((int) nb.get(i), index, tags.get(i)));

			return list;
		}



		@Override
		public TLink<Integer, Integer> link(TNode<Integer, Integer> other) 
		{
			check();
			if(other.graph() != this.graph())
				throw new IllegalArgumentException("Argument node not from the same graph.");
			
			Collection<? extends KLink> col = links((DNode)other);
			if(col.isEmpty())
				return null;
			return col.iterator().next();
		}

		@Override
		public Collection<Integer> tags() 
		{
			List<Integer> tags = new ArrayList<Integer>(degree());

			tags.addAll(outTags.get((int) index));
			tags.addAll(inTags.get((int) index));

			return tags;
		}

		@Override
		public Collection<KNode> toTag(Integer tag) 
		{
			List<KNode> list = new ArrayList<KNode>();
			
			List<Integer> nb = out.get((int) index);
			List<Integer> tags = outTags.get((int) index);

			for (int i : series(nb.size())) {
				if (tags.get(i) == tag) {
					list.add(new KNode(nb.get(i)));
				}
			}
			return list;
		}

		@Override
		public Collection<KNode> fromTag(Integer tag) 
		{
			List<KNode> list = new ArrayList<KNode>();
			
			List<Integer> nb   = in.get((int) index);
			List<Integer> tags = inTags.get((int) index);

			for (int i : series(nb.size())) {
				if (tags.get(i) == tag) {
					list.add(new KNode(nb.get(i)));
				}
			}
			
			return list;
		}
	}
	
	public class KLink implements DTLink<Integer,Integer>
	{
		private KNode from, to;
		private int tag;

		private long nodeModState = nodeModCount;
		private boolean dead = false;

		public KLink(int from, int to, int tag)
		{
			this.from = new KNode(from);
			this.to = new KNode(to);
			this.tag = tag;
		}

		private void check()
		{
			if(dead)
				throw new IllegalStateException("Link object is dead");

			if(nodeModCount != nodeModState)
				throw new IllegalStateException("Graph was modified since node creation.");
		}		

		@Override
		public Collection<? extends KNode> nodes()
		{
			check();
			return Arrays.asList(from, to);
		}

		@Override
		public KGraph graph()
		{
			check();
			return KGraph.this;
		}

		/**
		 * 
		 */
		@Override
		public void remove()
		{
			check();

			int index = out.get(from.index()).indexOf(to.index());
			if(index < 0)
				throw new IllegalStateException("Illegal state. Live link object not found in graph.");
			
			out.get(from.index()).remove(index);
			outTags.get(from.index()).remove(index);
			
			index = in.get(to.index()).indexOf(from.index());
			if(index < 0)
				throw new IllegalStateException("Illegal state. Live link object not found in graph.");
			
			in.get(to.index()).remove(index);
			inTags.get(to.index()).remove(index);

			numLinks --;
			
			modCount++;
			dead = true;

			sorted = false;
		}

		@Override
		public boolean dead()
		{
			return dead;
		}

		@Override
		public KNode first()
		{
			check();
			return from;
		}

		@Override
		public KNode second()
		{
			check();
			return to;
		}


		@Override public int hashCode() 
		{ 
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (dead ? 1231 : 1237);
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
			result = prime * result + tag;
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KLink other = (KLink) obj;				
			if (getOuterType() != other.getOuterType()) // They don't come from the same graph object (checking for graph equality here is really, really, really slow)
				return false;
			if (dead != other.dead)
				return false;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null)	{
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			if (tag != other.tag) {
				return false;
			}
			return true;
		}
		

		private KGraph getOuterType()
		{
			return KGraph.this;
		}

		public String toString()
		{
			check();
			return from + " -> " + to + "[label=" + tag + "]";
		}

		@Override
		public KNode from()
		{
			return from;
		}

		@Override
		public KNode to()
		{
			return to;
		}

		@Override
		public KNode other(Node<Integer> current)
		{
			if(first().index() != current.index())
				return first();
			return second();
		}

		@Override
		public Integer tag() 
		{
			check();
			return tag;
		}
	}
	
	private class NodeList extends AbstractList<KNode>
	{
		private List<Integer> indices;

		public NodeList(List<Integer> indices)
		{
			this.indices = indices;
		}

		@Override
		public KNode get(int index)
		{
			return new KNode(indices.get(index));
		}

		@Override
		public int size()
		{
			return indices.size();
		}
	}

	
	private class SeriesSet extends AbstractSet<Integer>
	{
		int size;
		
		public SeriesSet(int size)
		{
			this.size = size;
		}
		
		@Override
		public Iterator<Integer> iterator()
		{
			return Series.series(size).iterator();
		}

		@Override
		public int size()
		{
			return size;
		}

		@Override
		public boolean contains(Object o)
		{
			if (!(o instanceof Integer))
				return false;
					
			int i = (Integer) o;
			return i >= 0 && i < size;
		}
	};
	
	public List<KNode> nodes(List<Integer> indices)
	{
		return new NodeList(indices);
	}

	@Override
	public List<KNode> neighborsFast(Node<Integer> node)
	{
		if(node.graph() != this)
			throw new IllegalArgumentException("Cannot call with node from another graph.");
		
		int index = node.index();
		
		List<Integer> indices = concat(in.get(index), out.get(index));
		
		return new NodeList(indices);
	}

	public List<KNode> neighborsFastIn(Node<Integer> node)
	{
		return new NodeList(in.get(node.index()));
	}
			
	public List<KNode> neighborsFastOut(Node<Integer> node)
	{
		return new NodeList(out.get(node.index()));
	}

	
	private boolean eq(Object a, Object b)
	{
		if(a == null && b == null)
			return true;
		
		if(a == null || b == null)
			return false;
		
		return a.equals(b);
	}
	
	/**
	 * Returns a representation of the graph in Dot language format.
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("digraph {");
		
		Set<KNode> nodes = new HashSet<KNode>(nodes());
		
		boolean first = true;
		for(KLink link : links())
		{
			if(first) 
				first = false;
			else 
				sb.append("; ");
			
			sb.append(link);
			
			nodes.remove(link.first());
			nodes.remove(link.second());
		}
		
		for(KNode node : nodes)
			sb.append("; " + node);
		
		sb.append("}");
		
		return sb.toString();
	}
	
	/**
	 * Resets all neighbour list to their current capacity, plus the 
	 * given margin. 
	 * 
	 * @param margin
	 */
	public void compact(int margin)
	{
		for(int i : Series.series(in.size()))
		{
			List<Integer> old = in.get(i);
			List<Integer> nw = new ArrayList<Integer>(old.size() + margin);
			nw.addAll(old);
			
			in.set(i, nw);
		}
		
		for(int i : Series.series(out.size()))
		{
			List<Integer> old = out.get(i);
			List<Integer> nw = new ArrayList<Integer>(old.size() + margin);
			nw.addAll(old);
			
			out.set(i, nw);
		}
	}
	
	/**
	 * Sorts all neighbour lists
	 * 
	 * @param margin
	 */
	public void sort()
	{
		if(sorted)
			return;
		
		for(int i : Series.series(in.size()))
			Collections.sort(in.get(i));
		
		for(int i : Series.series(out.size()))
			Collections.sort(out.get(i));
		
		
		
		sorted = true;
	}
	
	/**
	 * Creates a copy of the given graph as a LightDGraph object. 
	 * 
	 * If the argument is undirectional, the link direction will be arbitrary.
	 * 
	 * @param graph
	 * @return
	 */
	public static <L> LightDGraph<L> copy(Graph<L> graph)
	{
		LightDGraph<L> copy = new LightDGraph<L>(graph.size());
		for(Node<L> node : graph.nodes())
			copy.add(node.label());
		
		for(Link<L> link : graph.links())
			copy.get(link.first().index()).connect(copy.get(link.second().index()));
		
		copy.compact(0);
		
		return copy;
	}
	
	@Override 
	public int hashCode()
	{
		if(hashMod != null && hashMod == modCount)
			return hash;
		
		hash = 1;
		sort();
		

		hash = 31 * hash + size();
		
		return hash;
	}	
	
	public boolean equals(Object other)
	{	
		if(!(other instanceof DTGraph<?,?>))
			return false;
		
		@SuppressWarnings("unchecked")
		DTGraph<Object, Object> otherGraph = (DTGraph<Object, Object>) other;
		
		if(! otherGraph.level().equals(level()))
			return false;
		
		if(size() != otherGraph.size())
			return false;
		
		if(numLinks() != otherGraph.numLinks())
			return false;
		
		if(labels().size() != otherGraph.labels().size())
			return false;
		
		// * for all connected nodes
		for(KNode node : nodes())
		{
			if(! Functions.equals(node.label(), otherGraph.get(node.index()).label()))
				return false;
			
			for(KNode neighbor : node.neighbors())
			{
				Collection<KLink> links = node.linksOut(neighbor);
				Collection<? extends DTLink<Object, Object>> otherLinks = 
						otherGraph.get(node.index())
							.linksOut(otherGraph.get(neighbor.index()));

				if(links.size() != otherLinks.size())
					return false;
				
				if(links.size() == 1)
				{
					// ** If there is only one link, check that there is a single 
					//    similar link in the other graph and that it has the same tag
					Integer tag = links.iterator().next().tag();
					Object otherTag = otherLinks.iterator().next().tag();
					
					if(! Functions.equals(tag, otherTag))
						return false;
				} else 
				{
					// ** If there are multiple links between these two nodes,
					//    count the occurrences of each tag and check that the 
					//    frequencies match between graphs
					FrequencyModel<Integer> 
							model = new FrequencyModel<Integer>();
					FrequencyModel<Object>
							otherModel = new FrequencyModel<Object>();
					
					for(KLink link : links)
						model.add(link.tag());
					
					for(DTLink<Object, Object> otherLink : otherLinks)
						otherModel.add(otherLink.tag());
					
					if(! model.tokens().equals(otherModel.tokens()))
						return false;
					
					for(Integer token : model.tokens())
						if(otherModel.frequency(token) != model.frequency(token))
							return false;
				}
			}
		}
		
		return true;	
	}
	
	/**
	 * Loads the given HDT file into a KGraph. 
	 * @param file
	 * @param nodes
	 * @param relations
	 * @return
	 */
	public static KGraph loadHDT(File file)
			throws IOException
	{
		return loadHDT(file, new ArrayList<String>(), new ArrayList<String>());
		
	}
	
	/**
	 * @param file HDT file, or gzipped HDT file
	 * @param nodes
	 * @param relations
	 * @return
	 * @throws IOException
	 */
	public static KGraph loadHDT(File file, List<String> nodes, List<String> relations) 
			throws IOException
	{
		int nextTag = 0; 
		
		Map<String, Integer> nodeMap = new HashMap<>(), relationMap = new HashMap<>();
		KGraph graph = new KGraph();
		
		HDT hdt = HDTManager.loadIndexedHDT(
				file.getName().endsWith(".gz")?
			    new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))) :
				new BufferedInputStream(new FileInputStream(file)), null);
	
		nodes.clear();
		relations.clear();
    
    	try {
    		IteratorTripleString it = hdt.search("", "", "");
    
    		int total = (int)it.estimatedNumResults(), i = 0;
    
        	while(it.hasNext()) 
        	{
        		TripleString ts = it.next();
        
        		String subject = ts.getSubject().toString(),
        			   predicate = ts.getPredicate().toString(),
        		       object = ts.getObject().toString();
        		
        		Integer node1 = nodeMap.get(subject);
        		if(node1 == null)
        		{
        			node1 = graph.add().index();
        			nodeMap.put(subject, node1);
        			nodes.add(subject);
        		}
        		
        		Integer node2 = nodeMap.get(object);
        		if(node2 == null)
        		{
        			node2 = graph.add().index();
        			nodeMap.put(object, node2);
        			nodes.add(object);
        		}
        		
        		Integer tag = relationMap.get(predicate);
        		if(tag == null)
        		{
        			tag = nextTag;
        			nextTag ++;
        			
        			relationMap.put(predicate, tag);
        			relations.add(predicate);
        		}
        		    							
        		graph.get(node1).connect(graph.get(node2), tag);
        		
        		// dot(i++, total);
        	}
    	} catch (NotFoundException e)
    	{
    		// Empty graph
    	} finally
    	{
    		hdt.close();
    	}

		return graph;
	}	
	
	/**
	 * Can't get this to work just yet. Should use the HDT dictionaries.
	 * 
	 * @param file
	 * @param nodes
	 * @param relations
	 * @return
	 * @throws IOException
	 */
	public static KGraph loadHDTBuggy(File file, List<String> nodes, List<String> relations) 
			throws IOException
	{
		int nextTag = 0; 
		
		Map<String, Integer> nodeMap = new HashMap<>(), relationMap = new HashMap<>();
		KGraph graph = new KGraph();
		
		HDT hdt = HDTManager.loadIndexedHDT(
				new BufferedInputStream(new FileInputStream(file)), null);
		
		LinkedHashSet<String> nodeSet = new LinkedHashSet<String>();
		LinkedHashSet<String> relSet = new LinkedHashSet<String>();

		try {
			
			// * First pass, get all node and predicate labels
			IteratorTripleString it = hdt.search("", "", "");
			
			while(it.hasNext())
			{
				TripleString ts = it.next();
				
				String subject   = ts.getSubject().toString(),
				       predicate = ts.getPredicate().toString(),
 				       object    = ts.getObject().toString();
				
				nodeSet.add(subject);
				nodeSet.add(object);
				relSet.add(predicate);
			}
		} catch (NotFoundException e) 
		{
			// File must be empty, return empty graph
		} finally 
		{
			// IMPORTANT: Free resources
			hdt.close();
		}

		nodes.clear();
		nodes.addAll(nodeSet);
		for(int i : series(nodes.size()))
			nodeMap.put(nodes.get(i), i);
		
		System.out.println(nodes.size());
			
		relations.clear();
		relations.addAll(relSet);
		for(int i : series(relations.size()))
			relationMap.put(relations.get(i), i);
		
		Global.log().info("Labels read from dictionary");
		
		for(int i : series(nodes.size()))
			graph.add();
		System.out.println("... " + graph.nodes().size());
				
		
		// * Second pass, construct graph
		for(KNode node : graph.nodes())
		{			
			Functions.dot(node.index(), graph.size());
			
			String subject = nodes.get(node.index());
			
			IteratorTripleString it;
			try {
				it = hdt.search(subject, "", "");
			} catch (NotFoundException e)
			{
				continue;
			}
			
			System.out.println(it.estimatedNumResults());
			
			while(it.hasNext()) 
			{
				TripleString ts = it.next();

				String predicate = ts.getPredicate().toString(),
				       object = ts.getObject().toString();
				    				
				Integer objIndex = nodeMap.get(object);
				if(objIndex == null)
					System.out.println("object not found in map: " + object);
				
				KNode node2 = graph.get(objIndex);
				int tag = relationMap.get(predicate);
								
				node.connect(node2, tag);
			}

		}

		return graph;
	}
	
	public static List<List<Integer>> degrees(DTGraph<Integer, Integer> graph)
	{
		List<Integer> inDegrees = new ArrayList<>(graph.size());
		List<Integer> outDegrees = new ArrayList<>(graph.size());
		
		List<Integer> tags = new ArrayList<>(graph.tags());
		List<Integer> tagDegrees = new ArrayList<>(tags.size());

		Map<Integer, Integer> tag2Index = new LinkedHashMap<>();
		for(int i : Series.series(tags.size()))
			tag2Index.put(tags.get(i), i);
		
		for(int i : series(graph.size()))
			inDegrees.add(0);
		for(int i : series(graph.size()))
			outDegrees.add(0);
		for(int i : series(tag2Index.size()))
			tagDegrees.add(0);
		
		for(DTLink<Integer, Integer> link : graph.links())
		{
			inc(link.from().index(), outDegrees);
			inc(link.to().index(), inDegrees);
			inc(tag2Index.get(link.tag()), tagDegrees);
		}
		
		return Arrays.asList(inDegrees, outDegrees, tagDegrees);
	}
	
	public static List<List<Integer>> degrees(KGraph graph)
	{
		List<Integer> in = new ArrayList<>(graph.size());
		List<Integer> out = new ArrayList<>(graph.size());
		List<Integer> tag = new ArrayList<>(graph.tags().size());
		
		for(int i : series(graph.size()))
			in.add(0);
		for(int i : series(graph.size()))
			out.add(0);
		for(int i : series(graph.tags().size()))
			tag.add(0);
		
		for(DTLink<Integer, Integer> link : graph.links())
		{
			inc(link.from().index(), out);
			inc(link.to().index(), in);
			inc(link.tag(), tag);
		}
		
		return Arrays.asList(in, out, tag);
	}
	
	private static void inc(int i, List<Integer> list)
	{
		list.set(i, list.get(i) + 1);
	}
	
	/**
	 * Turn a pattern for a KGraph into a string-based pattern. PNonegative labels 
	 * and tags are replaced by their original IRI, negative ones are replaced 
	 * by "?ni" for nodes, and "?pi" for predicates (with i the index of the variable). 
	 * 
	 * @return
	 */
	public static DTGraph<String, String> recover(DTGraph<Integer, Integer> pattern, List<String> nodes, List<String> tags)
	{
		DTGraph<String, String> result = new MapDTGraph<>();
		
		for(DTNode<Integer, Integer> node : pattern.nodes())
		{
			String label = node.label() < 0 ? "?n" + (- node.label()) : nodes.get(node.label());
			result.add(label);
		}
		
		for(DTLink<Integer, Integer> link : pattern.links())
		{
			int f = link.from().index(), t = link.to().index();
			String tag = link.tag() < 0 ? "?p" + (-link.tag()) : tags.get(link.tag());
			
			result.get(f).connect(result.get(t), tag);
		}
			
		return result;
	}
	
	/**
	 * 
	 * @param pattern
	 * @return
	 */
	public static String bgp(DTGraph<String, String> pattern)
	{
		StringBuilder builder = new StringBuilder();
		
		for(DTLink<String, String> link : pattern.links())
			builder.append(shorten(link.from().label()) + " " + shorten(link.tag()) + " " + shorten(link.to().label()) + ". ");

		return builder.toString();
	}
	
	/**
	 * @param values
	 * @param numVars
	 * @param nodes
	 * @param tags
	 * @return
	 */
	public static List<String> recover(List<Integer> values,  List<String> map)
	{
		List<String> result = new ArrayList<>(values.size());
		
		for(int value : values)
			result.add(map.get(value));
		
		return result;
	}
}
