package it.uniroma2.art.semanticturkey.mdr.bindings;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredProjectMetadata implements Settings {
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.mdr.bindings.StoredProjectMetadata";

		public static final String shortName = keyBase + ".shortName";
		public static final String datasetDescription$description = keyBase
				+ ".datasetDescription.description";
		public static final String datasetDescription$displayName = keyBase
				+ ".datasetDescription.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.datasetDescription$description + "}", displayName = "{"
			+ MessageKeys.datasetDescription$displayName + "}")
	@Required
	public Pair<IRI, String> datasetDescription;

}
