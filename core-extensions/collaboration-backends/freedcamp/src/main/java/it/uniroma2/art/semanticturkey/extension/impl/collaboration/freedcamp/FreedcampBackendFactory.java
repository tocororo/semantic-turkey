package it.uniroma2.art.semanticturkey.extension.impl.collaboration.freedcamp;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * Factory for the instantiation of {@link FreedcampBackend}.
 */
public class FreedcampBackendFactory implements NonConfigurableExtensionFactory<FreedcampBackend>,
		ProjectSettingsManager<FreedcampBackendProjectSettings>, PUSettingsManager<FreedcampBackendPUSettings> {

	@Override
	public String getName() {
		return "Freedcamp Backend";
	}

	@Override
	public String getDescription() {
		return "Use Freedcamp as a collaboration backend";
	}

	@Override
	public FreedcampBackend createInstance() {
		return new FreedcampBackend(this);
	}

}
