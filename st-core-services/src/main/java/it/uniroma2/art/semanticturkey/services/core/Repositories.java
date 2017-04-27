package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;

import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services for accessing remote repositories.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Repositories extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Repositories.class);

	@STServiceOperation(method=RequestMethod.POST)
	// TODO: establish authorization @PreAuthorize("@auth.isAuthorized('rdf(cls, taxonomy)', 'R')")
	public Collection<RepositoryInfo> getRemoteRepositories(String serverURL, @Optional String username,
			@Optional String password) {
		RemoteRepositoryManager remoteRepositoryManager = new RemoteRepositoryManager(serverURL);
		remoteRepositoryManager.initialize();
		try {
			return remoteRepositoryManager.getAllRepositoryInfos();
		} finally {
			remoteRepositoryManager.shutDown();
		}
	}

}