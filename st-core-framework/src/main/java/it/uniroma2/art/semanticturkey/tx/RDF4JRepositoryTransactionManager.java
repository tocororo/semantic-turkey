/**
 * 
 */
package it.uniroma2.art.semanticturkey.tx;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementation of {@link org.springframework.transaction.PlatformTransactionManager} for RDF4J
 * Repositories. This class is inspired by
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RDF4JRepositoryTransactionManager extends AbstractPlatformTransactionManager
		implements PlatformTransactionManager {

	private static final long serialVersionUID = -3096599710354974784L;

	private static final Logger logger = LoggerFactory.getLogger(RDF4JRepositoryTransactionManager.class);

	private static class RDF4JRepositoryTransactionObject {

		private RepositoryConnection connection;

		public RDF4JRepositoryTransactionObject(RepositoryConnection connection) {
			this.connection = connection;
		}

		public boolean hasConnection() {
			return connection != null;
		}

		public void setConnection(RepositoryConnection newConnection) {
			this.connection = newConnection;
		}

		public RepositoryConnection getConnection() {
			return connection;
		}
	}

	private Repository repository;

	public RDF4JRepositoryTransactionManager(Repository sesameRepository) {
		this.repository = sesameRepository;
		this.setNestedTransactionAllowed(false);
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		// RDF4JRepositoryTransactionObject txObject = new RDF4JRepositoryTransactionObject();

		// RDF4JRepositoryConnectionHolder connHolder = (RDF4JRepositoryConnectionHolder)
		// TransactionSynchronizationManager
		// .getResource(this.repository);

		RepositoryConnection conn = (RepositoryConnection) TransactionSynchronizationManager
				.getResource(this.repository);

		return new RDF4JRepositoryTransactionObject(conn);
		//
		//
		// txObject.setRDF4JRepositoryConnectionHolder(connHolder, false);
		//
		// return txObject;
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		RDF4JRepositoryTransactionObject txObject = (RDF4JRepositoryTransactionObject) transaction;
		RepositoryConnection conn = null;

		try {
			logger.debug("Inside doBegin");

			if (txObject.hasConnection()) {
				throw new CannotCreateTransactionException("Transaction already started for the repository");
			}

			logger.debug("About to begin");

			conn = repository.getConnection();

			conn.begin();

			txObject.setConnection(conn);

			TransactionSynchronizationManager.bindResource(this.repository, conn);

			logger.debug("Begun");

		} catch (Throwable e) {
			conn.close();
			throw new CannotCreateTransactionException("Could not open RDF4J Connection for transaction", e);
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		RDF4JRepositoryTransactionObject txObject = (RDF4JRepositoryTransactionObject) status
				.getTransaction();
		RepositoryConnection conn = txObject.getConnection();

		try {
			logger.debug("About to commit");
			conn.commit();
		} catch (RepositoryException e) {
			throw new TransactionSystemException("Could not commit RDF4J transaction", e);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		RDF4JRepositoryTransactionObject txObject = (RDF4JRepositoryTransactionObject) status
				.getTransaction();
		RepositoryConnection conn = txObject.getConnection();

		try {
			logger.debug("About to rollback");
			conn.rollback();
		} catch (RepositoryException e) {
			throw new TransactionSystemException("Could not rollback RDF4J transaction", e);
		}
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		logger.debug("Inside doCleanupAfterCompletion");

		RDF4JRepositoryTransactionObject txObject = (RDF4JRepositoryTransactionObject) transaction;
		try {
			TransactionSynchronizationManager.unbindResource(this.repository);

		} finally {
			RepositoryConnection conn = txObject.getConnection();
			conn.close();
		}
	}

}
