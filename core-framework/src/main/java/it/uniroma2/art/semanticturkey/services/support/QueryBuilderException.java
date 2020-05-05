package it.uniroma2.art.semanticturkey.services.support;

public class QueryBuilderException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public QueryBuilderException() {
	}

	public QueryBuilderException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public QueryBuilderException(String message, Throwable cause) {
		super(message, cause);
	}

	public QueryBuilderException(String message) {
		super(message);
	}

	public QueryBuilderException(Throwable cause) {
		super(cause);
	}

}
