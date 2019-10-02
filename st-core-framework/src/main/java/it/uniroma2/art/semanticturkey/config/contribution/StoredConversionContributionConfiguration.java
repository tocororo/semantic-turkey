package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public class StoredConversionContributionConfiguration extends StoredContributionConfiguration {

	@Override
	public String getShortName() {
		return "Stored Conversion Contribution";
	}

	@STProperty(description = "Input format of the resource to convert", displayName = "Format")
	@Required
	public String format;

	@STProperty(description = "Name of the resource", displayName = "Name")
	@Required
	public String resourceName;

	@STProperty(description = "Homepage URL of the resource", displayName = "Homepage")
	@Required
	public IRI homepage;

	@STProperty(description = "Description of the resource", displayName = "Description")
	@Required
	public String description;

	@STProperty(description = "Tells if the contributor is the owner of the resource", displayName = "Is owner")
	@Required
	public boolean isOwner;

	@STProperty(description = "Semantic model of the resource", displayName = "Semantic model")
	@Required
	public IRI model;

	@STProperty(description = "Lexicalizaion model of the resource", displayName = "Lexicalization model")
	@Required
	public IRI lexicalizationModel;

}
