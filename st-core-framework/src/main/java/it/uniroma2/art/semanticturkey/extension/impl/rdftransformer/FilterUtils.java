package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Utility class for the implementation of filters.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class FilterUtils {

	/**
	 * Returns the provided array of graphs, or return every graph (the name of which is an IRI) in case of an
	 * empty array.
	 * 
	 * @param workingRepositoryConnection
	 * @param graphs
	 * @return
	 */
	public static IRI[] expandGraphs(RepositoryConnection workingRepositoryConnection, IRI[] graphs) {
		if (graphs.length != 0) {
			return graphs;
		}

		return Iterations.stream(workingRepositoryConnection.getContextIDs()).filter(IRI.class::isInstance)
				.toArray(IRI[]::new);
	}
}
