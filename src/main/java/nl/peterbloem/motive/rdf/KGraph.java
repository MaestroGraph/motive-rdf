package nl.peterbloem.motive.rdf;

import static java.lang.Math.max;
import static java.util.Collections.emptySet;
import static java.util.Collections.reverseOrder;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static nl.peterbloem.kit.Functions.concat;
import static nl.peterbloem.kit.Functions.dot;
import static nl.peterbloem.kit.Pair.p;
import static nl.peterbloem.kit.Series.series;
import static nl.peterbloem.motive.rdf.Pref.shorten;
import static nl.peterbloem.motive.rdf.Triple.t;

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
 * A simplified object representing a knowledge graph. Node labels are their 
 * indices.
 * 
 * NOTES:
 * <ul>
 * <li>
 * Removing a node will cause all labels (above it) to change. Removing a 
 * node or link may cause tags to change (that is, labels are always kept 
 * contiguous). This makes removals potentially very costly.</li>
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
		DTGraph<Integer, Integer>
{
	// The graph is stored as seven indices. 
	// * no variables
	private Set<Triple> triples = new LinkedHashSet<>();
	
	// * two variables. sIndex maps an s integer to all triples with s at the s position
	private Map<Integer, Set<Triple>> sIndex = new LinkedHashMap<>();
	private Map<Integer, Set<Triple>> pIndex = new LinkedHashMap<>();
	private Map<Integer, Set<Triple>> oIndex = new LinkedHashMap<>();		
	
	// * one variable
	private Map<Pair<Integer, Integer>, Set<Triple>> spIndex = new LinkedHashMap<>();
	private Map<Pair<Integer, Integer>, Set<Triple>> soIndex = new LinkedHashMap<>();
	private Map<Pair<Integer, Integer>, Set<Triple>> poIndex = new LinkedHashMap<>();
	
	private int maxNode = -1;
	private int maxTag = -1;

	private long modCount = 0;	
	
	// * changes for any edit which causes the node indices to change 
	//   (currently just removal). If this happens, all existing Node and Link 
	//   objects lose persistence 
	//   (currently not used, because modificiations aren't implemented yet)
	private long nodeModCount = 0;

	private int hash;
	private Long hashMod = null;
	
	/**
	 * 
	 * @param trips
	 */
	public KGraph(Collection<Triple> triplesIn)
	{
		for(Triple triple : triplesIn)
		{
			int s = triple.subject(), p = triple.predicate(), o = triple.object();
			
			triples.add(triple);
			
			put(sIndex, s, triple);			
			put(pIndex, p, triple);
			put(oIndex, o, triple);

			put(spIndex, p(s, p), triple);			
			put(soIndex, p(s, o), triple);
			put(poIndex, p(p, o), triple);
			
			maxTag  = max(maxTag, p);
			maxNode = max(maxNode, max(s, o));
		}
	}
	
	/**
	 * Find all matches of the given triple pattern. null arguments are taken as variables
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 */
	public Set<Triple> find(Integer subject, Integer predicate, Integer object)
	{
		if(subject == null)
		{
			if(predicate == null)
			{
				if(object == null)
					return unmodifiableSet(triples);
				else
					return unmodifiableSet(get(oIndex, object));
			} else 
			{
				if(object == null)
					return unmodifiableSet(get(pIndex, predicate));				
				else
					return unmodifiableSet(get(poIndex, p(predicate, object)));				
			}
		} else
		{
			if(predicate == null)
			{
				if(object == null)
					return unmodifiableSet(get(sIndex, subject));
				else
					return unmodifiableSet(get(soIndex, p(subject, object)));
			} else 
			{
				if(object == null)
					return unmodifiableSet(get(spIndex, p(subject, predicate)));				
				else
				{
					Triple t = t(subject, predicate, object);
					return triples.contains(t) ? singleton(t) : emptySet();
				}
			}
		}
	}
	
	private int numNull(Object... objects)
	{
		int res = 0;
		for(Object obj : objects)
			if(obj == null)
				res++;
		
		return res;
	}
	
	private <T> void put(Map<T, Set<Triple>> index, T key, Triple triple)
	{
		if(!index.containsKey(key))
			index.put(key, new LinkedHashSet<>());
		
		index.get(key).add(triple);
	}
	
	private <T> Set<Triple> get(Map<T, Set<Triple>> index, T key)
	{
		if(!index.containsKey(key))
			return Collections.emptySet();
		
		return index.get(key);
	}


	public int size()
	{
		return maxNode + 1;
	}

	public long numLinks()
	{
		return triples.size();
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

	public Collection<KLink> links()
	{
		return new LinkCollection(triples);		
	}
	
	public Collection<KLink> links(Collection<Triple> trips)
	{
		return new LinkCollection(trips);		
	}
	
	private class LinkCollection extends AbstractCollection<KLink>
	{
		Collection<Triple> trips;
		
		public LinkCollection(Collection<Triple> trips)
		{
			this.trips = trips;
		}
		
		@Override
		public Iterator<KLink> iterator()
		{
			return new LLIterator();
		}

		@Override
		public int size()
		{
			return trips.size();
		}

		private class LLIterator implements Iterator<KLink>
		{
			Iterator<Triple> master = trips.iterator();

			@Override
			public boolean hasNext()
			{
				return master.hasNext();
			}

			@Override
			public KLink next()
			{
				return new KLink(master.next());
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("Method not supported");
			}

		}	
	}

	/**
	 * Add a node. It immediately receives the next available integer label 
	 * @return
	 */
	public KNode add()
	{
		throw new UnsupportedOperationException("Graph modification is not supported.");

//		return add(size());
	}
	
	/**
	 * This method is only present for 
	 */
	public KNode add(Integer label)
	{			
		throw new UnsupportedOperationException("Graph modification is not supported.");
//
//		if(label != size())
//			throw new IllegalArgumentException("Can only add node with the next available integer as a label (ie. size()). Consider using add() instead.");
//
//		maxNode ++;
//
//		return new KNode(size() - 1);
	}

	public Set<Integer> labels()
	{
		return new SeriesSet(size());
	}

	public boolean connected(Integer from, Integer to)
	{
		return ! pIndex.get(p(from, to)).isEmpty();
	}

	public long state()
	{
		return 0; // TODO
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
		// The modCount of the graph for which this node object is safe to use
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
			throw new UnsupportedOperationException("Graph modification is not supported.");
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
			
			for(Triple triple : find(index, null, null))
				indices.add(triple.object());
			for(Triple triple : find(null, null, index))
				indices.add(triple.subject());

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

			for(Triple triple : find(index, null, null))
				indices.add(triple.object());

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

			for(Triple triple : find(null, null, index))
				indices.add(triple.subject());

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
			throw new UnsupportedOperationException("Graph modification is not supported.");
		}	

		@Override
		public KLink connect(TNode<Integer, Integer> other, Integer tag) 
		{
			throw new UnsupportedOperationException("Graph modification is not supported.");
		}

		@Override
		public void disconnect(Node<Integer> other)
		{			
			throw new UnsupportedOperationException("Graph modification is not supported.");
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
			
			Set<Triple> triples = find(mine, null, his);
			
			return ! triples.isEmpty();
		}

		@Override
		public boolean connectedTo(TNode<Integer, Integer> other, Integer tag) 
		{
			if(other.graph()  != this.graph())
				return false;
			
			int mine = index, his = other.index();

			return triples.contains(t(mine, tag, his));
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
			return find(null, null, index).size();
		}

		@Override
		public int outDegree()
		{
			check();
			return find(index, null, null).size();
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
			check();
			Set<Triple> triples = new LinkedHashSet<>();
			
			triples.addAll(find(index, null, null));
			triples.addAll(find(null, null, index));
			
			List<KLink> result = new ArrayList<>(triples.size());
			for(Triple triple : triples)
				result.add(new KLink(triple));

			return result;
		}

		@Override
		public List<KLink> linksOut()
		{
			check();
			return new ArrayList<KLink>(new LinkCollection(find(index, null, null)));
		}

		@Override
		public List<KLink> linksIn()
		{
			check();
			return new ArrayList<KLink>(new LinkCollection(find(null, null, index)));
		}

		@Override
		public Collection<KLink> links(Node<Integer> other)
		{
			check();
			Set<Triple> triples = new LinkedHashSet<>();

			
			triples.addAll(find(index, null, other.index()));
			triples.addAll(find(other.index(), null, index));
			
			List<KLink> result = new ArrayList<>(triples.size());
			for(Triple triple : triples)
				result.add(new KLink(triple));

			return result;
		}

		@Override
		public Collection<KLink> linksOut(DNode<Integer> other)
		{
			check();
			return new ArrayList<KLink>(
					new LinkCollection(find(index, null, other.index()))
			);
		}

		@Override
		public Collection<KLink> linksIn(DNode<Integer> other)
		{			
			check();
			return new ArrayList<KLink>(
					new LinkCollection(find(other.index(), null, index))
			);
		}

		@Override
		public TLink<Integer, Integer> link(TNode<Integer, Integer> other) 
		{
			check();
			if(other.graph() != this.graph())
				throw new IllegalArgumentException("Argument 'node' not from the same graph.");
			
			Collection<? extends KLink> col = links((DNode)other);
			if(col.isEmpty())
				return null;
			return col.iterator().next();
		}

		@Override
		public Collection<Integer> tags() 
		{
			Set<Integer> tags = new LinkedHashSet<>();

			for(Triple triple : find(index, null, null))
				tags.add(triple.predicate());
			for(Triple triple : find(null, null, index))
				tags.add(triple.predicate());			
			
			return tags;
		}

		@Override
		public Collection<KNode> toTag(Integer tag) 
		{
			check();
			Set<Integer> indices = new LinkedHashSet<Integer>();
			
			for(Triple triple : find(index, tag, null))
				indices.add(triple.object());
		
			return new NodeList(new ArrayList<>(indices));
		}

		@Override
		public Collection<KNode> fromTag(Integer tag) 
		{
			check();
			Set<Integer> indices = new LinkedHashSet<Integer>();
			
			for(Triple triple : find(null, tag, index))
				indices.add(triple.subject());
		
			return new NodeList(new ArrayList<>(indices));
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

		public KLink(Triple t)
		{
			this(t.subject(), t.object(), t.predicate());
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
			throw new UnsupportedOperationException("Graph modification is not supported.");
//			
//			check();
//
//			int index = out.get(from.index()).indexOf(to.index());
//			if(index < 0)
//				throw new IllegalStateException("Illegal state. Live link object not found in graph.");
//			
//			out.get(from.index()).remove(index);
//			outTags.get(from.index()).remove(index);
//			
//			index = in.get(to.index()).indexOf(from.index());
//			if(index < 0)
//				throw new IllegalStateException("Illegal state. Live link object not found in graph.");
//			
//			in.get(to.index()).remove(index);
//			inTags.get(to.index()).remove(index);
//
//			numLinks --;
//			
//			modCount++;
//			dead = true;
//
//			sorted = false;
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
	
	private class KLinkList extends AbstractList<KLink>
	{
		private List<Triple> triples;

		public KLinkList(List<Triple> triples)
		{
			this.triples = triples;
		}

		@Override
		public KLink get(int index)
		{
			return new KLink(triples.get(index));
		}

		@Override
		public int size()
		{
			return triples.size();
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
	
	private class FastNodeList extends AbstractList<KNode>
	{
		private List<Triple> triples;
		private boolean useSubject;

		public FastNodeList(List<Triple> triples, boolean useSubject)
		{
			this.triples = triples;
			this.useSubject = useSubject;
		}

		@Override
		public KNode get(int index)
		{
			Triple triple = triples.get(index);
			int ni = useSubject ? triple.subject() : triple.object();  
			
			return new KNode(ni);
		}

		@Override
		public int size()
		{
			return triples.size();
		}
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
		
		HDT hdt = HDTManager.loadIndexedHDT(
				file.getName().endsWith(".gz")?
			    new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))) :
				new BufferedInputStream(new FileInputStream(file)), null);
	
		nodes.clear();
		relations.clear();
		
		List<Triple> triples = Collections.emptyList();
    
    	try {
    		
    		// * Pass one: fill the maps
    		IteratorTripleString it = hdt.search("", "", "");
    
    		int estTotal = (int)it.estimatedNumResults(), total = 0;
    
    		int nextIndex = 0;
        	while(it.hasNext()) 
        	{
        		TripleString ts = it.next();
        
        		String subject = ts.getSubject().toString(),
        			   predicate = ts.getPredicate().toString(),
        		       object = ts.getObject().toString();
        		
        		Integer node1 = nodeMap.get(subject);
        		if(node1 == null)
        		{
        			node1 = nextIndex ++;
        			nodeMap.put(subject, node1);
        			nodes.add(subject);
        		}
        		
        		Integer node2 = nodeMap.get(object);
        		if(node2 == null)
        		{
        			node2 = nextIndex ++;
        			nodeMap.put(object, node2);
        			nodes.add(object);
        		}
        		
        		Integer tag = relationMap.get(predicate);
        		if(tag == null)
        		{
        			tag = nextTag ++;
        			
        			relationMap.put(predicate, tag);
        			relations.add(predicate);
        		}
        		  
        		total++;
        		// dot(i++, total);
        	}
        	
    		triples = new ArrayList<Triple>(total); 
        	
        	// * Pass two : extract the triples
     		it = hdt.search("", "", "");
     	        
        	while(it.hasNext()) 
        	{
        		TripleString ts = it.next();
        
        		String subject = ts.getSubject().toString(),
        			   predicate = ts.getPredicate().toString(),
        		       object = ts.getObject().toString();
        		
        		int s = nodeMap.get(subject),
        		    o = nodeMap.get(object),       		
        		    p = relationMap.get(predicate);
        		
        		triples.add(t(s, p, o));
        		    							        		
        		// dot(i++, total);
        	}
    	} catch (NotFoundException e)
    	{
    		// Empty graph
    	} finally
    	{
    		hdt.close();
    	}

		return new KGraph(triples);
	}	
	
//	/**
//	 * Can't get this to work just yet. Should use the HDT dictionaries.
//	 * 
//	 * @param file
//	 * @param nodes
//	 * @param relations
//	 * @return
//	 * @throws IOException
//	 */
//	public static KGraph loadHDTBuggy(File file, List<String> nodes, List<String> relations) 
//			throws IOException
//	{
//		int nextTag = 0; 
//		
//		Map<String, Integer> nodeMap = new HashMap<>(), relationMap = new HashMap<>();
//		KGraph graph = new KGraph();
//		
//		HDT hdt = HDTManager.loadIndexedHDT(
//				new BufferedInputStream(new FileInputStream(file)), null);
//		
//		LinkedHashSet<String> nodeSet = new LinkedHashSet<String>();
//		LinkedHashSet<String> relSet = new LinkedHashSet<String>();
//
//		try {
//			
//			// * First pass, get all node and predicate labels
//			IteratorTripleString it = hdt.search("", "", "");
//			
//			while(it.hasNext())
//			{
//				TripleString ts = it.next();
//				
//				String subject   = ts.getSubject().toString(),
//				       predicate = ts.getPredicate().toString(),
// 				       object    = ts.getObject().toString();
//				
//				nodeSet.add(subject);
//				nodeSet.add(object);
//				relSet.add(predicate);
//			}
//		} catch (NotFoundException e) 
//		{
//			// File must be empty, return empty graph
//		} finally 
//		{
//			// IMPORTANT: Free resources
//			hdt.close();
//		}
//
//		nodes.clear();
//		nodes.addAll(nodeSet);
//		for(int i : series(nodes.size()))
//			nodeMap.put(nodes.get(i), i);
//		
//		System.out.println(nodes.size());
//			
//		relations.clear();
//		relations.addAll(relSet);
//		for(int i : series(relations.size()))
//			relationMap.put(relations.get(i), i);
//		
//		Global.log().info("Labels read from dictionary");
//		
//		for(int i : series(nodes.size()))
//			graph.add();
//		System.out.println("... " + graph.nodes().size());
//				
//		
//		// * Second pass, construct graph
//		for(KNode node : graph.nodes())
//		{			
//			Functions.dot(node.index(), graph.size());
//			
//			String subject = nodes.get(node.index());
//			
//			IteratorTripleString it;
//			try {
//				it = hdt.search(subject, "", "");
//			} catch (NotFoundException e)
//			{
//				continue;
//			}
//			
//			System.out.println(it.estimatedNumResults());
//			
//			while(it.hasNext()) 
//			{
//				TripleString ts = it.next();
//
//				String predicate = ts.getPredicate().toString(),
//				       object = ts.getObject().toString();
//				    				
//				Integer objIndex = nodeMap.get(object);
//				if(objIndex == null)
//					System.out.println("object not found in map: " + object);
//				
//				KNode node2 = graph.get(objIndex);
//				int tag = relationMap.get(predicate);
//								
//				node.connect(node2, tag);
//			}
//
//		}
//
//		return graph;
//	}
//	
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
	 * Turn a pattern for a KGraph into a string-based pattern. Nonegative labels 
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
