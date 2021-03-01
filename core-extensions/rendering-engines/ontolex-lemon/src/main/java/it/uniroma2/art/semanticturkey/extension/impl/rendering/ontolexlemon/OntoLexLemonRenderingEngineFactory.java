package it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link OntoLexLemonRenderingEngine}.
 */
public class OntoLexLemonRenderingEngineFactory implements
		ConfigurableExtensionFactory<OntoLexLemonRenderingEngine, OntoLexLemonRenderingEngineConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon.OntoLexLemonRenderingEngineFactory";
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
	public OntoLexLemonRenderingEngine createInstance(OntoLexLemonRenderingEngineConfiguration conf)
			throws InvalidConfigurationException {
		return new OntoLexLemonRenderingEngine(conf);
	}

	@Override
	public Collection<OntoLexLemonRenderingEngineConfiguration> getConfigurations() {
		return Arrays.asList(new OntoLexLemonRenderingEngineConfiguration());
	}
}
