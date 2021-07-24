package it.uniroma2.art.semanticturkey.config.invokablereporter;

import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.Map;

public class AdditionalFile implements STProperties {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.invokablereporter.AdditionalFile";

		public static final String shortName = keyBase + ".shortName";
		public static final String sourcePath$description = keyBase + ".sourcePath.description";
		public static final String sourcePath$displayName = keyBase + ".sourcePath.displayName";
		public static final String destinationPath$description = keyBase + ".destinationPath.description";
		public static final String destinationPath$displayName = keyBase + ".destinationPath.displayName";
		public static final String required$description = keyBase + ".required.description";
		public static final String required$displayName = keyBase + ".required.displayName";
	}

	@Override
	public String getShortName() {
		return MessageKeys.shortName;
	}

	@STProperty(displayName = "{" + MessageKeys.sourcePath$displayName + "}", description = "{" + MessageKeys.sourcePath$description + "}")
	@Required
	public String sourcePath;

	@STProperty(displayName = "{" + MessageKeys.destinationPath$displayName + "}", description = "{"
			+ MessageKeys.destinationPath$description + "}")
	@Required
	public String destinationPath;

	@STProperty(displayName = "{" + MessageKeys.required$displayName + "}", description = "{"
			+ MessageKeys.required$description + "}")
	@Required
	public Boolean required;

}
