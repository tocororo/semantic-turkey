package it.uniroma2.art.semanticturkey.extension.extpts.loader;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * The {@link StreamTargetingLoader} extension point.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StreamTargetingLoaderExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return StreamTargetingLoader.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT_USER;
	}

}
