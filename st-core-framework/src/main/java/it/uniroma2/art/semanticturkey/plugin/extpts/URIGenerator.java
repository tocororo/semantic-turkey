package it.uniroma2.art.semanticturkey.plugin.extpts;

import it.uniroma2.art.semanticturkey.data.id.ARTURIResAndRandomString;
import it.uniroma2.art.semanticturkey.services.STServiceContext;

import java.util.Map;

public interface URIGenerator {
	ARTURIResAndRandomString generateURI(STServiceContext stServiceContext, String template, Map<String, String> valueMapping) throws URIGenerationException;
}
