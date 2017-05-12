package it.uniroma2.art.semanticturkey.plugin;

import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.DatasetMetadataExporterException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

/**
 * A factory responsible for the instantiation of a plugin.
 * 
 * @param <T>
 */
public interface PluginFactory<T extends PluginConfiguration> {

	/**
	 * Returns the factory identifier.
	 * 
	 * @return
	 */
	String getID();

	/**
	 * Returns allowed configurations for this factory.
	 * 
	 * @return
	 */
	Collection<PluginConfiguration> getPluginConfigurations();

	/**
	 * Returns the default configuration.
	 * 
	 * @return
	 */
	T createDefaultPluginConfiguration();

	/**
	 * Instantiates a configuration object given the configuration class name.
	 * 
	 * @param confType
	 * @return
	 * @throws UnsupportedPluginConfigurationException
	 * @throws UnloadablePluginConfigurationException
	 * @throws ClassNotFoundException
	 */
	T createPluginConfiguration(String configType) throws UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, ClassNotFoundException;

	/**
	 * Instantiates a plugin based on the given configuration object.
	 * 
	 * @param conf
	 * @return
	 */
	Object createInstance(PluginConfiguration conf);

	default STProperties getProjectSettings(Project<?> project) throws STPropertyAccessException {
		return null;
	};

	default void storeProjectSettings(STProperties props) {
	}
}
