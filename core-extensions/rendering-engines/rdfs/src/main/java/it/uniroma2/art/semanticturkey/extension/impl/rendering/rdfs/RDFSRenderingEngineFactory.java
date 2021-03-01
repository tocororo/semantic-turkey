package it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link RDFSRenderingEngine}.
 */
public class RDFSRenderingEngineFactory
		implements ConfigurableExtensionFactory<RDFSRenderingEngine, RDFSRenderingEngineConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs.RDFSRenderingEngineFactory";
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
	public RDFSRenderingEngine createInstance(RDFSRenderingEngineConfiguration conf)
			throws InvalidConfigurationException {
		return new RDFSRenderingEngine(conf);
	}

	@Override
	public Collection<RDFSRenderingEngineConfiguration> getConfigurations() {
		return Arrays.asList(new RDFSRenderingEngineConfiguration());
	}

}
