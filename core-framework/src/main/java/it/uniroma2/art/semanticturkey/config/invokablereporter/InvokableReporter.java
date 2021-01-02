package it.uniroma2.art.semanticturkey.config.invokablereporter;

import java.util.List;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * An invokable reporter based on the invocation of custom services
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class InvokableReporter implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.invokablereporter.InvokableReporter";

		public static final String shortName = keyBase + ".shortName";
		public static final String label$description = keyBase + ".label.description";
		public static final String label$displayName = keyBase + ".label.displayName";
		public static final String description$description = keyBase + ".description.description";
		public static final String description$displayName = keyBase + ".description.displayName";
		public static final String sections$description = keyBase + ".sections.description";
		public static final String sections$displayName = keyBase + ".sections.displayName";
		public static final String template$description = keyBase + ".template.description";
		public static final String template$displayName = keyBase + ".template.displayName";
		public static final String mimeType$description = keyBase + ".mimeType.description";
		public static final String mimeType$displayName = keyBase + ".mimeType.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.label$description + "}", displayName = "{" + MessageKeys.label$displayName + "}")
	@Required
	public String label;

	@STProperty(description = "{" + MessageKeys.description$description + "}", displayName = "{" + MessageKeys.description$displayName + "}")
	public String description;

	@STProperty(description = "{" + MessageKeys.sections$description + "}", displayName = "{" + MessageKeys.sections$displayName + "}")
	public List<ServiceInvocation> sections;

	@STProperty(description = "{" + MessageKeys.template$description + "}", displayName = "{" + MessageKeys.template$displayName + "}")
	@Required
	public String template = "";

	@STProperty(description = "{" + MessageKeys.mimeType$description + "}", displayName = "{" + MessageKeys.mimeType$displayName + "}")
	@Required
	public String mimeType = "text/plain";

}
