package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

/**
 * This exception class represents a failure of a {@link ReformattingExporter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
public class ReformattingWrongLexModelException extends ReformattingException{

	private static final long serialVersionUID = 8581278466365295831L;

	public ReformattingWrongLexModelException() {
		super();
	}

	public ReformattingWrongLexModelException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ReformattingWrongLexModelException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReformattingWrongLexModelException(String message) {
		super(message);
	}

	public ReformattingWrongLexModelException(Throwable cause) {
		super(cause);
	}
	
}
