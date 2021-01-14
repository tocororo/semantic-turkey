package it.uniroma2.art.semanticturkey.config.sparql;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Enumeration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class StoredSPARQLOperation implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLOperation";

		public static final String shortName = keyBase + ".shortName";
		public static final String sparql$description = keyBase + ".sparql.description";
		public static final String sparql$displayName = keyBase + ".sparql.displayName";
		public static final String type$description = keyBase + ".type.description";
		public static final String type$displayName = keyBase + ".type.displayName";
		public static final String includeInferred$description = keyBase + ".includeInferred.description";
		public static final String includeInferred$displayName = keyBase + ".includeInferred.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description="{" + MessageKeys.sparql$description + "}", displayName = "{" + MessageKeys.sparql$displayName + "}")
	@Required
	public String sparql;

	@STProperty(description="{" + MessageKeys.type$description + "}", displayName = "{" + MessageKeys.type$displayName + "}")
	@Required
	@Enumeration({"query", "update"})
	public String type;
	
	@STProperty(description="{" + MessageKeys.type$description + "}", displayName = "{" + MessageKeys.type$displayName + "}")
	@Required
	public Boolean includeInferred;
}
