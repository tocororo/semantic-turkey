package it.uniroma2.art.semanticturkey.sparql;

public class SPARQLShallowParserException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SPARQLShallowParserException() {
	}

	public SPARQLShallowParserException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SPARQLShallowParserException(String message, Throwable cause) {
		super(message, cause);
	}

	public SPARQLShallowParserException(String message) {
		super(message);
	}

	public SPARQLShallowParserException(Throwable cause) {
		super(cause);
	}

}
