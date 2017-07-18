package it.uniroma2.art.semanticturkey.exceptions;

public class AlreadyExistingLiteralFormForResourceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7470247664805672183L;

	/**
	 * construct this exception with the uri of the resource which has not been found
	 * 
	 * @param uri
	 */
	public AlreadyExistingLiteralFormForResourceException(String text) {
		super(text);
	}
}
