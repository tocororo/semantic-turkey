package it.uniroma2.art.semanticturkey.properties.dynamic;

import java.util.List;

import org.eclipse.rdf4j.model.Literal;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A schema for a property in an {@link STProperties} object.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertySchema implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.properties.dynamic.STPropertySchema";

		public static final String shortName = keyBase + ".shortName";
		public static final String name$description = keyBase + ".name.description";
		public static final String name$displayName = keyBase + ".name.displayName";
		public static final String required$description = keyBase + ".required.description";
		public static final String required$displayName = keyBase + ".required.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String displayName$description = keyBase + ".displayName.description";
		public static final String displayName$displayName = keyBase + ".displayName.displayName";
		public static final String type$description = keyBase + ".type.description";
		public static final String type$displayName = keyBase + ".type.displayName";
		public static final String enumeration$description = keyBase + ".enumeration.description";
		public static final String enumeration$displayName = keyBase + ".enumeration.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.name$description + "}", displayName = "{" + MessageKeys.name$displayName + "}")
	@Required
	public String name;

	@STProperty(description = "{" + MessageKeys.required$description + "}", displayName = "{" + MessageKeys.required$displayName + "}")
	@Required
	public boolean required = false;

	@STProperty(description = "{" + MessageKeys.description$description + "}", displayName = "{" + MessageKeys.description$displayName + "}")
	@Required
	public List<@LanguageTaggedString Literal> description;

	@STProperty(description = "{" + MessageKeys.displayName$description + "}", displayName = "{" + MessageKeys.displayName$displayName + "}")
	@Required
	public List<@LanguageTaggedString Literal> displayName;

	@STProperty(description = "{" + MessageKeys.type$description + "}", displayName = "{" + MessageKeys.type$displayName + "}")
	@Required
	public STPropertyType type = new STPropertyType();
	
	@STProperty(description = "{" + MessageKeys.enumeration$description + "}", displayName = "{" + MessageKeys.enumeration$displayName + "}")
	public STPropertyEnumeration enumeration;

}
