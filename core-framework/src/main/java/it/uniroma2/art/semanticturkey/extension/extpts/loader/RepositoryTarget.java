package it.uniroma2.art.semanticturkey.extension.extpts.loader;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFHandler;

/**
 * A {@link Target} for a {@link Loader} that wraps an {@link RepositoryConnection}. This kind of target is
 * used, for example, when data are loaded from a source capable of producing RDF data natively (e.g. a triple
 * store).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RepositoryTarget extends Target {
	private final RDFHandler targetRDFHandler;

	public RepositoryTarget(RDFHandler targetRDFHandler) {
		this.targetRDFHandler = targetRDFHandler;
	}

	public RDFHandler getTargetRepositoryConnection() {
		return targetRDFHandler;
	}
}
