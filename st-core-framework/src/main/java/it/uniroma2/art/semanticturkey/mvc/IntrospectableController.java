package it.uniroma2.art.semanticturkey.mvc;

import it.uniroma2.art.semanticturkey.services.STService;
import it.uniroma2.art.semanticturkey.services.ServiceSpecies;

/**
 * This is a temporary interface introduced to recognized new-style service controllers, for which we
 * developed {@link LegacyAndNewStyleServiceConnectioManagementHandlerInterceptor}.
 * 
 * Old extensions should be recompiled so that the generated controller implements this interface.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface IntrospectableController {
	ServiceSpecies getServiceSpecies();
	STService getService();
}
