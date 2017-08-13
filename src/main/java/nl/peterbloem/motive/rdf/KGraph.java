package nl.peterbloem.motive.rdf;

import static nl.peterbloem.kit.Functions.concat;
import static nl.peterbloem.kit.Series.series;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.nodes.DLink;
import org.nodes.DNode;
import org.nodes.DTGraph;
import org.nodes.DTLink;
import org.nodes.DTNode;
import org.nodes.FastWalkable;
import org.nodes.LightDTGraph;
import org.nodes.Node;
import org.nodes.TLink;
import org.nodes.TNode;

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
		out = new ArrayList<List<Integer>>(capacity);
		in = new ArrayList<List<Integer>>(capacity); 

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

	public DTNode<Integer, Integer> node(Integer label)
	{
		if(label < 0 || label >= size()) 
			return null;
		
		return new KNode(label);
	}

	public Collection<? extends DTNode<Integer, Integer>> nodes(Integer label)
	{
		return Arrays.asList(node(label));
	}

	public List<? extends DTNode<Integer, Integer>> nodes()
	{
		return new NodeList(Series.series(size()));
	}

	public DTNode<Integer, Integer> get(int i)
	{
		return node(i);
	}

	public Iterable<? extends DTLink<Integer, Integer>> links()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public DTNode<Integer, Integer> add(Integer label)
	{
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

	public Class<? extends DTGraph<?, ?>> level()
	{
		// TODO Auto-generated method stub
		return null;
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
		
			for (int i : series(in.size())) 
			{
				List<Integer> neighbors = in.get(i);
				List<Integer> tags      = inTags.get(i);
				
				assert(neighbors.size() == inTags.size());
								
				// walk backwards through the list for safe removal
				for (int j : series(neighbors.size()-1, -1)) 
					if (neighbors.get(j).equals(this.index))
					{
						neighbors.remove((int) j);
						tags.remove((int) j);
						numLinks --;
					}

				assert(neighbors.size() == inTags.size());

			}

			for (int i : series(out.size())) 
			{
				List<Integer> neighbors = out.get(i);
				List<Integer> tags      = outTags.get(i);
				
				for (int j : series(neighbors.size())) 
					if (neighbors.get(j).equals(this.index)) 
					{
						neighbors.remove((int) j);
						tags.remove((int) j);	
					}
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
			List<Integer> indices = new ArrayList<Integer>(degree());

			for(int i : in.get(this.index))
				indices.add(i);
			for(int i : out.get(this.index))
				indices.add(i);

			return new NodeList(indices);
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
			return KGraph.this;
		}

		@Override
		public int index()
		{
			return index;
		}

		@Override
		public int inDegree()
		{
			return in.get(index).size();
		}

		@Override
		public int outDegree()
		{
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
		public Collection<? extends KLink> links(Node<Integer> other)
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
		public Collection<? extends KLink> linksOut(DNode<Integer> other)
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
		public Collection<? extends KLink> linksIn(DNode<Integer> other)
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
		public Collection<Integer> tags() {
			List<Integer> tags = new ArrayList<Integer>(degree());

			tags.addAll(outTags.get((int) index));
			tags.addAll(inTags.get((int) index));

			return tags;
		}

		@Override
		public Collection<? extends DTNode<Integer, Integer>> toTag(Integer tag) 
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
		public Collection<? extends KNode> fromTag(Integer tag) 
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
	
	private class KLink implements DTLink<Integer,Integer>
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
			
			in.get(from.index()).remove(index);
			inTags.get(from.index()).remove(index);

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

	@Override
	public List<KNode> neighborsFast(Node<Integer> node)
	{
		if(node.graph() != this)
			throw new IllegalArgumentException("Cannot call with node from another graph.");
		
		int index = node.index();
		
		List<Integer> indices = concat(in.get(index), out.get(index));
		
		return new NodeList(indices);
	}
}
