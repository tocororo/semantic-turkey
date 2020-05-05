package it.uniroma2.art.semanticturkey.settings.metadata;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredProjectMetadata implements Settings {

	@Override
	public String getShortName() {
		return "Stored Project Metadata";
	}

	@STProperty(description = "A pair consisting of an IRI uniquely identifying the dataset associated with the proeject together with its description encoded in the Turtle syntax", displayName = "Dataset Description")
	@Required
	public Pair<IRI, String> datasetDescription;

}
