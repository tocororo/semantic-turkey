package it.uniroma2.art.semanticturkey.services;

public class InvalidContextException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5442305832215859184L;

	public InvalidContextException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidContextException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public InvalidContextException(String message) {
		super(message);
	}

	public InvalidContextException(Throwable cause) {
		super(cause);
	}

}
