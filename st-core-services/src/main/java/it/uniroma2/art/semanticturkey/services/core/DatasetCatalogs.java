package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services for interacting with dataset catalogs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class DatasetCatalogs extends STServiceAdapter {

	/**
	 * Searched a dataset in the connected catalog matching the given criteria
	 * 
	 * @param connectorConfigReference
	 * @param query
	 * @param facets
	 * @param page
	 * @return
	 * @throws InvalidConfigurationException
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	@STServiceOperation
	public SearchResultsPage<DatasetSearchResult> searchDataset(String connectorId, String query,
			@Optional(defaultValue = "{}") @JsonSerialized Map<String, List<String>> facets,
			@Optional(defaultValue = "1") int page) throws WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException, IOException {
		DatasetCatalogConnector metadataRepositoryConnector = exptManager.instantiateExtension(
				DatasetCatalogConnector.class, new PluginSpecification(connectorId, null, null, null));

		return metadataRepositoryConnector.searchDataset(query, facets, page);
	}

	/**
	 * Returns the description of a given dataset provided by the connected catalog
	 * 
	 * @param connectorId
	 * @param datasetId
	 * @return
	 * @throws IllegalArgumentException
	 * @throws NoSuchExtensionException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws InvalidConfigurationException
	 * @throws IOException
	 */
	@STServiceOperation
	public DatasetDescription describeDataset(String connectorId, String datasetId)
			throws IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException, IOException {
		DatasetCatalogConnector metadataRepositoryConnector = exptManager.instantiateExtension(
				DatasetCatalogConnector.class, new PluginSpecification(connectorId, null, null, null));
		return metadataRepositoryConnector.describeDataset(datasetId);
	}

	
}
