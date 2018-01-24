package it.uniroma2.art.semanticturkey.extension.settings;

import java.util.Collection;

/**
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface SystemSettingsManager<T extends Settings> extends SettingsManager<T> {

	Collection<String> getSystemSettingsIdentifiers();
	
	T getSystemSettings(String identifier);
	
	void storeSystemSettings(String identifier, T settings);
	
	
}
