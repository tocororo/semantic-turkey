package it.uniroma2.art.semanticturkey.extension;

import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.PUConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.ProjectConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.SystemConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.UserConfigurationManager;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * A ProjectScopedConfigurableComponent is able to look on configurations from all different {@link Scope}s.
 * If a more restricted configuration management capability is needed for a certain extension point, it is
 * possible to separately implement the various {@link ConfigurationManager} subinterfaces. Please notice that
 * if the scope of the extension point is {@link Scope#PROJECT}, neither {@link UserConfigurationManager} nor
 * {@link PUConfigurationManager} should be implemented, thus restricting the choice to the removal of
 * {@link SystemConfigurationManager} only
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface ProjectScopedConfigurableComponent<CONFTYPE extends Configuration>
		extends ConfigurableComponent<CONFTYPE>, ScopedComponent, ProjectConfigurationManager<CONFTYPE>,
		UserConfigurationManager<CONFTYPE> {

	@Override
	default Scope getScope() {
		return Scope.PROJECT;
	}

}
