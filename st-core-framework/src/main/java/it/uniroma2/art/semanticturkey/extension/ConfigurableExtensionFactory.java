package it.uniroma2.art.semanticturkey.extension;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.properties.STProperties;

public interface ConfigurableExtensionFactory<ExtType extends Extension> extends ExtensionFactory<ExtType> {

	/**
	 * Instantiates an extension based on the given configuration object.
	 * 
	 * @param conf
	 * @return
	 */
	ExtType createInstance(STProperties conf);
	
	
	/**
	 * Returns allowed configurations for this factory.
	 * 
	 * @return
	 */
	Collection<STProperties> getConfigurations();
	
}
