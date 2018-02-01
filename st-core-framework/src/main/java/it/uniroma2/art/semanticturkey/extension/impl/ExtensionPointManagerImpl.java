package it.uniroma2.art.semanticturkey.extension.impl;

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

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionPointException;
import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.extension.config.impl.ConfigurationSupport;
import it.uniroma2.art.semanticturkey.project.Project;
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

	@PostConstruct
	public void init() {
		extensionPointTracker = new ServiceTracker(context, ExtensionPoint.class.getName(), null);
		extensionFactoryTracker = new ServiceTracker(context, ExtensionFactory.class.getName(), null);
		configurationManagerTracker = new ServiceTracker(context, ConfigurationManager.class.getName(), null);
		extensionPointTracker.open();
		extensionFactoryTracker.open();
		configurationManagerTracker.open();
	}

	@PreDestroy
	public void destroy() {
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
				if (configurationManagerTracker != null) {
					configurationManagerTracker.close();
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

	private ConfigurationManager<?> getConfigurationManager(String componentIdentifier)
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
	public Collection<Reference> getConfigurationReferences(Project project, STUser user,
			String componentIdentifier) throws NoSuchConfigurationManager {
		return getConfigurationManager(componentIdentifier).getConfigurationReferences(project, user);
	}

	@Override
	public Configuration getConfiguration(String componentIdentifier, Reference reference) throws IOException,
			ConfigurationNotFoundException, WrongPropertiesException, NoSuchConfigurationManager {
		return getConfigurationManager(componentIdentifier).getConfiguration(reference);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void storeConfiguration(String componentIdentifier, Reference reference,
			Map<String, Object> configuration) throws IOException, WrongPropertiesException, NoSuchConfigurationManager {
		ConfigurationManager<?> configurationManager = getConfigurationManager(componentIdentifier);
		((ConfigurationManager) configurationManager).storeConfiguration(reference,
				ConfigurationSupport.createConfiguration(configurationManager, configuration));
	}
}
