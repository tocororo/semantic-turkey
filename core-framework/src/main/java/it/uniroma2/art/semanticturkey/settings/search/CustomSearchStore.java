package it.uniroma2.art.semanticturkey.settings.search;

import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.SystemSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.UserSettingsManager;

/**
 * A storage for custom searches (based on parameterizations of stored SPARQL queries).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CustomSearchStore
		implements SystemSettingsManager<StoredCustomSearch>, ProjectSettingsManager<StoredCustomSearch>,
		UserSettingsManager<StoredCustomSearch>, PUSettingsManager<StoredCustomSearch> {

	@Override
	public String getId() {
		return CustomSearchStore.class.getName();
	}

}
