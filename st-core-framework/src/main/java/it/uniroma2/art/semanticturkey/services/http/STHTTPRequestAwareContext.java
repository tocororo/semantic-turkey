package it.uniroma2.art.semanticturkey.services.http;

import javax.annotation.PreDestroy;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class STHTTPRequestAwareContext {
	
	private static final Logger logger = LoggerFactory.getLogger(STHTTPRequestAwareContext.class);
	
	private RepositoryConnection repoConn;
	
	public synchronized RepositoryConnection acquireConnectionIfNecessary(Repository repo) {
		if (repoConn == null) {
			repoConn = repo.getConnection();
			logger.debug("Connection created: " + repoConn);
		}
		
		return repoConn;
	}
	
	@PreDestroy
	public synchronized void destory() {
		if (repoConn != null) {
			logger.debug("Connection closed: " + repoConn);
			repoConn.close();
			repoConn = null;
		}
	}
	
}
