package it.uniroma2.art.semanticturkey.extension.extpts.export;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Extension point for the export filters. Different export filters are subsequently invoked on a <i>working
 * copy</i> (in memory, without inference) of the <i>repository to be exported</i>.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface RDFTransformer {
	/**
	 * Apply a (possibly destructive) filter on a <i>working copy</i> of the <i>source repository<i>.
	 * 
	 * @param sourceRepositoryConnection
	 *            a connection to the source (unmodified) repository
	 * @param workingRepositoryConnection
	 *            a connection to the working of repository
	 * @param graphs
	 *            graphs to filter. An empty array indicates that every graph (the name of which is an IRI) in
	 *            the <code>workingRepository</code> is filtered
	 * @throws RDF4JException
	 */
	void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException;
}
