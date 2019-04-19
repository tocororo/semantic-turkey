package it.uniroma2.art.semanticturkey.changetracking.sail.config;

import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.DelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

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

		String serverURL = config2.getServerURL();
		String metadataRepoId = config2.getSupportRepositoryID();
		String metadataNS = config2.getMetadataNS();
		IRI metadataGraph = config2.getHistoryGraph();
		Set<IRI> includeGraph = config2.getIncludeGraph();
		Set<IRI> excludeGraph = config2.getExcludeGraph();
		boolean historyEnabled = config2.isHistoryEnabled();
		boolean validationEnabled = config2.isValidationEnabled();
		boolean blacklistEnabled = config2.isBlacklistingEnabled();
		Optional<Boolean> interactiveNotifications = config2.isInteractiveNotifications();
		IRI validationGraph = config2.getValidationGraph();
		IRI blacklistGraph = config2.getBlacklistGraph();
		SailImplConfig config3 = config;

		if (!interactiveNotifications.isPresent()) {
			while (config3 instanceof DelegatingSailImplConfig) {
				config3 = ((DelegatingSailImplConfig) config).getDelegate();

				String type = config3.getType();
				if (type.equals("graphdb:FreeSail") || type.equals("owlim:Sail")) {
					interactiveNotifications = ChangeTracker.OPTIONAL_FALSE;
					break;
				} else if (type.equals("openrdf:MemoryStore") || type.equals("openrdf:NativeStore")) {
					interactiveNotifications = ChangeTracker.OPTIONAL_TRUE;
					break;
				}
			}
		}

		return new ChangeTracker(serverURL, metadataRepoId, metadataNS, metadataGraph, includeGraph,
				excludeGraph, historyEnabled, validationEnabled, interactiveNotifications, validationGraph,
				blacklistEnabled, blacklistGraph);
	}

}
