/**
 * 
 */
package it.uniroma2.art.semanticturkey.tx;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
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

	private static class SesameRepositoryTransactionObject {

		private RDF4JRepositoryConnectionHolder conHolder;
		private boolean newConnectionHolder;
		
		public void setSesameRepositoryConnectionHolder(RDF4JRepositoryConnectionHolder conHolder, boolean newConnectionHolder) {
			this.conHolder = conHolder;
			this.newConnectionHolder = newConnectionHolder;
		}

		public RDF4JRepositoryConnectionHolder getSesameRepositoryConnectionHolder() {
			return this.conHolder;
		}

		public boolean isNewConnectionHolder() {
			return newConnectionHolder;
		}
	}

	private Repository sesameRepository;

	public RDF4JRepositoryTransactionManager(Repository sesameRepository) {
		this.sesameRepository = sesameRepository;
		this.setNestedTransactionAllowed(false);
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		SesameRepositoryTransactionObject txObject = new SesameRepositoryTransactionObject();

		RDF4JRepositoryConnectionHolder connHolder = (RDF4JRepositoryConnectionHolder) TransactionSynchronizationManager
				.getResource(this.sesameRepository);

		txObject.setSesameRepositoryConnectionHolder(connHolder, false);

		return txObject;
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		SesameRepositoryTransactionObject txObject = (SesameRepositoryTransactionObject) transaction;
		RepositoryConnection conn = null;

		try {
			boolean newConnHolder = false;

			if (txObject.getSesameRepositoryConnectionHolder() == null
					|| txObject.getSesameRepositoryConnectionHolder().isSynchronizedWithTransaction()) {
				System.out.println("New connection!");
				conn = this.sesameRepository.getConnection();
				txObject.setSesameRepositoryConnectionHolder(
						new RDF4JRepositoryConnectionHolder(conn), true);
				newConnHolder = true;
			}

			txObject.getSesameRepositoryConnectionHolder().setSynchronizedWithTransaction(true);
			conn = txObject.getSesameRepositoryConnectionHolder().getConnection();

			System.out.println("Beginning!");

			conn.begin();

			txObject.getSesameRepositoryConnectionHolder().setTransactionActive(true);

			int timeout = determineTimeout(definition);

			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getSesameRepositoryConnectionHolder().setTimeoutInSeconds(timeout);
			}
			if (newConnHolder) {
				TransactionSynchronizationManager.bindResource(this.sesameRepository,
						txObject.getSesameRepositoryConnectionHolder());
			}
		} catch (Throwable e) {
			RDF4JRepositoryUtils.releaseConnection(conn, sesameRepository);
			throw new CannotCreateTransactionException("Could not open Sesame Connection for transaction", e);
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		SesameRepositoryTransactionObject txObject = (SesameRepositoryTransactionObject) status
				.getTransaction();
		RepositoryConnection conn = txObject.getSesameRepositoryConnectionHolder().getConnection();

		try {
			System.out.println("Committing");
			conn.commit();
		} catch (RepositoryException e) {
			throw new TransactionSystemException("Could not commit Sesame transaction", e);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		SesameRepositoryTransactionObject txObject = (SesameRepositoryTransactionObject) status
				.getTransaction();
		RepositoryConnection conn = txObject.getSesameRepositoryConnectionHolder().getConnection();

		try {
			System.out.println("Rolling back");
			conn.rollback();
		} catch (RepositoryException e) {
			throw new TransactionSystemException("Could not rollback Sesame transaction", e);
		}
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		SesameRepositoryTransactionObject txObject = (SesameRepositoryTransactionObject) transaction;

		if (txObject.isNewConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(this.sesameRepository);
			RepositoryConnection conn = txObject.getSesameRepositoryConnectionHolder().getConnection();
			RDF4JRepositoryUtils.releaseConnection(conn, this.sesameRepository);
		}
		txObject.getSesameRepositoryConnectionHolder().clear();
	}

}
