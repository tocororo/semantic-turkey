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

		private RDF4JRepositoryConnectionHolder conHolder;
		private boolean newConnectionHolder;
		
		public void setRDF4JRepositoryConnectionHolder(RDF4JRepositoryConnectionHolder conHolder, boolean newConnectionHolder) {
			this.conHolder = conHolder;
			this.newConnectionHolder = newConnectionHolder;
		}

		public RDF4JRepositoryConnectionHolder getRDF4JRepositoryConnectionHolder() {
			return this.conHolder;
		}

		public boolean isNewConnectionHolder() {
			return newConnectionHolder;
		}
	}

	private Repository repository;

	public RDF4JRepositoryTransactionManager(Repository sesameRepository) {
		this.repository = sesameRepository;
		this.setNestedTransactionAllowed(false);
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		RDF4JRepositoryTransactionObject txObject = new RDF4JRepositoryTransactionObject();

		RDF4JRepositoryConnectionHolder connHolder = (RDF4JRepositoryConnectionHolder) TransactionSynchronizationManager
				.getResource(this.repository);

		txObject.setRDF4JRepositoryConnectionHolder(connHolder, false);

		return txObject;
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		RDF4JRepositoryTransactionObject txObject = (RDF4JRepositoryTransactionObject) transaction;
		RepositoryConnection conn = null;

		try {
			logger.debug("Inside doBegin");
			boolean newConnHolder = false;

			if (txObject.getRDF4JRepositoryConnectionHolder() == null
					|| txObject.getRDF4JRepositoryConnectionHolder().isSynchronizedWithTransaction()) {
				conn = this.repository.getConnection();
				logger.debug("New connection: {}", conn);
				txObject.setRDF4JRepositoryConnectionHolder(
						new RDF4JRepositoryConnectionHolder(conn), true);
				newConnHolder = true;
			}

			txObject.getRDF4JRepositoryConnectionHolder().setSynchronizedWithTransaction(true);
			conn = txObject.getRDF4JRepositoryConnectionHolder().getConnection();

			logger.debug("About to begin");

			conn.begin();

			txObject.getRDF4JRepositoryConnectionHolder().setTransactionActive(true);

			int timeout = determineTimeout(definition);

			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getRDF4JRepositoryConnectionHolder().setTimeoutInSeconds(timeout);
			}
			if (newConnHolder) {
				TransactionSynchronizationManager.bindResource(this.repository,
						txObject.getRDF4JRepositoryConnectionHolder());
			}
		} catch (Throwable e) {
			RDF4JRepositoryUtils.releaseConnection(conn, repository);
			throw new CannotCreateTransactionException("Could not open RDF4J Connection for transaction", e);
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		RDF4JRepositoryTransactionObject txObject = (RDF4JRepositoryTransactionObject) status
				.getTransaction();
		RepositoryConnection conn = txObject.getRDF4JRepositoryConnectionHolder().getConnection();

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
		RepositoryConnection conn = txObject.getRDF4JRepositoryConnectionHolder().getConnection();

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

		if (txObject.isNewConnectionHolder()) {
			logger.debug("About to release connection");
			TransactionSynchronizationManager.unbindResource(this.repository);
			RepositoryConnection conn = txObject.getRDF4JRepositoryConnectionHolder().getConnection();
			RDF4JRepositoryUtils.releaseConnection(conn, this.repository);
		}
		txObject.getRDF4JRepositoryConnectionHolder().clear();
	}

}
