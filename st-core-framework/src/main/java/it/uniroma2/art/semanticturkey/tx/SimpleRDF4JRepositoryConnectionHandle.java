package it.uniroma2.art.semanticturkey.tx;

import org.openrdf.repository.RepositoryConnection;

public class SimpleRDF4JRepositoryConnectionHandle implements RDF4JRepositoryConnectionHandle {

	private RepositoryConnection connection;

	public SimpleRDF4JRepositoryConnectionHandle(RepositoryConnection connection) {
		this.connection = connection;
	}
	
	@Override
	public RepositoryConnection getConnection() {
		return connection;
	}

	/* (non-Javadoc)
	 * @see it.uniroma2.art.semanticturkey.tx.ConnectionHandle#releaseConnection(org.openrdf.repository.RepositoryConnection)
	 */
	@Override
	public void releaseConnection(RepositoryConnection connection) {
	}
	
}
