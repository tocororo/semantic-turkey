package it.uniroma2.art.semanticturkey.tx;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;

public class TransactionAwareRDF4JRepostoryConnection extends RepositoryConnectionWrapper {

	public TransactionAwareRDF4JRepostoryConnection(Repository repository, RepositoryConnection delegate) {
		super(repository, delegate);
	}

	@Override
	public void close() throws RepositoryException {
		RDF4JRepositoryUtils.releaseConnection(getDelegate(), getRepository());
	}
}
