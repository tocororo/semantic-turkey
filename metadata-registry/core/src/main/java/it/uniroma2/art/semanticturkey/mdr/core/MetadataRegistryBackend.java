package it.uniroma2.art.semanticturkey.mdr.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.maple.orchestration.AssessmentException;

/**
 * A registry collecting metadata about RDF Datasets.
 * 
 */
public interface MetadataRegistryBackend {

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
	 * @param renameOnConflict
	 * @return the IRI of the newly created dataset
	 */
	IRI createConcreteDataset(String datasetLocalName,
							  String uriSpace,
							  Literal title,
							  Literal description,
							  Boolean dereferenceable,
							  Distribution distribution,
							  AbstractDatasetAttachment abstractDatasetAttachment, boolean renameOnConflict) throws MetadataRegistryWritingException;

	/**
	 * Returns root datasets, i.e. datasets (either abstract or concrete) that are not connect to another (abstract)
	 * dataset (e.g. as a version, master copy or LOD version thereof)
	 * @return a collection of records for root datasets
	 */
	Collection<CatalogRecord2> listRootDatasets();

	/**
	 * Returns the datasets connected to a given abstract dataset, e.g. as a version, master copy or LOD version thereof
	 *
	 * @param abstractDataset an abstract dataset
	 * @return a collection of datasets connected to the given abstract dataset
	 */
	Collection<CatalogRecord2> listConnectedDatasets(IRI abstractDataset);

	/**
	 * Connect a root concrete dataset to an abstract dataset
	 * @param dataset
	 * @param abstractDatasetAttachment
	 */
	void connectToAbstractDataset(IRI dataset, AbstractDatasetAttachment abstractDatasetAttachment) throws MetadataRegistryWritingException;

	/**
	 * Disconnect a dataset from an abstract dataset
	 * @param dataset
	 * @param abstractDataset
	 */
	void disconnectFromAbstractDataset(IRI dataset, IRI abstractDataset) throws MetadataRegistryWritingException;

	/**
	 * Spawn a new abstract dataset grouping two existing root concrete datasets
	 * @param dataset1
	 * @param abstractDatasetAttachment1
	 * @param dataset2
	 * @param abstractDatasetAttachment2
	 * @param datasetLocalName
	 * @param uriSpace
	 * @param title
	 * @param description
	 * @param dereferenceable
	 * @return
	 */
	IRI spawnNewAbstractDataset(IRI dataset1,
								AbstractDatasetAttachment abstractDatasetAttachment1,
								IRI dataset2,
								AbstractDatasetAttachment abstractDatasetAttachment2,
								String datasetLocalName,
								String uriSpace,
								Literal title,
								Literal description, Boolean dereferenceable) throws MetadataRegistryWritingException;

	/**
	 * Returns metadata about a given catalog record
	 *
	 * @param catalogRecord a catalog record
	 * @return metadata about the provided catalog record or null if none is found
	 */
	CatalogRecord2 getCatalogRecordMetadata(IRI catalogRecord);

	/// --- OLD METHODS --- ///

	/**
	 * Deletes a dcat:CatalogRecord
	 * 
	 * @param catalogRecord
	 * @throws MetadataRegistryWritingException
	 */
	void deleteCatalogRecord(IRI catalogRecord) throws MetadataRegistryWritingException;

	/**
	 * Adds an embedded lexicalization set for a dataset.
	 * 
	 * @param dataset
	 * @param lexicalizationSet
	 *            if {@code null} is passed, a local IRI is created
	 * @param lexicon
	 * @param lexicalizationModel
	 * @param language
	 * @param references
	 * @param lexicalEntries
	 * @param lexicalizations
	 * @param percentage
	 * @param avgNumOfLexicalizations
	 * @throws MetadataRegistryWritingException
	 */
	void addEmbeddedLexicalizationSet(IRI dataset, @Nullable IRI lexicalizationSet, @Nullable IRI lexicon,
			IRI lexicalizationModel, String language, @Nullable BigInteger references,
			@Nullable BigInteger lexicalEntries, @Nullable BigInteger lexicalizations,
			@Nullable BigDecimal percentage, @Nullable BigDecimal avgNumOfLexicalizations)
			throws MetadataRegistryWritingException;

	/**
	 * Deletes an embeded lexicalization set
	 * 
	 * @param lexicalizationSet
	 * @throws MetadataRegistryWritingException
	 * @throws MetadataRegistryStateException
	 */
	void deleteEmbeddedLexicalizationSet(IRI lexicalizationSet)
			throws MetadataRegistryWritingException, MetadataRegistryStateException;

    void addEmbeddedLinkset(IRI dataset, @Nullable IRI linkset, @Nullable String targetUriSpace, @Nullable IRI linkPredicate, @Nullable Integer linkCount) throws MetadataRegistryWritingException;

    /**
	 * Sets whether a dataset is derefereanceable or not. If {@code value} is {@code true}, then sets
	 * {@code mdreg:standardDereferenciation} and if {@code false} sets {@code mdreg:noDereferenciation}
	 * 
	 * @param dataset
	 * @param value
	 *            if {@code null}, the dereferenciability is left unspecified
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws MetadataRegistryWritingException
	 */
	void setDereferenciability(IRI dataset, Boolean value)
			throws IllegalArgumentException, MetadataRegistryWritingException;

	/**
	 * Sets the SPARQL endpoint of a dataset.
	 * 
	 * @param dataset
	 * @param endpoint
	 *            if {@code null}, the endpoint is left unspecified
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 * @throws IOException
	 */
	void setSPARQLEndpoint(IRI dataset, IRI endpoint)
			throws IllegalArgumentException, MetadataRegistryWritingException;

	/**
	 * Sets the title of a dataset. This operation also deletes any existing title of the dataset in the same language.
	 * 
	 * @param dataset
	 * @param title
	 *
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	void setTitle(IRI dataset, Literal title)
			throws IllegalArgumentException, MetadataRegistryWritingException;

	/**
	 * Deletes a title of a dataset. If no title is specified, then all titles are deleted.
	 *
	 * @param dataset
	 * @param title
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	void deleteTitle(IRI dataset, Literal title) throws IllegalArgumentException, MetadataRegistryWritingException;

	/**
	 * Sets the description of a dataset. This operation also deletes any existing description of the dataset in the same language.
	 *
	 * @param dataset
	 * @param description
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	void setDescription(IRI dataset, Literal description)
			throws IllegalArgumentException, MetadataRegistryWritingException;

	/**
	 * Deletes a description of a dataset. If no title is specified, then all descriptions are deleted.
	 *
	 * @param dataset
	 * @param description
	 * @throws IllegalArgumentException
	 * @throws MetadataRegistryWritingException
	 */
	void deleteDescription(IRI dataset, Literal description) throws IllegalArgumentException, MetadataRegistryWritingException;

	/**
	 * Returns a list of limitations associated with the provided <em>endpoint</em>
	 * 
	 * @param endpoint
	 * @return
	 */
	Set<IRI> getSPARQLEndpointLimitations(IRI endpoint);

	/**
	 * Sets the given <em>limitation</em> for the provided <em>endpoint</em>
	 * 
	 * @param endpoint
	 * @param limitation
	 * @throws MetadataRegistryWritingException
	 */
	void setSPARQLEndpointLimitation(IRI endpoint, IRI limitation) throws MetadataRegistryWritingException;

	/**
	 * Removes the given <em>limitation</em> from the provided <em>endpoint</em>
	 * 
	 * @param endpoint
	 * @param limitation
	 * @throws MetadataRegistryWritingException
	 */
	void removeSPARQLEndpointLimitation(IRI endpoint, IRI limitation) throws MetadataRegistryWritingException;

	/**
	 * Returns the metadata associated with the given dataset
	 * 
	 * @param dataset
	 * @return
	 * @throws MetadataRegistryStateException
	 * @throws NoSuchDatasetMetadataException
	 */
	DatasetMetadata getDatasetMetadata(IRI dataset)
			throws NoSuchDatasetMetadataException, MetadataRegistryStateException;

	/**
	 * Returns the lexicalization sets associated with the given dataset.
	 * 
	 * @param dataset
	 * @return
	 */
	Collection<LexicalizationSetMetadata> getEmbeddedLexicalizationSets(IRI dataset);

	/**
	 * Returns the linksets associated with the given dataset
	 * 
	 * @param dataset
	 * @param treshold
	 * @param coalesce
	 * @return
	 */
	Collection<LinksetMetadata> getEmbeddedLinksets(IRI dataset, long treshold, boolean coalesce);

	/**
	 * Returns metadata about the dataset identified by the given URI. If no dataset is found, then the method
	 * returns <code>null</code>.
	 * 
	 * @param uriResource
	 * @return
	 */
	DatasetMetadata findDatasetForResource(IRI iriResource);

	/**
	 * Returns metadata about all datasets that might contain the provided IRI.
	 * @param iriResource
	 * @return
	 */
	Collection<DatasetMetadata> listDatasetsForResource(IRI iriResource);

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
	 * Discover metadata about a dataset given an IRI without storing it persistently
	 * 
	 * @param iri
	 * @return
	 * @return the discovered metadata
	 * @throws MetadataDiscoveryException
	 */
	DatasetMetadata discoverDatasetMetadata(IRI iri) throws MetadataDiscoveryException;

	/**
	 * Assess the lexicalization model of the given {@code dataset}
	 * 
	 * @param dataset
	 * @throws MetadataRegistryWritingException
	 * @throws MetadataDiscoveryException
	 */
	void discoverLexicalizationSets(IRI dataset) throws AssessmentException, MetadataRegistryWritingException;

	/**
	 * Returns the lexicalization model of the given {@code dataset} given its embedded lexicalization sets
	 * 
	 * @param dataset
	 * @return
	 * @throws AssessmentException
	 */
	Optional<IRI> getComputedLexicalizationModel(IRI dataset) throws AssessmentException;

	/**
	 * Returns a connection to the underlying RDF repository
	 * 
	 * @return
	 */
	RepositoryConnection getConnection();

	/**
	 * Returns an RDF description of the given dataset
	 * 
	 * @param dataset
	 * @return
	 */
	Model extractProfile(IRI dataset);

	/**
	 * Initializes the metadata registry.
	 * 
	 * @throws MetadataRegistryIntializationException
	 */
	void initialize() throws MetadataRegistryIntializationException;

	/**
	 * Releases the resources consumed by the metadata registry
	 */
	void destroy();

    Map<String, Integer> getClassPartitions(IRI dataset);
}