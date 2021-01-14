package it.uniroma2.art.semanticturkey.config.template;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.List;
import java.util.Map;

public class StoredTemplateConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.template.StoredTemplateConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String template$description = keyBase + ".template.description";
		public static final String template$displayName = keyBase + ".template.displayName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.template$description + "}", displayName = "{" + MessageKeys.template$displayName + "}")
	public Map<RDFResourceRole, List<String>> template;

}
