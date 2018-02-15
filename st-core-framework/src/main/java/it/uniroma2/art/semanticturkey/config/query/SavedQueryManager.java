package it.uniroma2.art.semanticturkey.config.query;

import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

public class SavedQueryManager implements PUScopedConfigurableComponent<SavedQuery> {

	@Override
	public String getId() {
		return SavedQueryManager.class.getName();
	}

}
