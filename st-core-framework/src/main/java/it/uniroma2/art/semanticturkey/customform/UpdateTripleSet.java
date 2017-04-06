package it.uniroma2.art.semanticturkey.customform;

import java.util.ArrayList;
import java.util.List;

import it.uniroma2.art.coda.structures.ARTTriple;

public class UpdateTripleSet {
	
	private List<ARTTriple> insertTriples;
	private List<ARTTriple> deleteTriples;
	
	public UpdateTripleSet() {
		insertTriples = new ArrayList<>();
		deleteTriples = new ArrayList<>();
	}
	
	public List<ARTTriple> getInsertTriples() {
		return insertTriples;
	}
	public void setInsertTriples(List<ARTTriple> insertTriples) {
		this.insertTriples = insertTriples;
	}
	public void addInsertTriple(ARTTriple triple) {
		this.insertTriples.add(triple);
	}
	public void addInsertTriples(List<ARTTriple> triples) {
		this.insertTriples.addAll(triples);
	}
	
	public List<ARTTriple> getDeleteTriples() {
		return deleteTriples;
	}
	public void setDeleteTriples(List<ARTTriple> deleteTriples) {
		this.deleteTriples = deleteTriples;
	}
	public void addDeleteTriple(ARTTriple triple) {
		this.deleteTriples.add(triple);
	}
	public void addDeleteTriples(List<ARTTriple> triples) {
		this.deleteTriples.addAll(triples);
	}

}
