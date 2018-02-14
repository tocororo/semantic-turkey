package it.uniroma2.art.semanticturkey.extension;

import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.config.SystemConfigurationManager;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * A SystemScopedConfigurableComponent is able to look on configurations from the sole {@link Scope#SYSTEM} scope.
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@uniroma2.it&gt;
 *
 * @param <CONFTYPE>
 */
public interface SystemScopedConfigurableComponent<CONFTYPE extends Configuration>
		extends ConfigurableComponent<CONFTYPE>, ScopedComponent, SystemConfigurationManager<CONFTYPE> {
	
	@Override
	default Scope getScope() {
		return Scope.SYSTEM;
	}

}
