package it.uniroma2.art.semanticturkey.config.invokablereporter;

import java.util.Map;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class ServiceInvocation implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.invokablereporter.ServiceInvocation";

		public static final String shortName = keyBase + ".shortName";
		public static final String extensionPath$description = keyBase + ".extensionPath.description";
		public static final String extensionPath$displayName = keyBase + ".extensionPath.displayName";
		public static final String service$description = keyBase + ".service.description";
		public static final String service$displayName = keyBase + ".service.displayName";
		public static final String operation$description = keyBase + ".operation.description";
		public static final String operation$displayName = keyBase + ".operation.displayName";
		public static final String arguments$description = keyBase + ".arguments.description";
		public static final String arguments$displayName = keyBase + ".arguments.displayName";
		public static final String label$description = keyBase + ".label.description";
		public static final String label$displayName = keyBase + ".label.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String template$description = keyBase + ".template.description";
		public static final String template$displayName = keyBase + ".template.displayName";
	}

	@Override
	public String getShortName() {
		return MessageKeys.shortName;
	}

	@STProperty(displayName = "{" + MessageKeys.extensionPath$displayName + "}", description = "{" + MessageKeys.extensionPath$description + "}")
	public String extensionPath;

	@STProperty(displayName = "{" + MessageKeys.service$displayName + "}", description = "{"
			+ MessageKeys.service$description + "}")
	@Required
	public String service;

	@STProperty(displayName = "{" + MessageKeys.operation$displayName + "}", description = "{"
			+ MessageKeys.operation$description + "}")
	@Required
	public String operation;

	@STProperty(displayName = "{" + MessageKeys.arguments$displayName + "}", description = "{"
			+ MessageKeys.arguments$description + "}")
	public Map<String, String> arguments;

	@STProperty(displayName = "{" + MessageKeys.label$displayName + "}", description = "{"
			+ MessageKeys.label$description + "}")
	public String label;

	@STProperty(displayName = "{" + MessageKeys.description$displayName + "}", description = "{"
			+ MessageKeys.description$description + "}")
	public String description;

	@STProperty(displayName = "{" + MessageKeys.template$displayName + "}", description = "{"
			+ MessageKeys.template$description + "}")
	public String template;

}
