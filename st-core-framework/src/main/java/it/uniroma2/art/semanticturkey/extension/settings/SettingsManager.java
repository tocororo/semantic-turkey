package it.uniroma2.art.semanticturkey.extension.settings;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
public interface SettingsManager<T extends Settings> {

	Collection<Reference> getSettingReferences(Project project, STUser user);
	
	T getSetting(Reference reference);
	
	void storeSetting(Reference reference, T configuration);
	
}
