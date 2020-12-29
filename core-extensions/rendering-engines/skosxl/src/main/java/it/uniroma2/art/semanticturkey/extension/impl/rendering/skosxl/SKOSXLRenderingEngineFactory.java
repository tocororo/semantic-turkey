package it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link SKOSXLRenderingEngine}.
 */
public class SKOSXLRenderingEngineFactory implements NonConfigurableExtensionFactory<SKOSXLRenderingEngine>,
		PUSettingsManager<SKOSXLRenderingEnginePUSettings> {

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
	public SKOSXLRenderingEngine createInstance() {
		return new SKOSXLRenderingEngine(this);
	}
}
