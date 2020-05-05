package it.uniroma2.art.semanticturkey.changetracking;

import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

/**
 * Signals that it was not possible to detect the {@link ChangeTracker} sail on a
 * {@link RepositoryConnection}. In general, there are two possibilities:
 * <ul>
 * <li>the <em>change tracker</em> was not configured</li>
 * <li>the <em>change tracker</em> is in fact active, but it is an old version (2.0) that does not support
 * detection</li>
 * </ul>
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerNotDetectedException extends Exception {

	private static final long serialVersionUID = 6680349708289160482L;

	private static final String MESSAGE = "Either the change tracker sail is not configured on the given repository or it is an outdated version (up to 2.0) that is not detectable";

	public ChangeTrackerNotDetectedException() {
		super();
	}

	public ChangeTrackerNotDetectedException(Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(MESSAGE, cause, enableSuppression, writableStackTrace);
	}

	public ChangeTrackerNotDetectedException(Throwable cause) {
		super(cause);
	}

}
