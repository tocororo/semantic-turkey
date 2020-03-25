package it.uniroma2.art.semanticturkey.extension.extpts.customservice;

import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * This extension point allows for plugging different technologies for the implementation of custom services
 * (see {@link CustomServiceDefinitionStore}).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CustomServiceBackendExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return CustomServiceBackend.class;
	}

	@Override
	public Scope getScope() {
		return Scope.SYSTEM;
	}

}
