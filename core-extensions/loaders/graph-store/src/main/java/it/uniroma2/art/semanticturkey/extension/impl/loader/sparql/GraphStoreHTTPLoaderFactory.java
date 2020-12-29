package it.uniroma2.art.semanticturkey.extension.impl.loader.sparql;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * The {@link ExtensionFactory} for the the {@link GraphStoreHTTPLoader}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class GraphStoreHTTPLoaderFactory implements ExtensionFactory<GraphStoreHTTPLoader>,
		ConfigurableExtensionFactory<GraphStoreHTTPLoader, GraphStoreHTTPLoaderConfiguration>,
		PUScopedConfigurableComponent<GraphStoreHTTPLoaderConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.loader.sparql.GraphStoreHTTPLoaderFactory";
		private static final String name = keyBase + ".name";
		private static final String description = keyBase + ".description";
	}

	@Override
	public String getName() {
		return STMessageSource.getMessage(MessageKeys.name);
	}

	@Override
	public String getDescription() {
		return STMessageSource.getMessage(MessageKeys.description);
	}

	@Override
	public GraphStoreHTTPLoader createInstance(GraphStoreHTTPLoaderConfiguration conf) {
		return new GraphStoreHTTPLoader(conf);
	}

	@Override
	public Collection<GraphStoreHTTPLoaderConfiguration> getConfigurations() {
		return Arrays.asList(new GraphStoreHTTPLoaderConfiguration());
	}

}
