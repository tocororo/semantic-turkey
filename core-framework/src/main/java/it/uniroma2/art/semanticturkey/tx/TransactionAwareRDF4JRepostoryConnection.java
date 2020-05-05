package it.uniroma2.art.semanticturkey.tx;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;

/**
 * A {@link RepositoryConnectionWrapper} that is aware of possible transactions bound the current thread.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class TransactionAwareRDF4JRepostoryConnection extends RepositoryConnectionWrapper {

	public TransactionAwareRDF4JRepostoryConnection(Repository repository, RepositoryConnection delegate) {
		super(repository, delegate);
	}

	@Override
	public void close() throws RepositoryException {
		RDF4JRepositoryUtils.releaseConnection(getDelegate(), getRepository());
	}
}
