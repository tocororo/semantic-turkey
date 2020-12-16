package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import java.util.Objects;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;

/**
 * Configuration class for {@link OntoPortalDeployerFactory} targeting a generic OntoPortal instance.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OntoPortalDeployerConfiguration extends AbstractOntoPortalDeployerConfiguration
		implements Configuration {

	public static class MessageKeys {

		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.OntoPortalDeployerConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	/*
	 * Overrides #isRequiredProperty(String) to tighten the definition of the property "apiBaseURL", which is
	 * mandatory to target a generic OntoPortal instance
	 */
	@Override
	public boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
		if (Objects.equals(parID, "apiBaseURL")) {
			return true;
		} else {
			return super.isRequiredProperty(parID);
		}
	}

}
