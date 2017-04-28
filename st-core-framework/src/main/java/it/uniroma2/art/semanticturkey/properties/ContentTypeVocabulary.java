package it.uniroma2.art.semanticturkey.properties;

import java.util.Collection;
import java.util.HashSet;

/**
 * a vocabulary of content types to be defined for properties<br/>
 * see annotation: {@link ContentType}
 * 
 * @author Armando Stellato
 *
 */
public class ContentTypeVocabulary {

	static HashSet<String> registeredTypes = new HashSet<String>();
	
	static {
		registeredTypes.add("url");
		registeredTypes.add("file");
		registeredTypes.add("directory");
	}
	
	public static Collection<String> getContentTypeVocabulary() {
		return registeredTypes;
	}
	
	public static void registerNewContentType(String type) {
		registeredTypes.add(type);
	}
	
}
