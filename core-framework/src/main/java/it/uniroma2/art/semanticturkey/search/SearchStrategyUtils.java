package it.uniroma2.art.semanticturkey.search;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;

public abstract class SearchStrategyUtils {
	public static SearchStrategy instantiateSearchStrategy(ExtensionPointManager exptManager,
			SearchStrategies searchStrategy) {
		String factoryId;
		switch (searchStrategy) {
		case REGEX:
			factoryId = "it.uniroma2.art.semanticturkey.extension.impl.search.regex.RegexSearchStrategy";
			break;
		case GRAPH_DB:
			factoryId = "it.uniroma2.art.semanticturkey.extension.impl.search.graphdb.GraphDBSearchStrategy";
			break;
		default:
			throw new IllegalStateException("Unsupported search stategy: " + searchStrategy);
		}

		PluginSpecification spec = new PluginSpecification(factoryId, null, null, null);
		try {
			return exptManager.instantiateExtension(SearchStrategy.class, spec);
		} catch (IllegalArgumentException | NoSuchExtensionException | WrongPropertiesException
				| STPropertyAccessException | InvalidConfigurationException e) {
			throw new RuntimeException("A problem occurred during the instantiation of a SearchStrategy", e);
		}
	}
}
