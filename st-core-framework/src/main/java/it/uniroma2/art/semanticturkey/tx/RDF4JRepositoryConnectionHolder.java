package it.uniroma2.art.semanticturkey.tx;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.ResourceHolder;
import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * This class is inspired by {@link org.springframework.jdbc.datasource.ConnectionHolder}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RDF4JRepositoryConnectionHolder extends ResourceHolderSupport implements ResourceHolder  {

	private static final Logger logger = LoggerFactory.getLogger(RDF4JRepositoryConnectionHolder.class);
	
	private RepositoryConnection currentConnection;
	private SimpleRDF4JRepositoryConnectionHandle connectionHandle;
	private boolean transactionActive = false;
	
	public RDF4JRepositoryConnectionHolder(RepositoryConnection connection) {
		this.currentConnection = connection;
		this.connectionHandle = new SimpleRDF4JRepositoryConnectionHandle(connection);
	}

	public RepositoryConnection getConnection() {
		if (this.currentConnection == null) {
			this.currentConnection = this.connectionHandle.getConnection();
		}
		
		return this.currentConnection;
	}
	
	@Override
	public void requested() {
		super.requested();
		logger.debug("Requested holder: {}", this);
	}
	
	@Override
	public void released() {
		super.released();
		logger.debug("Released holder: {}", this);
		if (!isOpen() && this.currentConnection != null) {
			this.connectionHandle.releaseConnection(this.currentConnection);
			this.currentConnection = null;
			transactionActive = false;
		}
	}

	public boolean hasConnection() {
		return this.connectionHandle != null;
	}

	public void setConnection(RepositoryConnection connection) {
		if (currentConnection != null) {
			connectionHandle.releaseConnection(currentConnection);
			currentConnection = null;
		}
				
		if (connection != null) {
			connectionHandle = new SimpleRDF4JRepositoryConnectionHandle(connection);
		} else {
			connectionHandle = null;
		}
	}

	public void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	public boolean isTransactionActive() {
		return transactionActive;
	}

	@Override
	public void clear() {
		super.clear();
		transactionActive = false;
	}
}
