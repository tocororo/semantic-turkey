package it.uniroma2.art.semanticturkey.extension;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporterExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformerExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngineExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurerExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategyExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGeneratorExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
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

	ConfigurationManager<?> getConfigurationManager(String componentID) throws NoSuchConfigurationManager;

	SettingsManager getSettingsManager(String componentID) throws NoSuchSettingsManager;

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
	 * @throws STPropertyAccessException
	 */
	Configuration getConfiguration(String componentIdentifier, Reference reference)
			throws IOException, ConfigurationNotFoundException, WrongPropertiesException,
			NoSuchConfigurationManager, STPropertyAccessException;

	void storeConfiguration(String componentIdentifier, Reference reference, ObjectNode configuration)
			throws IOException, WrongPropertiesException, NoSuchConfigurationManager,
			STPropertyUpdateException, STPropertyAccessException;

	void deleteConfiguraton(String componentIdentifier, Reference reference)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException;

	Settings getSettings(Project project, STUser user, String componentIdentifier, Scope scope)
			throws STPropertyAccessException, NoSuchSettingsManager;

	Collection<Scope> getSettingsScopes(String componentIdentifier) throws NoSuchSettingsManager;

	void storeSettings(String componentIdentifier, Project project, STUser user, Scope scope,
			ObjectNode settings)
			throws NoSuchSettingsManager, STPropertyUpdateException, WrongPropertiesException,
			STPropertyAccessException;

	Collection<ExtensionFactory<?>> getExtensions(String extensionPoint);

	/**
	 * Returns the {@link ExtensionFactory} matching the given <em>component identifier</code>
	 * 
	 * @param componentID
	 * @return
	 * @throws NoSuchExtensionException
	 */
	ExtensionFactory<?> getExtension(String componentID) throws NoSuchExtensionException;

	/**
	 * Create an instance of an extension that conforms to {@code targetInterface}, following the provided
	 * {@code spec}
	 * 
	 * @param targetInterface
	 * @param spec
	 * @return
	 * @throws IllegalArgumentException
	 * @throws NoSuchExtensionException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws InvalidConfigurationException
	 */
	public <T extends Extension> T instantiateExtension(Class<T> targetInterface, PluginSpecification spec)
			throws IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException;

}
