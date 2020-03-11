package it.uniroma2.art.semanticturkey.customform;

import java.util.ArrayList;
import java.util.List;

import it.uniroma2.art.coda.structures.CODATriple;

public class UpdateTripleSet {
	
	private List<CODATriple> insertTriples;
	private List<CODATriple> deleteTriples;
	
	public UpdateTripleSet() {
		insertTriples = new ArrayList<>();
		deleteTriples = new ArrayList<>();
	}
	
	public List<CODATriple> getInsertTriples() {
		return insertTriples;
	}
	public void setInsertTriples(List<CODATriple> insertTriples) {
		this.insertTriples = insertTriples;
	}
	public void addInsertTriple(CODATriple triple) {
		this.insertTriples.add(triple);
	}
	public void addInsertTriples(List<CODATriple> triples) {
		this.insertTriples.addAll(triples);
	}
	
	public List<CODATriple> getDeleteTriples() {
		return deleteTriples;
	}
	public void setDeleteTriples(List<CODATriple> deleteTriples) {
		this.deleteTriples = deleteTriples;
	}
	public void addDeleteTriple(CODATriple triple) {
		this.deleteTriples.add(triple);
	}
	public void addDeleteTriples(List<CODATriple> triples) {
		this.deleteTriples.addAll(triples);
	}

}
