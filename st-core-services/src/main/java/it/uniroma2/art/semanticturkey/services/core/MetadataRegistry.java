package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.UnknownResourcePosition;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.resources.CatalogRecord;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.LexicalizationSetMetadata;
import it.uniroma2.art.semanticturkey.resources.MetadataDiscoveryException;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryStateException;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.resources.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This service class allows the management of the metadata about remote datasets.
 */
@STService
public class MetadataRegistry extends STServiceAdapter {

	private MetadataRegistryBackend metadataRegistryBackend;

	private ResourceLocator resourceLocator;

	@Autowired
	public void setMetadataRegistry(MetadataRegistryBackend metadataRegistryBackend) {
		this.metadataRegistryBackend = metadataRegistryBackend;
	}

	@Autowired
	public void setResourceLocator(ResourceLocator resourceLocator) {
		this.resourceLocator = resourceLocator;
	}

	/**
	 * Adds a abstract version of a void:Dataset together with the dcat:CatalogRecord.
	 * 
	 * @param dataset
	 *            if not passed, a local IRI is created
	 * @param uriSpace
	 * @param title
	 * @param dereferenceable
	 *            if {@code true}, set to {@code mdreg:standardDereferenciation}; if {@code false}, set to
	 *            {@code mdreg:noDereferenciation}
	 * @param sparqlEndpoint
	 * @return the IRI of the dcat:CatalogRecord created for it
	 * @throws MetadataRegistryStateException
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public AnnotatedValue<IRI> addDataset(@Optional IRI dataset, String uriSpace, @Optional String title,
			@Optional Boolean dereferenceable, @Optional IRI sparqlEndpoint) throws IllegalArgumentException,
			MetadataRegistryStateException, MetadataRegistryWritingException {
		IRI catalogRecord = metadataRegistryBackend.addDataset(dataset, uriSpace, title, dereferenceable,
				sparqlEndpoint);
		return new AnnotatedValue<>(catalogRecord);
	}

	/**
	 * Adds {@code dataset} to the specified {@code catalogRecord} as a specific {@code versionInfo}.
	 * 
	 * @param catalogRecord
	 * @param dataset
	 *            if not passed, a local IRI is created
	 * @param versionInfo
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public void addDatasetVersion(IRI catalogRecord, @Optional IRI dataset, String versionInfo)
			throws IllegalArgumentException, IOException {
		metadataRegistryBackend.addDatasetVersion(catalogRecord, dataset, versionInfo);
	}

	/**
	 * Sets whether a dataset is derefereanceable or not. If {@code value} is {@code true}, then sets
	 * {@code mdreg:standardDereferenciation} and if {@code false} sets {@code mdreg:noDereferenciation}. If
	 * {@value} is not passed, the dereferenciation system is left unspecified.
	 * 
	 * @param dataset
	 * @param value
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void setDereferenciability(IRI dataset, @Optional Boolean value)
			throws IllegalArgumentException, IOException {
		metadataRegistryBackend.setDereferenciability(dataset, value);
	}

	/**
	 * Sets the SPARQL endpoint of a dataset.
	 * 
	 * @param dataset
	 * @param endpoint
	 *            if {@code null}, the endpoint is left unspecified
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void setSPARQLEndpoint(IRI dataset, @Optional IRI endpoint)
			throws IllegalArgumentException, IOException {
		metadataRegistryBackend.setSPARQLEndpoint(dataset, endpoint);
	}

	/**
	 * Deletes a catalog record
	 * 
	 * @param catalogRecord
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'D')")
	public void deleteCatalogRecord(IRI catalogRecord) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Deletes a dataset version
	 * 
	 * @param dataset
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'D')")
	public void deleteDatasetVersions(IRI dataset) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the catalog records
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Collection<CatalogRecord> getCatalogRecords() {
		return metadataRegistryBackend.getCatalogRecords();
	}

	/**
	 * Returns metadata about a given dataset
	 * 
	 * @throws MetadataRegistryStateException
	 * @throws NoSuchDatasetMetadataException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public DatasetMetadata getDatasetMetadata(IRI dataset)
			throws NoSuchDatasetMetadataException, MetadataRegistryStateException {
		return metadataRegistryBackend.getDatasetMetadata(dataset);
	}

	/**
	 * Adds an embedded lexicalization set for a dataset.
	 * 
	 * @param dataset
	 * @param lexicalizationSet
	 *            if {@code null} is passed, a local IRI is created
	 * @param lexiconDataset
	 * @param lexicalizationModel
	 * @param language
	 * @param references
	 * @param lexicalEntries
	 * @param lexicalizations
	 * @param percentage
	 * @param avgNumOfLexicalizations
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public void addEmbeddedLexicalizationSet(IRI dataset, @Optional IRI lexicalizationSet,
			@Optional IRI lexiconDataset, IRI lexicalizationModel, String language,
			@Optional BigInteger references, @Optional BigInteger lexicalEntries,
			@Optional BigInteger lexicalizations, @Optional BigDecimal percentage,
			@Optional BigDecimal avgNumOfLexicalizations) throws MetadataRegistryWritingException {
		metadataRegistryBackend.addEmbeddedLexicalizationSet(dataset, lexicalizationSet, lexiconDataset,
				lexicalizationModel, language, references, lexicalEntries, lexicalizations, percentage,
				avgNumOfLexicalizations);
	}

	/**
	 * Delete an embedded lexicalization set
	 * 
	 * @param lexicalizationSet
	 * @throws MetadataRegistryWritingException
	 * @throws MetadataRegistryStateException 
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'D')")
	public void deleteEmbeddedLexicalizationSet(IRI lexicalizationSet)
			throws MetadataRegistryWritingException, MetadataRegistryStateException {
		metadataRegistryBackend.deleteEmbeddedLexicalizationSet(lexicalizationSet);
	}

	/**
	 * Returns metadata about the lexicalization sets embedded in a given dataset
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Collection<LexicalizationSetMetadata> getEmbeddedLexicalizationSets(IRI dataset) {
		return metadataRegistryBackend.getEmbeddedLexicalizationSets(dataset);
	}

	/**
	 * Consults the dataset (in the best possible way going from more to less noble availabilities:
	 * localProject --> SPARQLendpoint --> http-dereferenciation) in order to assess its lexicalization model.
	 * 
	 * @param dataset
	 * @return the lexicalization model
	 * @throws AssessmentException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void assessLexicalizationModel(IRI dataset) throws AssessmentException {
		metadataRegistryBackend.assessLexicalizationModel(dataset);
	}

	/**
	 * Find a dataset matching the given IRI.
	 * 
	 * @param iri
	 * @return
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public ResourcePosition findDataset(IRI iri) throws ProjectAccessException {
		return resourceLocator.locateResource(getProject(), getRepository(), iri);
	}

	/**
	 * Discover the metadata for a dataset given an IRI. If discovery is unsuccessful, an exception is thrown.
	 * 
	 * @param iri
	 * @return the newly created dcat:CatalogRecord for the discovered dataset
	 * @throws ProjectAccessException
	 * @throws DeniedOperationException
	 * @throws MetadataDiscoveryException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public AnnotatedValue<IRI> discoverDataset(IRI iri)
			throws ProjectAccessException, DeniedOperationException, MetadataDiscoveryException {
		ResourcePosition rp = resourceLocator.locateResource(getProject(), getRepository(), iri);

		if (!(rp instanceof UnknownResourcePosition)) {
			throw new DeniedOperationException("A position for the provided IRI " + RenderUtils.toSPARQL(iri)
					+ " is already known: " + rp.getPosition());
		}

		IRI catalogRecord = metadataRegistryBackend.discoverDataset(iri);

		return new AnnotatedValue<>(catalogRecord);
	}

}
