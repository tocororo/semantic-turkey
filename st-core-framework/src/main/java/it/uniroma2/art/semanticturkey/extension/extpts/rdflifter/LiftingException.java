package it.uniroma2.art.semanticturkey.extension.extpts.rdflifter;

/**
 * This exception class represents a failure of a {@link RDFLifter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class LiftingException extends Exception {

	private static final long serialVersionUID = 8581278466365295831L;

	public LiftingException() {
		super();
	}

	public LiftingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public LiftingException(String message, Throwable cause) {
		super(message, cause);
	}

	public LiftingException(String message) {
		super(message);
	}

	public LiftingException(Throwable cause) {
		super(cause);
	}
	
}
