package it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link SKOSXLRenderingEngine}.
 */
public class SKOSXLRenderingEngineFactory
		implements ConfigurableExtensionFactory<SKOSXLRenderingEngine, SKOSXLRenderingEngineConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl.SKOSXLRenderingEngineFactory";
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
	public SKOSXLRenderingEngine createInstance(SKOSXLRenderingEngineConfiguration conf)
			throws InvalidConfigurationException {
		return new SKOSXLRenderingEngine(conf);
	}

	@Override
	public Collection<SKOSXLRenderingEngineConfiguration> getConfigurations() {
		return Arrays.asList(new SKOSXLRenderingEngineConfiguration());
	}
}
