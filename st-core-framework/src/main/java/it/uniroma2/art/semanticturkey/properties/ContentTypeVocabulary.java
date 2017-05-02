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
	
	public static final String URL = "url";
	public static final String FILE = "file";
	public static final String DIRECTORY = "directory";
	
	static {
		registeredTypes.add(URL);
		registeredTypes.add(FILE);
		registeredTypes.add(DIRECTORY);
		registeredTypes.add(BOOLEAN);
	}
	
	public static Collection<String> getContentTypeVocabulary() {
		return registeredTypes;
	}
	
	public static void registerNewContentType(String type) {
		registeredTypes.add(type);
	}
	
}
