package it.uniroma2.art.semanticturkey.exceptions;

/**
 * Exception thrown when an operation is denied because of a blacklisted argument.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class BlacklistForbiddendException extends RuntimeException {

	private static final long serialVersionUID = 5823749040619351198L;

	public BlacklistForbiddendException() {
	}

	public BlacklistForbiddendException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BlacklistForbiddendException(String message, Throwable cause) {
		super(message, cause);
	}

	public BlacklistForbiddendException(String message) {
		super(message);
	}

	public BlacklistForbiddendException(Throwable cause) {
		super(cause);
	}

}
