package it.uniroma2.art.semanticturkey.changetracking.sail.config;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;
import it.uniroma2.art.semanticturkey.changetracking.sail.RepositoryRegistry;

/**
 * Factory for {@link ChangeTracker}.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerFactory implements SailFactory {
	private static final Logger logger = LoggerFactory.getLogger(ChangeTrackerFactory.class);

	public static final String SAIL_TYPE = "http://semanticturkey.uniroma2.it/sail/changetracker";

	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new ChangeTrackerConfig(null);
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		ChangeTrackerConfig config2 = (ChangeTrackerConfig) config;

		ValueFactory vf = SimpleValueFactory.getInstance();

		String serverURL = config2.getServerURL();
		String metadataRepoId = config2.getHistoryRepositoryID();
		String metadataNS = config2.getHistoryNS();
		IRI metadataGraph = config2.getHistoryGraph();
		Set<IRI> includeGraph = config2.getIncludeGraph();
		Set<IRI> excludeGraph = config2.getExcludeGraph();
		boolean interactiveNotifications = config2.isInteractiveNotifications();

		Repository metadataRepo;

		if (serverURL != null) {
			metadataRepo = new HTTPRepository(serverURL, metadataRepoId);
		} else {
			metadataRepo = RepositoryRegistry.getInstance().getRepository(metadataRepoId);
		}
		logger.debug(
				"Created new ChangeTracker // metadataRepoId = {} // metadataRepo = {} // metadataNS = {} // metadataGraph = {} // includeGraph = {} // excludeGraph = {} // interactiveNotifications = {}",
				metadataRepoId, metadataRepo, metadataNS, metadataGraph, includeGraph, excludeGraph,
				interactiveNotifications);

		return new ChangeTracker(metadataRepo, metadataNS, metadataGraph, includeGraph, excludeGraph,
				interactiveNotifications);
	}

}
