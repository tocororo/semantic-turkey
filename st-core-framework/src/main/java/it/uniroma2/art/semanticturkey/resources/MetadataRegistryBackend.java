package it.uniroma2.art.semanticturkey.resources;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.maple.orchestration.AssessmentException;

/**
 * A registry collecting metadata about RDF Datasets.
 * 
 */
public interface MetadataRegistryBackend {

	/**
	 * Adds a abstract version of a void:Dataset together with the dcat:CatalogRecord.
	 * 
	 * @param dataset
	 *            if {@code null} passed, a local IRI is created
	 * @param uriSpace
	 * @param title
	 * @param dereferenceable
	 *            if {@code true}, set to {@code mdreg:standardDereferenciation}; if {@code false}, set to
	 *            {@code mdreg:noDereferenciation}
	 * @param sparqlEndpoint
	 * @return the IRI of the dcat:CatalogRecord created for it
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	IRI addDataset(IRI dataset, String uriSpace, String title, Boolean dereferenceable, IRI sparqlEndpoint)
			throws IllegalArgumentException, MetadataRegistryWritingException;

	/**
	 * Adds {@code dataset} to the specified {@code catalogRecord} as a specific {@code versionInfo}.
	 * 
	 * @param catalogRecord
	 * @param dataset
	 *            if not {@code null}, a local IRI is created
	 * @param versionInfo
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	void addDatasetVersion(IRI catalogRecord, IRI dataset, String versionInfo)
			throws IllegalArgumentException, IOException;

	/**
	 * Sets whether a dataset is derefereanceable or not. If {@code value} is {@code true}, then sets
	 * {@code mdreg:standardDereferenciation} and if {@code false} sets {@code mdreg:noDereferenciation}
	 * 
	 * @param dataset
	 * @param value
	 *            if {@code null}, the dereferenciability is left unspecified
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	void setDereferenciability(IRI dataset, Boolean value) throws IllegalArgumentException, IOException;

	/**
	 * Sets the SPARQL endpoint of a dataset.
	 * 
	 * @param dataset
	 * @param endpoint
	 *            if {@code null}, the endpoint is left unspecified
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	void setSPARQLEndpoint(IRI dataset, IRI endpoint) throws IllegalArgumentException, IOException;

	Collection<CatalogRecord> getCatalogRecords();

	/**
	 * Returns the metadata associated with the given dataset
	 * 
	 * @param dataset
	 * @return
	 */
	DatasetMetadata getDatasetMetadata(IRI dataset);

	/**
	 * Returns metadata about the dataset identified by the given URI. If no dataset is found, then the method
	 * returns <code>null</code>.
	 * 
	 * @param uriResource
	 * @return
	 */
	DatasetMetadata findDatasetForResource(IRI iriResource);

	/**
	 * Discover the metadata for a dataset given an IRI.
	 * 
	 * @param iri
	 * @return
	 * @return The newly created dcat:CatalogRecord for the discovered dataset
	 * @throws MetadataDiscoveryException
	 */
	IRI discoverDataset(IRI iri) throws MetadataDiscoveryException;

	/**
	 * Assess the lexicalization model of the given {@code dataset}
	 * 
	 * @param dataset
	 * @throws MetadataDiscoveryException
	 */
	void assessLexicalizationModel(IRI dataset) throws AssessmentException;

	RepositoryConnection getConnection();

}