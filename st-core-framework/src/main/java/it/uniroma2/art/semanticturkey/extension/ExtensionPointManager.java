package it.uniroma2.art.semanticturkey.extension;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporterExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformerExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngineExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurerExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategyExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGeneratorExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;

public interface ExtensionPointManager {

	/**
	 * Returns known extension points.
	 * 
	 * @param scopes
	 *            if not empty, indicates the scopes we are interested in. Otherwise, every scope is
	 *            considered.
	 * @return
	 */
	Collection<ExtensionPoint> getExtensionPoints(Scope... scopes);

	/**
	 * Returns an extension point given its identifier
	 * 
	 * @param identifier
	 * @return
	 * @throws NoSuchExtensionPointException
	 */
	ExtensionPoint getExtensionPoint(String identifier) throws NoSuchExtensionPointException;

	CollaborationBackendExtensionPoint getCollaborationBackend();

	DatasetMetadataExporterExtensionPoint getDatasetMetadataExporter();

	RenderingEngineExtensionPoint getRenderingEngine();

	RDFTransformerExtensionPoint getRDFTransformer();

	RepositoryImplConfigurerExtensionPoint getRepositoryImplConfigurer();

	SearchStrategyExtensionPoint getSearchStrategy();

	URIGeneratorExtensionPoint getURIGenerator();

	Collection<ConfigurationManager<?>> getConfigurationManagers();

	/**
	 * Returns the stored configurations associated with a given component
	 * 
	 * @param project
	 * @param user
	 * @param componentIdentifier
	 * 
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	Collection<Reference> getConfigurationReferences(Project project, STUser user, String componentIdentifier)
			throws NoSuchConfigurationManager;

	/**
	 * Returns a stored configuration located with the supplied identifier
	 * 
	 * @param componentIdentifier
	 * @param reference
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 */
	Configuration getConfiguration(String componentIdentifier, Reference reference) throws IOException,
			ConfigurationNotFoundException, WrongPropertiesException, NoSuchConfigurationManager;

	void storeConfiguration(String componentIdentifier, Reference reference,
			Map<String, Object> configuration) throws IOException, WrongPropertiesException,
			NoSuchConfigurationManager, STPropertyUpdateException;

	Settings getSettings(Project project, STUser user, String componentIdentifier, Scope scope)
			throws STPropertyAccessException, NoSuchSettingsManager;

	Collection<Scope> getSettingsScopes(String componentIdentifier) throws NoSuchSettingsManager;

	void storeSettings(String componentIdentifier, Project project, STUser user, Scope scope,
			Map<String, Object> settings)
			throws NoSuchSettingsManager, STPropertyUpdateException, WrongPropertiesException;

	Collection<ExtensionFactory<?>> getExtensions(String extensionPoint);

	ExtensionFactory<?> getExtension(String componentIdentifier);
}
