package it.uniroma2.art.semanticturkey.extension.impl;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniroma2.art.semanticturkey.properties.*;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionPointException;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackend;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnectorExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporter;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetmetadata.DatasetMetadataExporterExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformerExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngineExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurerExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategyExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGeneratorExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.extension.settings.SettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.impl.SettingsSupport;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

public class ExtensionPointManagerImpl implements ExtensionPointManager{

    @Autowired
    private BundleContext context;
    private ServiceTracker extensionPointTracker;
    private ServiceTracker extensionFactoryTracker;
    private ServiceTracker configurationManagerTracker;
    private ServiceTracker settingsManagerTracker;
    private ConcurrentHashMap<String, Class<?>> configClassName2Class = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        extensionPointTracker = new ServiceTracker(context, ExtensionPoint.class.getName(), null);
        extensionFactoryTracker = new ServiceTracker(context, ExtensionFactory.class.getName(), new ServiceTrackerCustomizer() {

            @Override
            public void removedService(ServiceReference arg0, Object arg1) {
                if (arg1 instanceof ConfigurableExtensionFactory<?, ?>) {
                    for (Configuration cfg : ((ConfigurableExtensionFactory<?, ?>) arg1)
                            .getConfigurations()) {
                        configClassName2Class.remove(cfg.getClass().getName(), cfg.getClass());
                    }
                }
                context.ungetService(arg0);
            }

            @Override
            public void modifiedService(ServiceReference arg0, Object arg1) {
                // nothing to do
            }

            @SuppressWarnings("unchecked")
            @Override
            public Object addingService(ServiceReference arg0) {
                Object obj = context.getService(arg0);
                if (obj instanceof ConfigurableExtensionFactory<?, ?>) {
                    for (Configuration cfg : ((ConfigurableExtensionFactory<?, ?>) obj)
                            .getConfigurations()) {
                        configClassName2Class.put(cfg.getClass().getName(), cfg.getClass());
                    }
                }
                return obj;
            }
        });
        configurationManagerTracker = new ServiceTracker(context, ConfigurationManager.class.getName(), null);
        settingsManagerTracker = new ServiceTracker(context, SettingsManager.class.getName(), null);
        extensionPointTracker.open();
        extensionFactoryTracker.open();
        configurationManagerTracker.open();
        settingsManagerTracker.open();

        ProjectManager.setExtensionPointManager(this);
    }

    @PreDestroy
    public void destroy() {
        ProjectManager.setExtensionPointManager(null);

        try {
            if (extensionPointTracker != null) {
                extensionPointTracker.close();
            }
        } finally {
            try {
                if (extensionFactoryTracker != null) {
                    extensionFactoryTracker.close();
                }
            } finally {
                try {
                    if (configurationManagerTracker != null) {
                        configurationManagerTracker.close();
                    }
                } finally {
                    if (settingsManagerTracker != null) {
                        settingsManagerTracker.close();
                    }
                }
            }
        }
    }

    @Override
    public Collection<ExtensionPoint> getExtensionPoints(Scope... scopes) {
        Collection<ExtensionPoint> rv = new ArrayList<>();
        Set<Scope> filter = new HashSet<>();
        Arrays.stream(scopes.length == 0 ? Scope.values() : scopes).forEach(filter::add);
        for (Object expt : extensionPointTracker.getServices()) {
            ExtensionPoint expt2 = ((ExtensionPoint) expt);
            if (filter.contains(expt2.getScope())) {
                rv.add(expt2);
            }
        }
        return rv;
    }

    @Override
    public ExtensionPoint getExtensionPoint(String identifier) throws NoSuchExtensionPointException {
        for (Object expt : extensionPointTracker.getServices()) {
            ExtensionPoint expt2 = ((ExtensionPoint) expt);

            if (expt2.getInterface().getName().equals(identifier)) {
                return expt2;
            }
        }

        throw new NoSuchExtensionPointException("Unrecognized extension point: " + identifier);
    }

    @Override
    public CollaborationBackendExtensionPoint getCollaborationBackend() {
        return ((CollaborationBackendExtensionPoint) getExtensionPoint(CollaborationBackend.class.getName()));
    }

    @Override
    public DatasetCatalogConnectorExtensionPoint getDatasetCatalogConnector() {
        return ((DatasetCatalogConnectorExtensionPoint) getExtensionPoint(
                DatasetCatalogConnector.class.getName()));
    }

    @Override
    public DatasetMetadataExporterExtensionPoint getDatasetMetadataExporter() {
        return ((DatasetMetadataExporterExtensionPoint) getExtensionPoint(
                DatasetMetadataExporter.class.getName()));
    }

    @Override
    public RenderingEngineExtensionPoint getRenderingEngine() {
        return ((RenderingEngineExtensionPoint) getExtensionPoint(RenderingEngine.class.getName()));
    }

    @Override
    public RDFTransformerExtensionPoint getRDFTransformer() {
        return ((RDFTransformerExtensionPoint) getExtensionPoint(RDFTransformer.class.getName()));
    }

    @Override
    public RepositoryImplConfigurerExtensionPoint getRepositoryImplConfigurer() {
        return ((RepositoryImplConfigurerExtensionPoint) getExtensionPoint(
                RepositoryImplConfigurer.class.getName()));
    }

    @Override
    public SearchStrategyExtensionPoint getSearchStrategy() {
        return ((SearchStrategyExtensionPoint) getExtensionPoint(SearchStrategy.class.getName()));
    }

    @Override
    public URIGeneratorExtensionPoint getURIGenerator() {
        return (URIGeneratorExtensionPoint) getExtensionPoint(URIGenerator.class.getName());
    }

    @Override
    public ConfigurationManager<?> getConfigurationManager(String componentIdentifier)
            throws NoSuchConfigurationManager {
        for (Object confManager : configurationManagerTracker.getServices()) {
            ConfigurationManager<?> confManager2 = (ConfigurationManager<?>) confManager;
            if (Objects.equals(componentIdentifier, confManager2.getId())) {
                return confManager2;
            }
        }

        throw new NoSuchConfigurationManager("Unrecognized configuration manager: " + componentIdentifier);
    }

    @Override
    public Collection<ConfigurationManager<?>> getConfigurationManagers() {
        return Arrays.stream(configurationManagerTracker.getServices()).map(o -> (ConfigurationManager<?>) o)
                .collect(toList());
    }

    @Override
    public Collection<Reference> getConfigurationReferences(Project project, STUser user,
                                                            String componentIdentifier) throws NoSuchConfigurationManager {
        return getConfigurationManager(componentIdentifier).getConfigurationReferences(project, user);
    }

    @Override
    public Configuration getConfiguration(String componentIdentifier, Reference reference)
            throws NoSuchConfigurationManager, STPropertyAccessException {
        return getConfigurationManager(componentIdentifier).getConfiguration(reference);
    }

    @Override
    public SettingsManager getSettingsManager(String componentIdentifier) throws NoSuchSettingsManager {
        for (Object settingsManager : settingsManagerTracker.getServices()) {
            SettingsManager settingsManager2 = (SettingsManager) settingsManager;
            if (Objects.equals(componentIdentifier, settingsManager2.getId())) {
                return settingsManager2;
            }
        }

        throw new NoSuchSettingsManager("Unrecognized settings manager: " + componentIdentifier);
    }

    @Override
    public Collection<SettingsManager> getSettingsManagers() {
        return Arrays.stream(settingsManagerTracker.getServices()).map(o -> (SettingsManager) o)
                .collect(toList());
    }

    @Override
    public Collection<Scope> getSettingsScopes(String componentIdentifier) throws NoSuchSettingsManager {
        return getSettingsManager(componentIdentifier).getSettingsScopes();
    }

    @Override
    public Settings getSettings(Project project, STUser user, UsersGroup group, String componentIdentifier, Scope scope)
            throws STPropertyAccessException, NoSuchSettingsManager {
        return getSettingsManager(componentIdentifier).getSettings(project, user, group, scope);
    }

    @Override
    public Settings getSettingsDefault(Project project, STUser user, UsersGroup group, String componentID, Scope scope, Scope defaultScope) throws STPropertyAccessException, NoSuchSettingsManager {
        return getSettingsManager(componentID).getSettingsDefault(project, user, group, scope, defaultScope);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void storeConfiguration(String componentIdentifier, Reference reference, ObjectNode configuration)
            throws IOException, WrongPropertiesException, NoSuchConfigurationManager,
            STPropertyUpdateException, STPropertyAccessException {
        ConfigurationManager<?> configurationManager = getConfigurationManager(componentIdentifier);
        Class<? extends Configuration> configBaseClass = ReflectionUtilities
                .<Configuration>getInterfaceArgumentTypeAsClass(configurationManager.getClass(),
                        ConfigurationManager.class, 0);

        Configuration configObj = STPropertiesManager.loadSTPropertiesFromObjectNodes(configBaseClass, true,
                configuration);
        ((ConfigurationManager) configurationManager).storeConfiguration(reference, configObj);
    }

    @Override
    public void deleteConfiguraton(String componentIdentifier, Reference reference)
            throws NoSuchConfigurationManager, ConfigurationNotFoundException {
        ConfigurationManager<?> configurationManager = getConfigurationManager(componentIdentifier);
        ((ConfigurationManager) configurationManager).deleteConfiguration(reference);
    }

    @Override
    public void storeSettings(String componentIdentifier, Project project, STUser user, UsersGroup group, Scope scope,
                              ObjectNode settings) throws NoSuchSettingsManager, STPropertyUpdateException,
            WrongPropertiesException, STPropertyAccessException {
        SettingsManager settingsManager = getSettingsManager(componentIdentifier);
        Settings settingsObj = SettingsSupport.createSettings(settingsManager, scope, settings);
        settingsManager.storeSettings(project, user, group, scope, settingsObj);
    }

    @Override
    public void storeSetting(String componentID, Project project, STUser loggedUser, UsersGroup group, Scope scope, String property, JsonNode propertyValue) throws NoSuchSettingsManager, STPropertyUpdateException, WrongPropertiesException, STPropertyAccessException, PropertyNotFoundException, IOException {
        // A non-atomic read-update of a single settings property
        SettingsManager settingsManager = getSettingsManager(componentID);
        Settings explicitSettings = settingsManager.getSettings(project, loggedUser, group, scope, true);
        Type propertyType = explicitSettings.getPropertyType(property);
        ObjectMapper om = STPropertiesManager.createObjectMapper();
        Object parsedPropertyValue = om.readValue(om.treeAsTokens(propertyValue), om.constructType(propertyType));
        explicitSettings.setPropertyValue(property, parsedPropertyValue);
        settingsManager.storeSettings(project, loggedUser, group, scope, explicitSettings);
    }

    @Override
    public void storeSettingsDefault(String componentIdentifier, Project project, STUser user, UsersGroup group, Scope scope, Scope defaultScope,
                                     ObjectNode settings) throws NoSuchSettingsManager, STPropertyUpdateException,
            WrongPropertiesException, STPropertyAccessException {
        SettingsManager settingsManager = getSettingsManager(componentIdentifier);
        Settings settingsObj = SettingsSupport.createSettings(settingsManager, scope, settings);
        settingsManager.storeSettingsDefault(project, user, group, scope, defaultScope, settingsObj);
    }

    @Override
    public void storeSettingDefault(String componentID, Project project, STUser user, UsersGroup group, Scope scope, Scope defaultScope,
                                    String property, JsonNode propertyValue) throws NoSuchSettingsManager, STPropertyUpdateException,
            WrongPropertiesException, STPropertyAccessException, PropertyNotFoundException, IOException {
        // A non-atomic read-update of a single settings default property
        SettingsManager settingsManager = getSettingsManager(componentID);
        Settings settingsDefault = settingsManager.getSettingsDefault(project, user, group, scope, defaultScope);
        Type propertyType = settingsDefault.getPropertyType(property);
        ObjectMapper om = STPropertiesManager.createObjectMapper();
        Object parsedPropertyValue = propertyValue == null ? null : om.readValue(om.treeAsTokens(propertyValue), om.constructType(propertyType));
        settingsDefault.setPropertyValue(property, parsedPropertyValue);
        settingsManager.storeSettingsDefault(project, user, group, scope, defaultScope, settingsDefault);
    }

    @Override
    public Collection<ExtensionFactory<?>> getExtensions(String extensionPoint) {
        ExtensionPoint expt = getExtensionPoint(extensionPoint);
        Class<?> exptInt = expt.getInterface();
        Collection<ExtensionFactory<?>> rv = new ArrayList<>();
        for (Object extFactory : extensionFactoryTracker.getServices()) {
            if (exptInt.isAssignableFrom(((ExtensionFactory<?>) extFactory).getExtensionType())) {
                rv.add((ExtensionFactory<?>) extFactory);
            }
        }
        return rv;
    }

    @Override
    public ExtensionFactory<?> getExtension(String componentID) throws NoSuchExtensionException {
        for (Object extFactory : extensionFactoryTracker.getServices()) {
            if (((ExtensionFactory<?>) extFactory).getId().equals(componentID)) {
                return (ExtensionFactory<?>) extFactory;
            }
        }

        throw new NoSuchExtensionException("Unrecognized extension: " + componentID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Extension> T instantiateExtension(Class<T> targetInterface, PluginSpecification spec)
            throws IllegalArgumentException, InvalidConfigurationException, NoSuchExtensionException,
            STPropertyAccessException {
        @SuppressWarnings("unchecked")
        ExtensionFactory<?> extFactory = this.getExtension(spec.getFactoryId());
        if (!targetInterface.isAssignableFrom(extFactory.getExtensionType())) {
            throw new IllegalArgumentException("Extension \"" + spec.getFactoryId()
                    + "\" is not assignable to interface \"" + targetInterface.getName() + "\"");
        }

        ObjectNode config = spec.getConfiguration();

        if (spec.getConfigType() != null && config != null
                && !config.hasNonNull(STPropertiesManager.SETTINGS_TYPE_PROPERTY)) {
            config = config.deepCopy();
            config.put(STPropertiesManager.SETTINGS_TYPE_PROPERTY, spec.getConfigType());
        }
        T obj;

        if (config == null || !config.fieldNames().hasNext()) {
            if (extFactory instanceof NonConfigurableExtensionFactory) {
                obj = ((NonConfigurableExtensionFactory<T>) extFactory).createInstance();
            } else {
                throw new IllegalArgumentException("Missing configuration");
            }
        } else {
            if (extFactory instanceof ConfigurableExtensionFactory) {
                Class<? extends Configuration> configBaseClass = ReflectionUtilities
                        .getInterfaceArgumentTypeAsClass(extFactory.getClass(), ConfigurationManager.class,
                                0);

                Configuration configObj = STPropertiesManager.loadSTPropertiesFromObjectNodes(configBaseClass,
                        true, config);
                STPropertiesChecker checker = STPropertiesChecker.getModelConfigurationChecker(configObj);
                if (!checker.isValid()) {
                    throw new InvalidConfigurationException(checker.getErrorMessage());
                }

                obj = (T) ((ConfigurableExtensionFactory<T, Configuration>) extFactory)
                        .createInstance(configObj);
            } else {
                throw new IllegalArgumentException(
                        "Provided configuration for a non configurable extension factory");
            }
        }
        return obj;

    }

    @Override
    public Optional<Class<?>> getConfigurationClassFromName(String configClassName) {
        return Optional.ofNullable(configClassName2Class.get(configClassName));
    }

}
