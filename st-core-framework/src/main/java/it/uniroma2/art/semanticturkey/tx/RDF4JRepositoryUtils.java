package it.uniroma2.art.semanticturkey.tx;

import org.eclipse.rdf4j.repository.DelegatingRepositoryConnection;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.base.InterceptingRepositoryConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Utility class that provides static methods for obtaining connections from Sesame Repositories. It includes
 * support for transactional connections managed by {@link RDF4JRepositoryTransactionManager}. This class is
 * inspired by {@link org.springframework.jdbc.datasource.DataSourceUtils}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * 
 */
public class RDF4JRepositoryUtils {

	private static final Logger logger = LoggerFactory.getLogger(RDF4JRepositoryUtils.class);

	/**
	 * Returns a connection to a RDF4J Repository. This method is aware of a connection already bound to the
	 * thread, e.g. by {@link RDF4JRepositoryTransactionManager}.
	 * 
	 * @param repository
	 * @return
	 */
	public static RepositoryConnection getConnection(Repository repository) {
		return getConnection(repository, true);
	}

	public static RepositoryConnection getConnection(Repository repository, boolean canCreate) {
		RepositoryConnection conn = (RepositoryConnection) TransactionSynchronizationManager
				.getResource(repository);

		if (conn == null || !conn.isOpen()) {
			if (canCreate) {
				conn = repository.getConnection();
			} else {
				throw new IllegalStateException("No connection is bound to the thread");
			}
		}

		return RDF4JRepositoryUtils.wrapConnection(conn);
	}

	/**
	 * Closes a {@code connection} the the given {@code data source}, if the connection is not externally
	 * managed.
	 * 
	 * @param repoConn
	 * @param repository
	 */
	public static void releaseConnection(RepositoryConnection repoConn, Repository repository) {
		logger.debug("Inside releaseConnection; repoConn = {}, repository = {}", repoConn, repository);
		if (repoConn == null)
			return;

		if (repository != null) {
			RepositoryConnection txConn = (RepositoryConnection) TransactionSynchronizationManager
					.getResource(repository);

			if (txConn != null && txConn.isOpen() && connectionEquals(txConn, repoConn)) {
				return;
			}
		}

		logger.debug("About to close the connection");
		repoConn.close();
	}

	/**
	 * Compares two connections for equality, taking into consideration possible wrappers (currently on the
	 * second connection only).
	 * 
	 * @param heldConnection
	 * @param connection
	 * @return
	 */
	private static boolean connectionEquals(RepositoryConnection heldConnection,
			RepositoryConnection connection) {
		while (connection instanceof DelegatingRepositoryConnection) {
			if (heldConnection == connection || heldConnection.equals(connection))
				return true;

			connection = ((DelegatingRepositoryConnection) connection).getDelegate();
		}
		return heldConnection == connection || heldConnection.equals(connection);
	}

	/**
	 * Wraps the given repository connections with appropriate wrappers implementing the interfaces
	 * {@link DelegatingRepositoryConnection}. Currently, the only supported wrapper enforces that during a
	 * read-only transaction it is not possible to invoke mutation operations on a connection.
	 * 
	 * @param connection
	 * @return
	 */
	public static RepositoryConnection wrapConnection(RepositoryConnection connection) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
				connection = wrapReadOnlyConnection(connection);
			}

			connection = new TransactionAwareRDF4JRepostoryConnection(connection.getRepository(), connection);
		}
		return connection;
	}

	public static RepositoryConnection wrapReadOnlyConnection(RepositoryConnection connection) {
		InterceptingRepositoryConnectionWrapper wrappedConnection = new InterceptingRepositoryConnectionWrapper(
				connection.getRepository(), connection);
		wrappedConnection.addRepositoryConnectionInterceptor(
				new ThrowingReadOnlyRDF4JRepositoryConnectionInterceptor());
		return wrappedConnection;
	}
}
