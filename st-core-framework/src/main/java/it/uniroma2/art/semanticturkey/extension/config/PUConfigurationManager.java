package it.uniroma2.art.semanticturkey.extension.config;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface PUConfigurationManager<CONFTYPE extends Configuration> extends ConfigurationManager<CONFTYPE> {

	Collection<String> getProjectConfigurationIdentifiers(Project project, STUser user);
	
	CONFTYPE getProjectConfiguration(Project project, STUser user, String identifier);
	
	void storeProjectConfiguration(Project project, STUser user, String identifier, CONFTYPE configuration);
	
}
