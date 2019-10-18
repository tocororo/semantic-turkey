package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public abstract class StoredContributionConfiguration implements Configuration {

	@STProperty(description = "Name of the contributor", displayName = "Name")
	@Required
	public String contributorName;

	@STProperty(description = "Last name of the contributor", displayName = "Last name")
	@Required
	public String contributorLastName;

	@STProperty(description = "Email of the contributor", displayName = "Email")
	@Required
	public String contributorEmail;

	@STProperty(description = "Organization of the contributor", displayName = "Organization")
	public String contributorOrganization;

	@STProperty(description = "Name of the resource", displayName = "Name")
	@Required
	public String resourceName;

	@STProperty(description = "BaseURI of the resource", displayName = "BaseURI")
	@Required
	public IRI baseURI;

}
