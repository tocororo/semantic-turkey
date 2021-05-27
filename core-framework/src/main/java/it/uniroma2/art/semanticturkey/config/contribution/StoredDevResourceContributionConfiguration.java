package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.showvoc.ShowVocConversionFormat;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public class StoredDevResourceContributionConfiguration extends StoredContributionConfiguration {

	public String getContributionTypeLabel() {
		return "Development";
	}

	@Override
	public String getShortName() {
		return "Stored Conversion Contribution";
	}

	@STProperty(description = "Input format of the resource to convert", displayName = "Format")
	public ShowVocConversionFormat format; //if not provided, the contribution does not require conversion, the data is already RDF

	@STProperty(description = "Homepage URL of the resource", displayName = "Homepage")
	public String homepage;

	@STProperty(description = "Description of the resource", displayName = "Description")
	@Required
	public String description;

	@STProperty(description = "Semantic model of the resource", displayName = "Semantic model")
	@Required
	public IRI model;

	@STProperty(description = "Lexicalizaion model of the resource", displayName = "Lexicalization model")
	@Required
	public IRI lexicalizationModel;

}
