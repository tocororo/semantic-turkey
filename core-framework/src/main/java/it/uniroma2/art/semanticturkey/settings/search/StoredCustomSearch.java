package it.uniroma2.art.semanticturkey.settings.search;

import java.util.Set;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredCustomSearch implements Settings {

	@Override
	public String getShortName() {
		return "Stored Custom Search";
	}

	@STProperty(description = "A set of relative references to stored parameterizations of SPARQL queries that can be used for custom search", displayName = "Search SPARQL parameterization references")
	@Required
	public Set<String> searchSPARQLParameterizationReferences;
}
