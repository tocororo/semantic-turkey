package it.uniroma2.art.semanticturkey.extension;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.customservice.Operation;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnectorExtensionPoint;
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
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;

public interface ExtensionPointManager {

    /**
     * Returns known extension points.
     *
     * @param scopes if not empty, indicates the scopes we are interested in. Otherwise, every scope is
     *               considered.
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

    DatasetCatalogConnectorExtensionPoint getDatasetCatalogConnector();

    DatasetMetadataExporterExtensionPoint getDatasetMetadataExporter();

    RenderingEngineExtensionPoint getRenderingEngine();

    RDFTransformerExtensionPoint getRDFTransformer();

    RepositoryImplConfigurerExtensionPoint getRepositoryImplConfigurer();

    SearchStrategyExtensionPoint getSearchStrategy();

    URIGeneratorExtensionPoint getURIGenerator();

    Collection<ConfigurationManager<?>> getConfigurationManagers();

    ConfigurationManager<?> getConfigurationManager(String componentID) throws NoSuchConfigurationManager;

    SettingsManager getSettingsManager(String componentID) throws NoSuchSettingsManager;

    Collection<SettingsManager> getSettingsManagers();

    /**
     * Returns the stored configurations associated with a given component
     *
     * @param project
     * @param user
     * @param componentIdentifier
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

    Settings getSettings(Project project, STUser user, UsersGroup group, String componentIdentifier, Scope scope)
            throws STPropertyAccessException, NoSuchSettingsManager;

    Settings getSettingsDefault(Project project, STUser user, UsersGroup group, String componentID, Scope scope, Scope defaultScope) throws STPropertyAccessException, NoSuchSettingsManager;

    Collection<Scope> getSettingsScopes(String componentIdentifier) throws NoSuchSettingsManager;

    void storeSettings(String componentIdentifier, Project project, STUser user, UsersGroup group, Scope scope,
                       ObjectNode settings)
            throws NoSuchSettingsManager, STPropertyUpdateException, WrongPropertiesException,
            STPropertyAccessException;

    void storeSetting(String componentID, Project project, STUser loggedUser, UsersGroup group, Scope scope, String property, JsonNode propertyValue)
            throws NoSuchSettingsManager, STPropertyUpdateException, WrongPropertiesException, STPropertyAccessException, PropertyNotFoundException, IOException;

    void storeSettingsDefault(String componentIdentifier, Project project, STUser user, UsersGroup group, Scope scope, Scope defaultScope,
                              ObjectNode settings) throws NoSuchSettingsManager, STPropertyUpdateException,
            WrongPropertiesException, STPropertyAccessException;

    void storeSettingDefault(String componentIdentifier, Project project, STUser user, UsersGroup group, Scope scope, Scope defaultScope,
                             String property, JsonNode propertyValue) throws NoSuchSettingsManager, STPropertyUpdateException,
            WrongPropertiesException, STPropertyAccessException, PropertyNotFoundException, IOException;

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


    /**
     * Create an instance of an extension that conforms to {@code targetInterface}, following the provided
     * {@code Configuration}
     *
     * @param targetInterface
     * @param conf
     * @return
     * @throws IllegalArgumentException
     * @throws NoSuchExtensionException
     * @throws WrongPropertiesException
     * @throws STPropertyAccessException
     * @throws InvalidConfigurationException
     */
    default <T extends Extension, C extends Configuration> T instantiateExtension(Class<T> targetInterface, C conf)
            throws IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
            STPropertyAccessException, InvalidConfigurationException {
        PluginSpecification pluginSpec = buildPluginSpecification(targetInterface, conf);

        return instantiateExtension(targetInterface,pluginSpec);
    }

    /**
     * Build a {@link PluginSpecification} for the provided {@code targetInterface} and {@code Configuration}
     * @param targetInterface
     * @param conf
     * @param <T>
     * @param <C>
     * @return
     */
    default <T extends Extension, C extends Configuration> PluginSpecification buildPluginSpecification(Class<T> targetInterface, C conf) {
        @SuppressWarnings("unchecked")
        ConfigurableExtensionFactory<?, Operation> extensionFactory = getExtensions(targetInterface.getName()).stream()
                .filter(ConfigurableExtensionFactory.class::isInstance)
                .map(ConfigurableExtensionFactory.class::cast)
                .filter(f -> f.getConfigurations().stream().anyMatch(
                        c -> c.getClass().getName().equals(conf.getClass().getName())))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unable to to find an extension for the configuration class "
                                + conf.getClass().getName()));

        PluginSpecification pluginSpec = new PluginSpecification(extensionFactory.getId(), null, null,
            STPropertiesManager.storeSTPropertiesToObjectNode(conf, true));
        return pluginSpec;
    }

    Optional<Class<?>> getConfigurationClassFromName(String propsType);

}
