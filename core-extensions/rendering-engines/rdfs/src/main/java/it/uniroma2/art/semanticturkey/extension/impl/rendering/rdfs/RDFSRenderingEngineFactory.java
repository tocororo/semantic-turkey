package it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link RDFSRenderingEngine}.
 */
public class RDFSRenderingEngineFactory
		implements NonConfigurableExtensionFactory<RDFSRenderingEngine>, PUSettingsManager<RDFSRenderingEnginePUSettings> {

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
	public RDFSRenderingEngine createInstance() {
		return new RDFSRenderingEngine(this);
	}
}
