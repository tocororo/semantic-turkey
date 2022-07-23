package it.uniroma2.art.semanticturkey.extension.impl.deployer.showvoc;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration abstract base class for {@link ShowVocDeployer}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class ShowVocDeployerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.showvoc.ShowVocDeployerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String stHost$description = keyBase + ".stHost.description";
		public static final String stHost$displayName = keyBase + ".stHost.displayName";
		public static final String vbUrl$description = keyBase + ".vbUrl.description";
		public static final String vbUrl$displayName = keyBase + ".vbUrl.displayName";
		public static final String username$description = keyBase + ".username.description";
		public static final String username$displayName = keyBase + ".username.displayName";
		public static final String password$description = keyBase + ".password.description";
		public static final String password$displayName = keyBase + ".password.displayName";
		public static final String project$description = keyBase + ".project.description";
		public static final String project$displayName = keyBase + ".project.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}

	@STProperty(description = "{" + ShowVocDeployerConfiguration.MessageKeys.stHost$description + "}", displayName = "{" + ShowVocDeployerConfiguration.MessageKeys.stHost$displayName + "}")
	@Required
	public String stHost;

	@STProperty(description = "{" + ShowVocDeployerConfiguration.MessageKeys.vbUrl$description + "}", displayName = "{" + ShowVocDeployerConfiguration.MessageKeys.vbUrl$displayName + "}")
	@Required
	public String vbUrl;

	@STProperty(description = "{" + ShowVocDeployerConfiguration.MessageKeys.username$description + "}", displayName = "{" + ShowVocDeployerConfiguration.MessageKeys.username$displayName + "}")
	@Required
	public String username;

	@STProperty(description = "{" + ShowVocDeployerConfiguration.MessageKeys.password$description + "}", displayName = "{" + ShowVocDeployerConfiguration.MessageKeys.password$displayName + "}")
	@Required
	public String password;

	@STProperty(description = "{" + ShowVocDeployerConfiguration.MessageKeys.project$description + "}", displayName = "{" + ShowVocDeployerConfiguration.MessageKeys.project$displayName + "}")
	@Required
	public String project;
}
