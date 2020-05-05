package it.uniroma2.art.semanticturkey.config.sparql;

import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * A storage for parameterizations of stored SPARQL Operations (see: {@link SPARQLStore}).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * 
 */
public class SPARQLParameterizationStore implements PUScopedConfigurableComponent<StoredSPARQLParameterization> {

	@Override
	public String getId() {
		return SPARQLParameterizationStore.class.getName();
	}

}
