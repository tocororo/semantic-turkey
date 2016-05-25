package it.uniroma2.art.semanticturkey.tx;

import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public interface RDF4JRepositoryConnectionHandle {

	void releaseConnection(RepositoryConnection connection);

	RepositoryConnection getConnection();

}