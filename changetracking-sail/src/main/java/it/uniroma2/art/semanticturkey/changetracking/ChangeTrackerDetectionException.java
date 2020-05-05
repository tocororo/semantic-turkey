package it.uniroma2.art.semanticturkey.changetracking;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

/**
 * Signals a problem occurred during the application of the protocol to detect the presence of the
 * {@link ChangeTracker} sail on the given connection.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerDetectionException extends RuntimeException {

	private static final long serialVersionUID = 8417282777034590355L;

	public ChangeTrackerDetectionException() {
		super();
	}

	public ChangeTrackerDetectionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ChangeTrackerDetectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChangeTrackerDetectionException(String message) {
		super(message);
	}

	public ChangeTrackerDetectionException(Throwable cause) {
		super(cause);
	}

}
