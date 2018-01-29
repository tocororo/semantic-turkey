package it.uniroma2.art.semanticturkey.extension;

import java.util.Collection;

import it.uniroma2.art.semanticturkey.resources.Scope;

public interface ExtensionPointManager {

	/**
	 * Returns known extension points.
	 * 
	 * @param scopes
	 *            if not empty, indicates the scopes we are interested in. Otherwise, every scope is
	 *            considered.
	 * @return
	 */
	Collection<ExtensionPoint> getExtensionPoints(Scope... scopes);

	/**
	 * Returns an extension point given its identifier
	 * 
	 * @param identifier
	 * @return
	 * @throws NoSuchExtensionPointException
	 */
	ExtensionPoint getExtensionPoint(String identifier) throws NoSuchExtensionPointException;

}
