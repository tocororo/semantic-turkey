package it.uniroma2.art.semanticturkey.extension;

import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationManager;

public interface ConfigurableExtensionPoint<CONFTYPE extends Configuration> extends ExtensionPoint, ConfigurationManager<CONFTYPE> {
	
	
	
}
