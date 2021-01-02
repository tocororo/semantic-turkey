package it.uniroma2.art.semanticturkey.exceptions;

import java.util.Arrays;

import org.eclipse.rdf4j.model.Resource;


public class NonExistingPredicateBetweenResourcesException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7470247664805672183L;

	/**
	 * construct this exception with the uri of the resource which has not been found
	 * 
	 * @param uri
	 */
	public NonExistingPredicateBetweenResourcesException(Resource resourceA, Resource resourceB) {
		super("There is no predicate between resource: " + resourceA.stringValue() + " and resource: "+resourceB.stringValue());
	}
	
	public NonExistingPredicateBetweenResourcesException(Resource resourceA, Resource resourceB, Resource[] graphs) {
		super("There is no predicate between resource: " + resourceA.stringValue() + " and resource: "+
				resourceB.stringValue()+" in graphs: " + Arrays.toString(graphs));
	}	
	
}
