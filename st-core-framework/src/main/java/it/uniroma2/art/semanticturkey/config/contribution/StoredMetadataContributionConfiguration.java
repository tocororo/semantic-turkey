package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

public class StoredMetadataContributionConfiguration extends StoredContributionConfiguration {

	@Override
	public String getShortName() {
		return "Stored Metadata Contribution";
	}

	//TODO other fields resulting the discovering of the baseURI

	@STProperty(description = "Name of the resource", displayName = "Name")
	@Required
	public String resourceName;

	@STProperty(description = "URI space... (?)", displayName = "URI space")
	public String uriSpace;

	/**
	 * YES => http://semanticturkey.uniroma2.it/ns/mdreg#standardDereferenciation
	 */
	@STProperty(description = "", displayName = "Dereferenciation system")
	public IRI dereferenciationSystem;

	@STProperty(description = "SPARQL endpoint", displayName = "SPARQL endpoint")
	public IRI sparqlEndpoint;

	/**
	 * ?????
	 */
//	@STProperty(description = "", displayName = "")
//	public String versionInfo;

}
