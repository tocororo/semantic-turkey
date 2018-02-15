package it.uniroma2.art.semanticturkey.extension.impl;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.impl.ConfigurationSupport;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionPointException;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackend;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendExtensionPoint;
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
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;

public class ExtensionPointManagerImpl implements ExtensionPointManager {

	@Autowired
	private BundleContext context;
	private ServiceTracker extensionPointTracker;
	private ServiceTracker extensionFactoryTracker;
	private ServiceTracker configurationManagerTracker;
	private ServiceTracker settingsManagerTracker;

	@PostConstruct
	public void init() {
		extensionPointTracker = new ServiceTracker(context, ExtensionPoint.class.getName(), null);
		extensionFactoryTracker = new ServiceTracker(context, ExtensionFactory.class.getName(), null);
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
	public Configuration getConfiguration(String componentIdentifier, Reference reference) throws IOException,
			ConfigurationNotFoundException, WrongPropertiesException, NoSuchConfigurationManager {
		return getConfigurationManager(componentIdentifier).getConfiguration(reference);
	}

	private SettingsManager getSettingsManager(String componentIdentifier) throws NoSuchSettingsManager {
		for (Object settingsManager : settingsManagerTracker.getServices()) {
			SettingsManager settingsManager2 = (SettingsManager) settingsManager;
			if (Objects.equals(componentIdentifier, settingsManager2.getId())) {
				return settingsManager2;
			}
		}

		throw new NoSuchSettingsManager("Unrecognized settings manager: " + componentIdentifier);
	}

	@Override
	public Collection<Scope> getSettingsScopes(String componentIdentifier) throws NoSuchSettingsManager {
		return getSettingsManager(componentIdentifier).getSettingsScopes();
	}

	@Override
	public Settings getSettings(Project project, STUser user, String componentIdentifier, Scope scope)
			throws STPropertyAccessException, NoSuchSettingsManager {
		return getSettingsManager(componentIdentifier).getSettings(project, user, scope);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void storeConfiguration(String componentIdentifier, Reference reference,
			Map<String, Object> configuration) throws IOException, WrongPropertiesException,
			NoSuchConfigurationManager, STPropertyUpdateException {
		ConfigurationManager<?> configurationManager = getConfigurationManager(componentIdentifier);
		((ConfigurationManager) configurationManager).storeConfiguration(reference,
				ConfigurationSupport.createConfiguration(configurationManager, configuration));
	}

	@Override
	public void storeSettings(String componentIdentifier, Project project, STUser user, Scope scope,
			Map<String, Object> settings)
			throws NoSuchSettingsManager, STPropertyUpdateException, WrongPropertiesException {
		SettingsManager settingsManager = getSettingsManager(componentIdentifier);
		settingsManager.storeSettings(project, user, scope,
				SettingsSupport.createSettings(settingsManager, scope, settings));
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
	public ExtensionFactory<?> getExtension(String componentIdentifier) {
		for (Object extFactory : extensionFactoryTracker.getServices()) {
			if (((ExtensionFactory<?>) extFactory).getId().equals(componentIdentifier)) {
				return (ExtensionFactory<?>) extFactory;
			}
		}

		throw new NoSuchExtensionException("Unrecognized extension: " + componentIdentifier);
	}

}
