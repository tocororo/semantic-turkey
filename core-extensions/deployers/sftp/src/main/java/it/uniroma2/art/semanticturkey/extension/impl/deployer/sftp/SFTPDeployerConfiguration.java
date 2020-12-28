package it.uniroma2.art.semanticturkey.extension.impl.deployer.sftp;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link SFTPDeployerFactory}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SFTPDeployerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.sftp.SFTPDeployerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String htmlWarning = keyBase + ".htmlWarning";
		public static final String host$description = keyBase + ".host.description";
		public static final String host$displayName = keyBase + ".host.displayName";
		public static final String port$description = keyBase + ".port.description";
		public static final String port$displayName = keyBase + ".port.displayName";
		public static final String serverKeyFingerprint$description = keyBase + ".serverKeyFingerprint.description";
		public static final String serverKeyFingerprint$displayName = keyBase + ".serverKeyFingerprint.displayName";
		public static final String username$description = keyBase + ".username.description";
		public static final String username$displayName = keyBase + ".username.displayName";
		public static final String password$description = keyBase + ".password.description";
		public static final String password$displayName = keyBase + ".password.displayName";
		public static final String destinationPath$description = keyBase + ".destinationPath.description";
		public static final String destinationPath$displayName = keyBase + ".destinationPath.displayName";
		public static final String timeout$description = keyBase + ".timeout.description";
		public static final String timeout$displayName = keyBase + ".timeout.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@Override
	public String getHTMLWarning() {
		return "{" + MessageKeys.htmlWarning + "}";
	}

	@STProperty(description = "{" + MessageKeys.host$description + "}", displayName = "{" + MessageKeys.host$displayName + "}")
	@Required
	public String host;

	@STProperty(description = "{" + MessageKeys.port$description + "}", displayName = "{" + MessageKeys.port$displayName + "}")
	@Required
	public Integer port = 21;

	@STProperty(description = "{" + MessageKeys.serverKeyFingerprint$description + "}", displayName = "{" + MessageKeys.serverKeyFingerprint$displayName + "}")
	@Required
	public String serverKeyFingerprint;

	@STProperty(description = "{" + MessageKeys.username$description + "}", displayName = "{" + MessageKeys.username$displayName + "}")
	@Required
	public String username;

	@STProperty(description = "{" + MessageKeys.password$description + "}", displayName = "{" + MessageKeys.password$displayName + "}")
	public String password;

	@STProperty(description = "{" + MessageKeys.destinationPath$description + "}", displayName = "{" + MessageKeys.destinationPath$displayName + "}")
	@Required
	public String destinationPath;

	@STProperty(description = "{" + MessageKeys.timeout$description + "}", displayName = "{" + MessageKeys.timeout$displayName + "}")
	@Required
	public Long timeout = 5 * 60 * 1000L;
}
