package it.uniroma2.art.semanticturkey.config.sparql;

import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * A storage for SPARQL operations (i.e. queries and updates).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class SPARQLStore implements PUScopedConfigurableComponent<StoredSPARQLOperation> {

	@Override
	public String getId() {
		return SPARQLStore.class.getName();
	}

}
