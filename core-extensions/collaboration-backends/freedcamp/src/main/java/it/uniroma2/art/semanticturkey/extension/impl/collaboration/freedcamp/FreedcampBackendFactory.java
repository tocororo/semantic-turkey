package it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link FreedcampBackend}.
 */
public class FreedcampBackendFactory implements NonConfigurableExtensionFactory<FreedcampBackend>,
		ProjectSettingsManager<FreedcampBackendProjectSettings>, PUSettingsManager<FreedcampBackendPUSettings> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp.FreedcampBackendFactory";
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
	public FreedcampBackend createInstance() {
		return new FreedcampBackend(this);
	}

}
