package it.uniroma2.art.semanticturkey.tx;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;

/**
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class TransactionAwareRDF4JRepository extends RepositoryWrapper {

	public TransactionAwareRDF4JRepository(Repository delegate) {
		super(delegate);
	}
	
	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		return RDF4JRepositoryUtils.getConnection(getDelegate());
	}
	
}
