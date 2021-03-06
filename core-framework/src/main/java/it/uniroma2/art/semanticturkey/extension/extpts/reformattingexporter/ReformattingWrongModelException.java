package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

/**
 * This exception class represents a failure of a {@link ReformattingExporter}.
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
public class ReformattingWrongModelException extends ReformattingException {

	private static final long serialVersionUID = 8581278466365295831L;

	public ReformattingWrongModelException() {
		super();
	}

	public ReformattingWrongModelException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ReformattingWrongModelException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReformattingWrongModelException(String message) {
		super(message);
	}

	public ReformattingWrongModelException(Throwable cause) {
		super(cause);
	}
	
}
