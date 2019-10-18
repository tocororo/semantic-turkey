package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public class StoredStableResourceContributionConfiguration extends StoredContributionConfiguration {

	@Override
	public String getShortName() {
		return "Stored RDF dataset Contribution";
	}

	@STProperty(description = "Homepage URL of the resource", displayName = "Homepage")
	public String homepage;

	@STProperty(description = "Description of the resource", displayName = "Description")
	@Required
	public String description;

	@STProperty(description = "Tells if the contributor is the owner of the resource", displayName = "Is owner")
	public boolean isOwner;

	@STProperty(description = "Semantic model of the resource", displayName = "Semantic model")
	@Required
	public IRI model;

	@STProperty(description = "Lexicalizaion model of the resource", displayName = "Lexicalization model")
	@Required
	public IRI lexicalizationModel;

	//TODO other fields resulting the discovering of the baseURI

}
