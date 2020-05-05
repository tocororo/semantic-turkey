package it.uniroma2.art.semanticturkey.exceptions;

import java.util.Arrays;

import org.eclipse.rdf4j.model.Resource;


public class NonExistingLiteralFormForResourceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7470247664805672183L;

	/**
	 * construct this exception with the uri of the resource which has not been found
	 * 
	 * @param uri
	 */
	public NonExistingLiteralFormForResourceException(Resource resource) {
		super("resource: " + resource + " does not have a literal form");
	}
	
	public NonExistingLiteralFormForResourceException(Resource resource, Resource[] graphs) {
		super("resource: " + resource + " does not not have a literal form in graphs: " + Arrays.toString(graphs));
	}	
	
}
