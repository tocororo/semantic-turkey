package it.uniroma2.art.semanticturkey.extension.impl.rendering.skos;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link SKOSRenderingEngine}.
 */
public class SKOSRenderingEngineFactory
		implements ConfigurableExtensionFactory<SKOSRenderingEngine, SKOSRenderingEngineConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skos.SKOSRenderingEngineFactory";
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
	public SKOSRenderingEngine createInstance(SKOSRenderingEngineConfiguration conf)
			throws InvalidConfigurationException {
		return new SKOSRenderingEngine(conf);
	}

	@Override
	public Collection<SKOSRenderingEngineConfiguration> getConfigurations() {
		return Arrays.asList(new SKOSRenderingEngineConfiguration());
	}

	
}
