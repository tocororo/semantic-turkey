package it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql;

import it.uniroma2.art.semanticturkey.config.customservice.Operation;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link SPARQLCustomServiceBackendFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLOperation extends Operation {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.customservice.sparql.SPARQLOperation";

		public static final String shortName = keyBase + ".shortName";
		public static final String sparql$description = keyBase + ".sparql.description";
		public static final String sparql$displayName = keyBase + ".sparql.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.sparql$description + "}", displayName = "{"
			+ MessageKeys.sparql$displayName + "}")
	@Required
	public String sparql;

}
