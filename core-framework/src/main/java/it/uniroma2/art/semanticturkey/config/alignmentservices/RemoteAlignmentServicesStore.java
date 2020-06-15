package it.uniroma2.art.semanticturkey.config.alignmentservices;

import it.uniroma2.art.semanticturkey.extension.SystemScopedConfigurableComponent;

/**
 * Stores the coordinates of remote alignment services.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RemoteAlignmentServicesStore
		implements SystemScopedConfigurableComponent<RemoteAlignmentServiceConfiguration> {

	@Override
	public String getId() {
		return RemoteAlignmentServicesStore.class.getName();
	}
}
