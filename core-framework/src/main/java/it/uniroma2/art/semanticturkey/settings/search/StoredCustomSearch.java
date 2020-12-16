package it.uniroma2.art.semanticturkey.settings.search;

import java.util.Set;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredCustomSearch implements Settings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.search.StoredCustomSearch";

		public static final String shortName = keyBase + ".shortName";
		public static final String searchSPARQLParameterizationReferences$description = keyBase
				+ ".searchSPARQLParameterizationReferences.description";
		public static final String searchSPARQLParameterizationReferences$displayName = keyBase
				+ ".searchSPARQLParameterizationReferences.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.searchSPARQLParameterizationReferences$description
			+ "}", displayName = "{" + MessageKeys.searchSPARQLParameterizationReferences$displayName + "}")
	@Required
	public Set<String> searchSPARQLParameterizationReferences;
}
