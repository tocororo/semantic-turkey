package it.uniroma2.art.semanticturkey.tx;

import org.openrdf.repository.DelegatingRepositoryConnection;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.base.InterceptingRepositoryConnectionWrapper;
import org.springframework.transaction.support.TransactionSynchronization;
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

	/**
	 * Returns a connection to a Sesame Repository. This method is aware of a connection already bound to the
	 * thread, e.g. by {@link RDF4JRepositoryTransactionManager}.
	 * 
	 * @param sesameRepository
	 * @return
	 */
	public static RepositoryConnection getConnection(Repository sesameRepository) {
		RDF4JRepositoryConnectionHolder connHolder = (RDF4JRepositoryConnectionHolder) TransactionSynchronizationManager
				.getResource(sesameRepository);

		if (connHolder != null
				&& (connHolder.hasConnection() || connHolder.isSynchronizedWithTransaction())) {
			connHolder.requested();
			if (!connHolder.hasConnection()) {
				connHolder.setConnection(sesameRepository.getConnection());
			}
			return RDF4JRepositoryUtils.wrapConnection(connHolder.getConnection());
		}

		RepositoryConnection conn = sesameRepository.getConnection();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {

			RDF4JRepositoryConnectionHolder holderToUse = connHolder;

			if (holderToUse == null) {
				holderToUse = new RDF4JRepositoryConnectionHolder(conn);
			} else {
				holderToUse.setConnection(conn);
			}
			holderToUse.requested();
			TransactionSynchronizationManager
					.registerSynchronization(new ConnectionSynchronization(holderToUse, sesameRepository));
			holderToUse.setSynchronizedWithTransaction(true);
			if (holderToUse != connHolder) {
				TransactionSynchronizationManager.bindResource(sesameRepository, holderToUse);
			}
		}

		return RDF4JRepositoryUtils.wrapConnection(conn);
	}

	/**
	 * Closes a {@code connection} the the given {@code data source}, if the connection is not externally
	 * managed.
	 * 
	 * @param repoConn
	 * @param sesameRepository
	 */
	public static void releaseConnection(RepositoryConnection repoConn, Repository sesameRepository) {
		if (repoConn == null)
			return;

		if (sesameRepository != null) {
			RDF4JRepositoryConnectionHolder connHolder = (RDF4JRepositoryConnectionHolder) TransactionSynchronizationManager
					.getResource(sesameRepository);

			if (connHolder != null && connectionEquals(connHolder.getConnection(), repoConn)) {
				connHolder.released();
				return;
			}
		}

		System.out.println("Connection closed");
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
		if (connection instanceof DelegatingRepositoryConnection) {
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
	private static RepositoryConnection wrapConnection(RepositoryConnection connection) {
		if (TransactionSynchronizationManager.isSynchronizationActive()
				&& TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			System.out.println("Proxied");
			@SuppressWarnings("resource")
			InterceptingRepositoryConnectionWrapper wrappedConnection = new InterceptingRepositoryConnectionWrapper(
					connection.getRepository(), connection);
			wrappedConnection.addRepositoryConnectionInterceptor(
					new ThrowingReadOnlyRDF4JRepositoryConnectionInterceptor());
			connection = wrappedConnection;
		}
		return connection;
	}

	private static class ConnectionSynchronization implements TransactionSynchronization {

		private RDF4JRepositoryConnectionHolder connHolder;
		private Repository sesameRepository;

		public ConnectionSynchronization(RDF4JRepositoryConnectionHolder connHolder,
				Repository sesameRepository) {
			this.connHolder = connHolder;
			this.sesameRepository = sesameRepository;
		}

		@Override
		public void suspend() {
		}

		@Override
		public void resume() {
		}

		@Override
		public void flush() {
		}

		@Override
		public void beforeCommit(boolean readOnly) {
		}

		@Override
		public void beforeCompletion() {
		}

		@Override
		public void afterCommit() {
		}

		@Override
		public void afterCompletion(int status) {
		}

	}

}
