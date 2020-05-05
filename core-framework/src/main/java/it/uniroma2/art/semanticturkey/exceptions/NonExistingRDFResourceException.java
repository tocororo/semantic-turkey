package it.uniroma2.art.semanticturkey.exceptions;

import java.util.Arrays;

import org.eclipse.rdf4j.model.Resource;

public class NonExistingRDFResourceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7470247664805672183L;

	/**
	 * construct this exception with the uri of the resource which has not been found
	 * 
	 * @param uri
	 */
	public NonExistingRDFResourceException(Resource resource) {
		super("resource: " + resource.stringValue() + " does not exist");
	}
	
	public NonExistingRDFResourceException(Resource resource, Resource[] graphs) {
		super("resource: " + resource.stringValue() + " does not exist in graphs: " + Arrays.toString(graphs));
	}
	
	public NonExistingRDFResourceException(String message){
		super(message);
	}

	/*public NonExistingRDFResourceException(ARTResource res, ARTResource[] graphs) {
		super("resource: " + res + " does not exist in graphs: " + Arrays.toString(graphs));
	}*/
	
}
