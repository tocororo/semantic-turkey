package it.uniroma2.art.semanticturkey.properties.dynamic;

import java.util.List;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * An enumeration for an {@link STProperties} property.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STPropertyEnumeration implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.properties.dynamic.STPropertyEnumeration";

		public static final String shortName = keyBase + ".shortName";
		public static final String values$description = keyBase + ".values.description";
		public static final String values$displayName = keyBase + ".values.displayName";
		public static final String open$description = keyBase + ".open.description";
		public static final String open$displayName = keyBase + ".open.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.values$description + "}", displayName = "{" + MessageKeys.values$displayName + "}")
	@Required
	public List<String> values;

	@STProperty(description = "{" + MessageKeys.open$description + "}", displayName = "{" + MessageKeys.open$displayName + "}")
	@Required
	public boolean open;
}
