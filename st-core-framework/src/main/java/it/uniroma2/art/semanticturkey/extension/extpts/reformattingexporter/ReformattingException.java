package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

/**
 * This exception class represents a failure of a {@link ReformattingExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ReformattingException extends Exception {

	private static final long serialVersionUID = 8581278466365295831L;

	public ReformattingException() {
		super();
	}

	public ReformattingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ReformattingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReformattingException(String message) {
		super(message);
	}

	public ReformattingException(Throwable cause) {
		super(cause);
	}
	
}
