package it.uniroma2.art.semanticturkey.extension;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <EXTTYPE>
 * @param <CONFIGTYPE>
 */
public interface ConfigurableExtensionFactory<EXTTYPE extends Extension, CONFIGTYPE extends Configuration> extends ExtensionFactory<EXTTYPE>, ConfigurationManager<CONFIGTYPE> {

	/**
	 * Instantiates an extension based on the given configuration object.
	 * 
	 * @param conf
	 * @return
	 */
	EXTTYPE createInstance(CONFIGTYPE conf) throws InvalidConfigurationException;
	
	
	/**
	 * Returns allowed configurations for this factory.
	 * 
	 * @return
	 */
	Collection<CONFIGTYPE> getConfigurations();
	
}
