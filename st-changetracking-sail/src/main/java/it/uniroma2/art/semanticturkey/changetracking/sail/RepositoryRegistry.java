package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.sail.NotifyingSail;

import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerFactory;

/**
 * A registry of repositories that is used by {@link ChangeTrackerFactory} to resolve references to history
 * repositories.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class RepositoryRegistry {
	private final Map<String, Repository> repos;

	private static final RepositoryRegistry instance = new RepositoryRegistry();

	public static RepositoryRegistry getInstance() {
		return instance;
	}

	private RepositoryRegistry() {
		repos = new HashMap<>();
	}

	public synchronized void addRepository(String id, Repository repo) {
		repos.put(id, repo);
	}

	public synchronized Repository getRepository(String id) {
		Repository repo = repos.get(id);

		return repo;
	}
}
