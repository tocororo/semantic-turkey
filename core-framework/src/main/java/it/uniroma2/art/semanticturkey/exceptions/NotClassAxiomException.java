package it.uniroma2.art.semanticturkey.exceptions;

import java.util.Arrays;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Resource;


public class NotClassAxiomException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7470247664805672183L;

	/**
	 * construct this exception with the uri of the resource which has not been found
	 * 
	 * @param uri
	 */
	public NotClassAxiomException(BNode bnode) {
		super("bnode: " + bnode + " does not represent a Class Axiom");
	}
	
	public NotClassAxiomException(BNode bnode, Resource[] graphs) {
		super("bnode: " + bnode + " does represent a Class Axiom in graphs: " + Arrays.toString(graphs));
	}	
	
}
