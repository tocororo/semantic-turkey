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

	@Override
	public String getShortName() {
		return "SFTP Deployer Configuration";
	}

	@Override
	public String getHTMLWarning() {
		return "The credentials are stored wihtout encryption on the server. "
				+ "Be aware that the system administration could be able to see them.";
	}

	@STProperty(description = "The target host name/address", displayName = "Host")
	@Required
	public String host;

	@STProperty(description = "The target port", displayName = "Port")
	@Required
	public Integer port = 21;

	@STProperty(description = "The fingerprint of the target server. It must have one of the allowed forms are (MD5:)?[\\dA-F][\\dA-F](:[\\dA-F][\\dA-F]){15} and SHA(1|224|256|384|512):[Base64 encoding without trailing =]", displayName = "Server key fingerprint")
	@Required
	public String serverKeyFingerprint;

	@STProperty(description = "Username", displayName = "Username")
	@Required
	public String username;

	@STProperty(description = "Password", displayName = "Password")
	public String password;

	@STProperty(description = "The path on the remote host where data will be deployed onto", displayName = "Destination name")
	@Required
	public String destinationPath;

	@STProperty(description = "Timeout in milliseconds", displayName = "Timeout (ms)")
	@Required
	public Long timeout = 5 * 60 * 1000L;
}
