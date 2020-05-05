package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.plugins.PluginInfo;

/**
 * This class provides services for handling plugins.
 */
@STService
public class Plugins extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Plugins.class);

	/**
	 * Returns the available configuration options for a given plug-in
	 * 
	 * @param factoryID
	 *            the identifier of the plug-in factory
	 * @return
	 * @throws PropertyNotFoundException
	 */
	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('sys(plugins)', 'R')") //temporarily disabled (maybe not required)
	public Collection<STProperties> getPluginConfigurations(String factoryID)
			throws PropertyNotFoundException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(factoryID);

		return pluginFactory.getPluginConfigurations();
	}

	/**
	 * Returns the implementations of a given extension point
	 * 
	 * @param extensionPoint
	 *            the name of the extension point (it should be the fully qualified name of the interface
	 *            implemented by the plug-in instances)
	 * @return
	 */
	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('sys(plugins)', 'R')") //temporarily disabled (maybe not required)
	public Collection<PluginInfo> getAvailablePlugins(String extensionPoint) {
		Collection<PluginFactory<?, ?, ?, ?, ?>> pluginFactoryCollection = PluginManager
				.getPluginFactories(extensionPoint);

		return pluginFactoryCollection.stream().map(fact -> new PluginInfo(fact.getClass().getName()))
				.collect(Collectors.toList());
	}

}