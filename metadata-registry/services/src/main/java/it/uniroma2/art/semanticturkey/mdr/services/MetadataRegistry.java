package it.uniroma2.art.semanticturkey.mdr.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniroma2.art.semanticturkey.mdr.core.AbstractDatasetAttachment;
import it.uniroma2.art.semanticturkey.mdr.core.CatalogRecord2;
import it.uniroma2.art.semanticturkey.mdr.core.Distribution;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.semanticturkey.data.access.ResourceLocator;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.mdr.core.CatalogRecord;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.LexicalizationSetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.LinksetMetadata;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataDiscoveryException;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryStateException;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.mdr.core.NoSuchCatalogRecordException;
import it.uniroma2.art.semanticturkey.mdr.core.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.project.Project;
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

	private STMetadataRegistryBackend metadataRegistryBackend;

	private ResourceLocator resourceLocator;

	@Autowired
	public void setMetadataRegistry(STMetadataRegistryBackend metadataRegistryBackend) {
		this.metadataRegistryBackend = metadataRegistryBackend;
	}

	@Autowired
	public void setResourceLocator(ResourceLocator resourceLocator) {
		this.resourceLocator = resourceLocator;
	}

	/**
	 * Creates a new abstract dataset.
	 *
	 * @param datasetLocalName  if {@code null} passed, a named is generated for the dataset
	 * @param uriSpace the <em>current</em> URI space of the dataset, as its concrete distributions may introduce futher
	 *                 ones
	 * @param title the <em>current</em> title of the dataset
	 * @param description the <em>current</em> description of the dataset
	 * @return the IRI of the newly created dataset
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public AnnotatedValue<IRI> createAbstractDataset(String datasetLocalName,
													 String uriSpace,
													 @Optional Literal title,
													 @Optional Literal description)
			throws MetadataRegistryWritingException {
		IRI datasetIRI = metadataRegistryBackend.createAbstractDataset(datasetLocalName, uriSpace, title, description);
		return new AnnotatedValue<>(datasetIRI);
	}

	/**
	 * Creates a new concrete dataset.
	 *
	 * @param datasetLocalName  if {@code null} passed, a named is generated for the dataset
	 * @param uriSpace the <em>current</em> URI space of the dataset, as its concrete distributions may introduce futher
	 *                 ones
	 * @param title the <em>current</em> title of the dataset
	 * @param description the <em>current</em> description of the dataset
	 * @param dereferenceable whether the dataset is dereferenceable: <code>null</code> means unknown
	 * @param distribution the distribution associated with the dataset, which also determines the dataset nature
	 * @param abstractDatasetAttachment optional connection to an abstract dataset
	 * @return the IRI of the newly created dataset
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public AnnotatedValue<IRI> createConcreteDataset(String datasetLocalName,
													 String uriSpace,
													 @Optional Literal title,
													 @Optional Literal description,
													 @Optional Boolean dereferenceable,
													 @Optional @JsonSerialized Distribution distribution,
													 @Optional @JsonSerialized AbstractDatasetAttachment abstractDatasetAttachment)
			throws MetadataRegistryWritingException {
		IRI datasetIRI = metadataRegistryBackend.createConcreteDataset(
				datasetLocalName,
				uriSpace,
				title,
				description,
				dereferenceable,
				distribution,
				abstractDatasetAttachment);
		return new AnnotatedValue<>(datasetIRI);
	}


	/**
	 * Returns root datasets, i.e. datasets (either abstract or concrete) that are not connect to another (abstract)
	 * dataset (e.g. as a version, master copy or LOD version thereof)
	 * @return a collection of records for root datasets
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Collection<CatalogRecord2> listRootDatasets() {
		return metadataRegistryBackend.listRootDatasets();
	}

	/**
	 * Returns the datasets connected to a given abstract dataset, e.g. as a version, master copy or LOD version thereof
	 *
	 * @param abstractDataset an abstract dataset
	 * @return a collection of datasets connected to the given abstract dataset
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Collection<CatalogRecord2> listConnectedDatasets(IRI abstractDataset) {
		return metadataRegistryBackend.listConnectedDatasets(abstractDataset);
	}

	/**
	 * Adds a abstract version of a void:DatasetSpecification together with the dcat:CatalogRecord.
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
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public void addDatasetVersion(IRI catalogRecord, @Optional IRI dataset, String versionInfo)
			throws IllegalArgumentException, MetadataRegistryWritingException {
		metadataRegistryBackend.addDatasetVersion(catalogRecord, dataset, versionInfo);
	}

	/**
	 * Sets whether a dataset is derefereanceable or not. If {@code value} is {@code true}, then sets
	 * {@code mdreg:standardDereferenciation} and if {@code false} sets {@code mdreg:noDereferenciation}. If
	 * {@value} is not passed, the dereferenciation system is left unspecified.
	 * 
	 * @param dataset
	 * @param value
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void setDereferenciability(IRI dataset, @Optional Boolean value)
			throws IllegalArgumentException, MetadataRegistryWritingException {
		metadataRegistryBackend.setDereferenciability(dataset, value);
	}

	/**
	 * Sets the SPARQL endpoint of a dataset.
	 * 
	 * @param dataset
	 * @param endpoint
	 *            if {@code null}, the endpoint is left unspecified
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void setSPARQLEndpoint(IRI dataset, @Optional IRI endpoint)
			throws IllegalArgumentException, MetadataRegistryWritingException {
		metadataRegistryBackend.setSPARQLEndpoint(dataset, endpoint);
	}

	/**
	 * Sets the title of a dataset.
	 * 
	 * @param dataset
	 * @param title
	 *            if {@code null}, the title is left unspecified
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void setTitle(IRI dataset, @Optional String title)
			throws IllegalArgumentException, MetadataRegistryWritingException {
		metadataRegistryBackend.setTitle(dataset, title);
	}

	/**
	 * Sets the given <em>limitation</em> for the provided <em>endpoint</em>
	 * 
	 * @param endpoint
	 * @param limitation
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void setSPARQLEndpointLimitation(IRI endpoint, IRI limitation)
			throws MetadataRegistryWritingException {
		metadataRegistryBackend.setSPARQLEndpointLimitation(endpoint, limitation);
	}

	/**
	 * Removes the given <em>limitation</em> from the provided <em>endpoint</em>
	 * 
	 * @param endpoint
	 * @param limitation
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void removeSPARQLEndpointLimitation(IRI endpoint, IRI limitation)
			throws MetadataRegistryWritingException {
		metadataRegistryBackend.removeSPARQLEndpointLimitation(endpoint, limitation);
	}

	/**
	 * Returns the limitations associated with the provided <em>endpoint</em>
	 * 
	 * @param endpoint
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Collection<IRI> getSPARQLEndpointLimitations(IRI endpoint) {
		return metadataRegistryBackend.getSPARQLEndpointLimitations(endpoint);
	}

	/**
	 * Deletes a catalog record
	 * 
	 * @param catalogRecord
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'D')")
	public void deleteCatalogRecord(IRI catalogRecord) throws MetadataRegistryWritingException {
		metadataRegistryBackend.deleteCatalogRecord(catalogRecord);
	}

	/**
	 * Deletes a dataset version
	 * 
	 * @param dataset
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'D')")
	public void deleteDatasetVersion(IRI dataset) throws MetadataRegistryWritingException {
		metadataRegistryBackend.deleteDatasetVersion(dataset);
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
	 * Returns a catalog record
	 * 
	 * @throws NoSuchCatalogRecordException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public CatalogRecord getCatalogRecord(IRI catalogRecord) throws NoSuchCatalogRecordException {
		return metadataRegistryBackend.getCatalogRecord(catalogRecord);
	}

	/**
	 * Returns metadata about a given dataset
	 * 
	 * @throws MetadataRegistryStateException
	 * @throws NoSuchDatasetMetadataException
	 */
	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')") //project wide service, could be
	// invoked without a ctx_project, so without capabililties
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
	@STServiceOperation(method = RequestMethod.POST)
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
	@STServiceOperation(method = RequestMethod.POST)
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
	 * Returns metadata about the linksets sets embedded in a given dataset
	 * 
	 * @param dataset
	 * @param treshold
	 *            minimum number of links (before linkset coalescing)
	 * @param coalesce
	 *            whether or not merge linksets for the same pair of datasets
	 * @return
	 */
	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')") //project wide service, could be
	// invoked without a ctx_project, so without capabililties
	public Collection<LinksetMetadata> getEmbeddedLinksets(IRI dataset,
			@Optional(defaultValue = "0") long treshold, @Optional(defaultValue = "false") boolean coalesce) {
		return metadataRegistryBackend.getEmbeddedLinksets(dataset, treshold, coalesce);
	}

	/**
	 * Consults the dataset (in the best possible way going from more to less noble availabilities:
	 * localProject --> SPARQLendpoint --> http-dereferenciation) in order to assess its lexicalization model.
	 * 
	 * @param dataset
	 * @return the lexicalization model
	 * @throws AssessmentException
	 * @throws MetadataRegistryWritingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void assessLexicalizationModel(IRI dataset)
			throws AssessmentException, MetadataRegistryWritingException {
		metadataRegistryBackend.discoverLexicalizationSets(dataset);
	}

	/**
	 * Get lexicalization model
	 * 
	 * @param dataset
	 * @return the lexicalization model or <code>null</code> if it can't be determined
	 */
	@STServiceOperation(method = RequestMethod.GET)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public IRI getComputedLexicalizationModel(IRI dataset)
			throws AssessmentException, MetadataRegistryWritingException {
		return metadataRegistryBackend.getComputedLexicalizationModel(dataset).orElse(null);
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
	@STServiceOperation
	public DatasetMetadata discoverDatasetMetadata(IRI iri)
			throws ProjectAccessException, DeniedOperationException, MetadataDiscoveryException {
		DatasetMetadata datasetMeta = metadataRegistryBackend.findDatasetForResource(iri);

		if (datasetMeta != null) {
			throw new DatasetAlreadyCatalogedException(iri, datasetMeta.getIdentity());
		}

		return metadataRegistryBackend.discoverDatasetMetadata(iri);
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
		DatasetMetadata datasetMeta = metadataRegistryBackend.findDatasetForResource(iri);

		if (datasetMeta != null) {
			throw new DatasetAlreadyCatalogedException(iri, datasetMeta.getIdentity());
		}

		IRI catalogRecord = metadataRegistryBackend.discoverDataset(iri);

		return new AnnotatedValue<>(catalogRecord);
	}

	/**
	 * Returns the datasets associated with the given projects, if any.
	 * 
	 * @param projects
	 * @return
	 */
	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')") //project wide service, could be
	// invoked without a ctx_project, so without capabililties
	public Map<String, AnnotatedValue<IRI>> findDatasetForProjects(List<Project> projects) {
		Map<String, AnnotatedValue<IRI>> rv = new HashMap<>(projects.size());
		for (Project proj : projects) {
			IRI dataset = metadataRegistryBackend.findDatasetForProject(proj);
			if (dataset != null) {
				rv.put(proj.getName(), new AnnotatedValue<>(dataset));
			}
		}
		return rv;
	}

	/**
	 * Returns the datasets associated with the given projects, if any.
	 * 
	 * @param datasets
	 * @param allowSubset
	 * @return
	 */
	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')") //project wide service, could be
	// invoked without a ctx_project, so without capabililties
	public Map<IRI, String> findProjectForDatasets(List<IRI> datasets, @Optional boolean allowSubset) {
		Map<IRI, String> rv = new HashMap<>();
		for (IRI dataset : datasets) {
			Project p = metadataRegistryBackend.findProjectForDataset(dataset, allowSubset);
			if (p != null) {
				rv.put(dataset, p.getName());
			}
		}
		return rv;
	}
}
