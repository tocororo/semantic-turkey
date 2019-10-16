package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class StoredMetadataContributionConfiguration extends StoredContributionConfiguration {

	@Override
	public String getShortName() {
		return "Stored Metadata Contribution";
	}

	@STProperty(description = "Name of the resource", displayName = "Name")
	@Required
	public String resourceName; //equivalent of title in it.uniroma2.art.semanticturkey.resources.DatasetMetadata

	@STProperty(description = "Name of the resource", displayName = "Name")
	@Required
	public IRI identity;

	@STProperty(description = "URI space", displayName = "URI space")
	public String uriSpace;

	/**
	 * YES => http://semanticturkey.uniroma2.it/ns/mdreg#standardDereferenciation
	 * NO => http://semanticturkey.uniroma2.it/ns/mdreg#noDereferenciation
	 */
	@STProperty(description = "Dereferenciation system", displayName = "Dereferenciation system")
	public IRI dereferenciationSystem;

	@STProperty(description = "SPARQL endpoint URL", displayName = "SPARQL endpoint")
	public IRI sparqlEndpoint;

	@STProperty(description = "SPARQL endpoint limitations", displayName = "SPARQL endpoint limitations")
	public Set<IRI> sparqlLimitations; //at the moment the only limitation is the aggregation yes/no

}
