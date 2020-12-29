package it.uniroma2.art.semanticturkey.extension.impl.rendering.skos;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link SKOSRenderingEngine}.
 */
public class SKOSRenderingEngineFactory
		implements NonConfigurableExtensionFactory<SKOSRenderingEngine>, PUSettingsManager<SKOSRenderingEnginePUSettings> {

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
	public SKOSRenderingEngine createInstance() {
		return new SKOSRenderingEngine(this);
	}
	
	
}
