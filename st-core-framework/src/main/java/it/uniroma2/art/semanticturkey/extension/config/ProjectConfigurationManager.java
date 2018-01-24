package it.uniroma2.art.semanticturkey.extension.config;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.project.Project;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface ProjectConfigurationManager<CONFTYPE extends Configuration> extends ConfigurationManager<CONFTYPE> {

	Collection<String> getProjectConfigurationIdentifiers(Project project);
	
	CONFTYPE getProjectConfiguration(Project project, String identifier);
	
	void storeProjectConfiguration(Project project, String identifier, CONFTYPE configuration);
	
}
