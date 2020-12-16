package it.uniroma2.art.semanticturkey.config.customservice;

import java.util.List;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * The definition of a <em>custom service</em>, which supports the implementation and deployment of an ST
 * service without the need to write, build and deploy Java code.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CustomService implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customservice.CustomService";

		public static final String shortName = keyBase + ".shortName";
		public static final String name$description = keyBase + ".name.description";
		public static final String name$displayName = keyBase + ".name.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String operations$description = keyBase + ".operations.description";
		public static final String operations$displayName = keyBase + ".operations.displayName";

	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.name$description+ "}", displayName = "{" + MessageKeys.name$displayName + "}")
	@Required
	public String name;

	@STProperty(description = "{" + MessageKeys.description$description+ "}", displayName = "{" + MessageKeys.description$displayName + "}")
	public String description;

	@STProperty(description = "{" + MessageKeys.operations$description+ "}", displayName = "{" + MessageKeys.operations$displayName + "}")
	public List<Operation> operations;

}
