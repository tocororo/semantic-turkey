package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.properties.STProperty;
import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class StoredMetadataContributionConfiguration extends StoredContributionConfiguration {

	@Override
	public String getShortName() {
		return "Stored Metadata Contribution";
	}

	@STProperty(description = "Identity", displayName = "Identity")
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
