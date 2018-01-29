package it.uniroma2.art.semanticturkey.extension.config;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface UserConfigurationManager<CONFTYPE extends Configuration> extends ConfigurationManager<CONFTYPE> {

	default Collection<String> getUserConfigurationIdentifiers(STUser user) {
		// @TODO
		throw new RuntimeException("still not implemented!!!");
	}
	
	default CONFTYPE getUserConfiguration(STUser user, String identifier) {
		// @TODO
		throw new RuntimeException("still not implemented!!!");
	}
	
	default void storeUserConfiguration(STUser user, String identifier, CONFTYPE configuration) {
		// @TODO
		throw new RuntimeException("still not implemented!!!");
	}
	
	
}
