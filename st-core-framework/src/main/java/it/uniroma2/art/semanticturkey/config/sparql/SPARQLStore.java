package it.uniroma2.art.semanticturkey.config.sparql;

import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

public class SPARQLStore implements PUScopedConfigurableComponent<StoredSPARQLOperation> {

	@Override
	public String getId() {
		return SPARQLStore.class.getName();
	}

}
