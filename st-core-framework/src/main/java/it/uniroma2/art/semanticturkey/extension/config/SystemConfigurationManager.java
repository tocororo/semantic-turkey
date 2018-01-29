package it.uniroma2.art.semanticturkey.extension.config;

import java.util.Collection;

/**
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface SystemConfigurationManager<CONFTYPE extends Configuration> extends ConfigurationManager<CONFTYPE> {

	default Collection<String> getSystemConfigurationIdentifiers() {
		// @TODO
		throw new RuntimeException("still not implemented!!!");
	}
	
	default CONFTYPE getSystemConfiguration(String identifier) {
		// @TODO
		throw new RuntimeException("still not implemented!!!");
	}
	
	default void storeSystemConfiguration(String identifier, CONFTYPE configuration) {
		// @TODO
		throw new RuntimeException("still not implemented!!!");
	}
	
	
}
