 package nl.peterbloem.motive.rdf;

/**
 * A triple is any ordered trio of integers, in the abstract. While it is usually 
 * used to represent edges in knowledge graphs, it does not make reference to any
 * specific knowledge graph. For a concrete object representing a specific edge 
 * in a specific knowledge graph, see KGraph.KLink
 *  
 * @author Peter
 *
 */
public class Triple
{
	
	public static enum Part {SUBJECT, PREDICATE, OBJECT}
	
	final int prime = 31;
	private int subject, object, predicate;
	private int hash;

	public Triple(int subject, int predicate, int object)
	{
		super();
		this.subject = subject;
		this.object = object;
		this.predicate = predicate;
		
		hash = 1;
		hash = prime * hash + object;
		hash = prime * hash + predicate;
		hash = prime * hash + subject;
	}

	public int get(Part part)
	{
		if(part == Part.SUBJECT)
			return subject;
		if(part == Part.PREDICATE)
			return predicate;
		return object;
	}
	
	@Override
	public String toString()
	{
		return "(" + subject + " [" + predicate + "> "+object+ ")";
	}

	@Override
	public int hashCode()
	{
		return hash;
	}

	public int subject()
	{
		return subject;
	}

	public int object()
	{
		return object;
	}

	public int predicate()
	{
		return predicate;
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
		Triple other = (Triple) obj;
		if (object != other.object)
			return false;
		if (predicate != other.predicate)
			return false;
		if (subject != other.subject)
			return false;
		return true;
	}
	
	public static Triple t(int s, int p, int o)
	{
		return new Triple(s, p, o);
	}
	
}
