package it.uniroma2.art.semanticturkey.config.customservice;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * The definition of <em>parameter</em> of a custom service operation
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see Operation
 */
public class Parameter implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.customservice.Parameter";

		public static final String shortName = keyBase + ".shortName";
		public static final String name$description = keyBase + ".name.description";
		public static final String name$displayName = keyBase + ".name.displayName";
		public static final String required$description = keyBase + ".required.description";
		public static final String required$displayName = keyBase + ".required.displayName";
		public static final String type$description = keyBase + ".type.description";
		public static final String type$displayName = keyBase + ".type.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(displayName = "{" + MessageKeys.name$displayName + "}", description = "{" + MessageKeys.name$description + "}")
	@Required
	public String name;

	@STProperty(displayName = "{" + MessageKeys.required$displayName + "}", description = "{" + MessageKeys.required$description + "}")
	@Required
	public Boolean required;

	@STProperty(displayName = "{" + MessageKeys.type$displayName + "}", description = "{" + MessageKeys.type$description + "}")
	@Required
	public Type type;
}
