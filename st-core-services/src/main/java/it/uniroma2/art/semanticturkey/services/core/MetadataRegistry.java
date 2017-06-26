package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepository;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepositoryWritingException;
import it.uniroma2.art.semanticturkey.resources.DuplicateDatasetMetadataException;
import it.uniroma2.art.semanticturkey.resources.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.metadata.DatasetInfo;

/**
 * This service class allows the management of the metadata about remote datasets.
 */
@STService
public class MetadataRegistry extends STServiceAdapter {

	private DatasetMetadataRepository datasetMetadataRepository;

	@Autowired
	public void setDatasetMetadataRepository(DatasetMetadataRepository datasetMetadataRepository) {
		this.datasetMetadataRepository = datasetMetadataRepository;
	}

	/**
	 * Adds the metadata describing the (remote) dataset identified by the given base URI.
	 * 
	 * @param baseURI
	 * @param title
	 * @param sparqlEndpoint
	 * @param dereferenceable
	 * @throws DuplicateDatasetMetadataException
	 * @throws DatasetMetadataRepositoryWritingException
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public void addDatasetMetadata(String baseURI, String title, String sparqlEndpoint,
			boolean dereferenceable)
			throws DuplicateDatasetMetadataException, DatasetMetadataRepositoryWritingException {

		DatasetMetadata meta = new DatasetMetadata(baseURI, title, null, sparqlEndpoint, dereferenceable,
				null);

		if (meta.getBaseURI() == null) {
			throw new IllegalArgumentException("Invalid base URI: " + baseURI);
		}
		synchronized (datasetMetadataRepository) {
			datasetMetadataRepository.addDatasetMetadata(meta);
			datasetMetadataRepository.writeBackToFile();
		}
	}

	/**
	 * Deletes the metadata about the dataset identified by the given base URI.
	 * 
	 * @param baseURI
	 * @throws DatasetMetadataRepositoryWritingException
	 * @throws NoSuchDatasetMetadataException
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'D')")
	public void deleteDatasetMetadata(String baseURI)
			throws DatasetMetadataRepositoryWritingException, NoSuchDatasetMetadataException {

		synchronized (datasetMetadataRepository) {
			datasetMetadataRepository.deleteDatasetMetadata(baseURI);
			datasetMetadataRepository.writeBackToFile();
		}
	}

	/**
	 * Edits the metadata describing the (remote) dataset identified by the given base URI.
	 * 
	 * @param baseURI
	 * @param newBaseURI
	 * @param newTitle
	 * @param newSparqlEndpoint
	 * @param newDereferenceable
	 * @throws NoSuchDatasetMetadataException
	 * @throws DatasetMetadataRepositoryWritingException
	 * @throws DuplicateDatasetMetadataException
	 */
	@STServiceOperation(method=RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void editDatasetMetadata(String baseURI, String newBaseURI, String newTitle,
			String newSparqlEndpoint, boolean newDereferenceable) throws NoSuchDatasetMetadataException,
			DatasetMetadataRepositoryWritingException, DuplicateDatasetMetadataException {

		if (newTitle.equals("")) {
			newTitle = null;
		}

		if (newSparqlEndpoint.equals("")) {
			newSparqlEndpoint = null;
		}

		DatasetMetadata meta = new DatasetMetadata(newBaseURI, newTitle, null, newSparqlEndpoint,
				newDereferenceable, null);

		synchronized (datasetMetadataRepository) {
			datasetMetadataRepository.replaceDatasetMetadata(baseURI, meta);
			datasetMetadataRepository.writeBackToFile();
		}
	}

	/**
	 * Returns the metadata describing the (remote) dataset identified by the given base URI.
	 * 
	 * @param baseURI
	 * @return
	 * @throws NoSuchDatasetMetadataException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public DatasetMetadata getDatasetMetadata(String baseURI) throws NoSuchDatasetMetadataException {

		DatasetMetadata meta = datasetMetadataRepository.getDatasetMetadata(baseURI);

		if (meta == null) {
			throw new NoSuchDatasetMetadataException(baseURI);
		}

		return meta;
	}

	/**
	 * Lists the (remote) datasets having associated metadata.
	 * 
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Collection<DatasetInfo> listDatasets() {
		return datasetMetadataRepository.getAllDatasetMetadata().stream()
				.map(meta -> new DatasetInfo(meta.getBaseURI(), meta.getTitle()))
				.collect(Collectors.toList());
	}

}
