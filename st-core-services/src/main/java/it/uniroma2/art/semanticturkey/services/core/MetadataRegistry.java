package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepository;
import it.uniroma2.art.semanticturkey.resources.DatasetMetadataRepositoryWritingException;
import it.uniroma2.art.semanticturkey.resources.DuplicateDatasetMetadataException;
import it.uniroma2.art.semanticturkey.resources.NoSuchDatasetMetadataException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

/**
 * This service class allows the management of the metadata about remote datasets.
 */
@GenerateSTServiceController
@Validated
@Component
public class MetadataRegistry extends STServiceAdapterOLD {

	private DatasetMetadataRepository datasetMetadataRepository;
	
	@Autowired
	public void setDatasetMetadataRepository(DatasetMetadataRepository datasetMetadataRepository) {
		this.datasetMetadataRepository = datasetMetadataRepository;
	}
	
	/**
	 * Adds the metadata describing the (remote) dataset identified by the given base URI.
	 * @param baseURI
	 * @param title
	 * @param sparqlEndpoint
	 * @param dereferenceable
	 * @throws DuplicateDatasetMetadataException 
	 * @throws DatasetMetadataRepositoryWritingException 
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'C')")
	public void addDatasetMetadata(String baseURI, String title, String sparqlEndpoint, boolean dereferenceable) throws DuplicateDatasetMetadataException, DatasetMetadataRepositoryWritingException {
		
		DatasetMetadata meta = new DatasetMetadata(baseURI, title, null, sparqlEndpoint, dereferenceable, null);
		
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
	 * @param baseURI
	 * @throws DatasetMetadataRepositoryWritingException 
	 * @throws NoSuchDatasetMetadataException 
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'D')")
	public void deleteDatasetMetadata(String baseURI) throws DatasetMetadataRepositoryWritingException, NoSuchDatasetMetadataException {
		
		synchronized (datasetMetadataRepository) {
			datasetMetadataRepository.deleteDatasetMetadata(baseURI);
			datasetMetadataRepository.writeBackToFile();			
		}
	}

	/**
	 * Edits the metadata describing the (remote) dataset identified by the given base URI.
	 * @param baseURI
	 * @param newBaseURI
	 * @param newTitle
	 * @param newSparqlEndpoint
	 * @param newDereferenceable
	 * @throws NoSuchDatasetMetadataException 
	 * @throws DatasetMetadataRepositoryWritingException 
	 * @throws DuplicateDatasetMetadataException 
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'U')")
	public void editDatasetMetadata(String baseURI, String newBaseURI, String newTitle, String newSparqlEndpoint, boolean newDereferenceable) throws NoSuchDatasetMetadataException, DatasetMetadataRepositoryWritingException, DuplicateDatasetMetadataException {
		
		if (newTitle.equals("")) {
			newTitle = null;
		}
		
		if (newSparqlEndpoint.equals("")) {
			newSparqlEndpoint = null;
		}
		
		DatasetMetadata meta = new DatasetMetadata(newBaseURI, newTitle, null, newSparqlEndpoint, newDereferenceable, null);
		
		synchronized (datasetMetadataRepository) {
			datasetMetadataRepository.replaceDatasetMetadata(baseURI, meta);
			datasetMetadataRepository.writeBackToFile();
		}
	}
	
	/**
	 * Returns the metadata describing the (remote) dataset identified by the given base URI.
	 * @param baseURI
	 * @return
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Response getDatasetMetadata(String baseURI) {
				
		DatasetMetadata meta = datasetMetadataRepository.getDatasetMetadata(baseURI);
		
		if (meta == null) {
			return createReplyFAIL("The Metadata Registry does not contain metadata about the dataset '" + baseURI + "'");
		}
		
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		
		Element datasetMetadataElement = XMLHelp.newElement(dataElement, "datasetMetadata");
		datasetMetadataElement.setAttribute("baseURI", meta.getBaseURI());
		
		String title = meta.getTitle();
		if (title != null && !title.equals("")) {
			XMLHelp.newElement(datasetMetadataElement, "title", title);
		}
		
		String sparqlEndpoint = meta.getSparqlEndpoint();
		if (sparqlEndpoint != null && !sparqlEndpoint.equals("")) {
			XMLHelp.newElement(datasetMetadataElement, "sparqlEndpoint", sparqlEndpoint);
		}
		XMLHelp.newElement(datasetMetadataElement, "dereferenceable", Boolean.toString(meta.isDereferenceable()));
		
		return response;
	}
	
	/**
	 * Lists the (remote) datasets having associated metadata.
	 * @return
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('sys(metadataRegistry)', 'R')")
	public Response listDatasets() {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		
		for (DatasetMetadata meta : datasetMetadataRepository.getAllDatasetMetadata()) {
			addDataset2Element(dataElement, meta);
		}
		
		return response;
	}
	
	private void addDataset2Element(Element element, DatasetMetadata meta) {
		String datasetTitle = meta.getTitle();
		String datasetBaseURI = meta.getBaseURI();
		
		Element datasetElement = XMLHelp.newElement(element, "dataset");
		
		datasetElement.setAttribute("baseURI", datasetBaseURI);
		
		if (datasetTitle != null) {
			datasetElement.setAttribute("title", datasetTitle);
		}		
	}
}
