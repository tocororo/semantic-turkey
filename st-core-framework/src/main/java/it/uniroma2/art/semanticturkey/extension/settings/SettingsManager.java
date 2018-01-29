package it.uniroma2.art.semanticturkey.extension.settings;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;

/**
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 */
public interface SettingsManager<T extends Settings> {

	default String getExtensionId() {
		if (this instanceof ExtensionFactory) {
			return ((ExtensionFactory<?>)this).getExtensionType().getName();
		} else if (this instanceof ExtensionPoint) {
			return ((ExtensionPoint)this).getInterface().getName();
		} else {
			throw new IllegalStateException("Could not determine extension id");
		}
	}
	
	
//	Collection<Reference> getSettingReferences(Project project, STUser user);
	
//	T getSetting(Reference reference);
	
//	void storeSetting(Reference reference, T configuration);
	
}
