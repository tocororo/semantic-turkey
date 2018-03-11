package it.uniroma2.art.semanticturkey.changetracking;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

/**
 * Signals a mismatch between the detected version of the {@link ChangeTracker} sail on a connection and the
 * expected one.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerParameterMismatchException extends Exception {

	private static final long serialVersionUID = 4617757884387260632L;

	private final IRI parameter;
	private final /* @Nullable */ String expectedVersion;
	private final String actualVersion;

	public ChangeTrackerParameterMismatchException(IRI parameter, /* @Nullable */ String expectedVersion,
			String actualVersion) {
		super("Expected value \'" + expectedVersion + "\' for parameter \'" + parameter + "\' but detected \'"
				+ actualVersion + "\'");
		this.parameter = parameter;
		this.expectedVersion = expectedVersion;
		this.actualVersion = actualVersion;
	}

	public String getActualVersion() {
		return actualVersion;
	}

	public /* @Nullable */ String getExpectedVersion() {
		return expectedVersion;
	}

	public IRI getParameter() {
		return parameter;
	}
}
