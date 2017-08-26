package nl.peterbloem.motive.rdf;

public class Triple
{
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
	
	
}
