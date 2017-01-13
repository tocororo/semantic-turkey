package it.uniroma2.art.semanticturkey.plugin.extpts;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Extension point for the export filters. Different export filters are subsequently invoked on a <i>working
 * copy</i> (in memory, without inference) of the <i>repository to be exported</i>.
 *
  * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface ExportFilter {
	/**
	 * Apply a (possibly destructive) filter on a <i>working copy</i> of the <i>source repository<i>.
	 * 
	 * @param sourceRepositoryConnection
	 *            a connection to the source (unmodified) repository
	 * @param workingRepositoryConnection
	 *            a connection to the working of repository
	 * @param workingGraph
	 *            the graph containing the core data (other graphs will usually contain imported data)
	 * @throws RDF4JException
	 */
	void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, Resource workingGraph) throws RDF4JException;
}
