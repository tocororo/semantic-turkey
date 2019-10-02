package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public abstract class StoredContributionConfiguration implements Configuration {

	@STProperty(description = "Name of the contributor", displayName = "Name")
	@Required
	public String name;

	@STProperty(description = "Last name of the contributor", displayName = "Last name")
	@Required
	public String lastName;

	@STProperty(description = "Email of the contributor", displayName = "Email")
	@Required
	public String email;

	@STProperty(description = "Organization of the contributor", displayName = "Organization")
	public String organization;

	@STProperty(description = "BaseURI of the resource", displayName = "BaseURI")
	@Required
	public IRI baseURI;

}
