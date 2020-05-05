package it.uniroma2.art.semanticturkey.config.sheet2rdf;

import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

public class AdvancedGraphApplicationStore implements PUScopedConfigurableComponent<StoredAdvancedGraphApplicationConfiguration> {

	@Override
	public String getId() {
		return AdvancedGraphApplicationStore.class.getName();
	}

}
