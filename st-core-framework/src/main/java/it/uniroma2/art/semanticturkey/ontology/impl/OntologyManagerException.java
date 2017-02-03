package it.uniroma2.art.semanticturkey.ontology.impl;

/**
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class OntologyManagerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OntologyManagerException() {
		super();
	}

	public OntologyManagerException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public OntologyManagerException(String message, Throwable cause) {
		super(message, cause);
	}

	public OntologyManagerException(String message) {
		super(message);
	}

	public OntologyManagerException(Throwable cause) {
		super(cause);
	}

}
