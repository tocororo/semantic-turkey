package it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link OntoLexLemonRenderingEngine}.
 */
public class OntoLexLemonRenderingEngineFactory implements NonConfigurableExtensionFactory<OntoLexLemonRenderingEngine>,
		PUSettingsManager<OntoLexLemonRenderingEnginePUSettings> {

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
	public OntoLexLemonRenderingEngine createInstance() {
		return new OntoLexLemonRenderingEngine(this);
	}
}
