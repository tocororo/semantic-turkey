package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;

import java.util.Objects;

/**
 * Configuration class for {@link OntoPortalConnectorFactory} targeting a generic OntoPortal instance.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class OntoPortalConnectorConfiguration extends AbstractOntoPortalConnectorConfiguration
		implements Configuration {

	public static class MessageKeys {

		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.OntoPortalConnectorConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	/*
	 * Overrides #isRequiredProperty(String) to tighten the definition of the property "frontendBaseURL", which is
	 * mandatory to target a generic OntoPortal instance
	 */
	@Override
	public boolean isRequiredProperty(String parID) throws PropertyNotFoundException {
		if (Objects.equals(parID, "frontendBaseURL")) {
			return true;
		} else {
			return super.isRequiredProperty(parID);
		}
	}

}
