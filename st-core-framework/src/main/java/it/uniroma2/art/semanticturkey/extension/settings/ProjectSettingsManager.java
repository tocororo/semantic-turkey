package it.uniroma2.art.semanticturkey.extension.settings;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.project.Project;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <T>
 */
public interface ProjectSettingsManager<T extends Settings> extends SettingsManager<T> {

	Collection<String> getProjectSettingsIdentifiers(Project project);
	
	T getProjectSettings(Project project, String identifier);
	
	void storeProjectSettings(Project project, String identifier, T settings);
	
}
