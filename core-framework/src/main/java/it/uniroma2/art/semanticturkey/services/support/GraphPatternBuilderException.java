package it.uniroma2.art.semanticturkey.services.support;

public class GraphPatternBuilderException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public GraphPatternBuilderException() {
	}

	public GraphPatternBuilderException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public GraphPatternBuilderException(String message, Throwable cause) {
		super(message, cause);
	}

	public GraphPatternBuilderException(String message) {
		super(message);
	}

	public GraphPatternBuilderException(Throwable cause) {
		super(cause);
	}

}
