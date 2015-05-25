package it.uniroma2.art.semanticturkey.plugin.extpts;

import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import java.util.Map;

/**
 * Extension point for the generation of URIs. Such a generation is performed, when it is not possible or
 * desired to generate a URI based on a provided local name.
 */
public interface URIGenerator {

	/**
	 * Generates a new URI for the identification of a resource. The parameter {@code xRole}
	 * holds the nature of the resource that will be identified with the given URI. Depending on the value of
	 * the parameter {@code xRole}, a conforming converter may generate differently shaped URIs, possibly
	 * using specific arguments passed via the map {@code args}.
	 * 
	 * @param stServiceContext
	 * @param xRole
	 * @param args
	 * @return
	 * @throws URIGenerationException
	 */
	ARTURIResource generateURI(STServiceContext stServiceContext, String xRole,
			Map<String, String> args) throws URIGenerationException;
}
