package it.uniroma2.art.semanticturkey.services.core.lime;

import java.math.BigInteger;

public class DatasetStatistics {
	private int references;
	private BigInteger triples;

	public int getReferences() {
		return references;
	}

	public void setReferences(int references) {
		this.references = references;
	}

	public void setTriples(BigInteger triples) {
		this.triples = triples;
	}
	
	public BigInteger getTriples() {
		return triples;
	}
	
}
