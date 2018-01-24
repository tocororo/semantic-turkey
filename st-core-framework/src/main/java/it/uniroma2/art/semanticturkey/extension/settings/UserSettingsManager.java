package it.uniroma2.art.semanticturkey.extension.settings;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface UserSettingsManager<T extends Settings> extends SettingsManager<T> {

	Collection<String> getUserSettingsIdentifiers(STUser user);
	
	T getUserSettings(STUser user, String identifier);
	
	void storeUserSettings(STUser user, String identifier, T settings);
	
	
}
