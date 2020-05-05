package it.uniroma2.art.semanticturkey.extension.extpts.loader;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.resources.Scope;

/**
 * The {@link Loader} extension point.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LoaderExtensionPoint implements ExtensionPoint {

	@Override
	public Class<?> getInterface() {
		return Loader.class;
	}

	@Override
	public Scope getScope() {
		return Scope.PROJECT_USER;
	}

}
