package it.uniroma2.art.semanticturkey.extension.settings;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface PUSettingsManager<T extends Settings> extends SettingsManager<T> {

	Collection<String> getProjectSettingsIdentifiers(Project project, STUser user);
	
	T getProjectSettings(Project project, STUser user, String identifier);
	
	void storeProjectSettings(Project project, STUser user, String identifier, T settings);
	
}
