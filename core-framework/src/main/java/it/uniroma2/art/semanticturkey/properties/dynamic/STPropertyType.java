package it.uniroma2.art.semanticturkey.properties.dynamic;

import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A type for an {@link STProperties} property.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertyType implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.properties.dynamic.STPropertyType";

		public static final String shortName = keyBase + ".shortName";
		public static final String name$description = keyBase + ".name.description";
		public static final String name$displayName = keyBase + ".name.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.name$description + "}", displayName = "{" + MessageKeys.name$displayName + "}")
	@Required
	@Enumeration(value = { "boolean", "short", "integer", "long", "float", "double", "IRI", "BNode",
			"Resource", "Literal", "RDFValue", "URL", "java.lang.String" })
	public String name = "java.lang.String";

}
