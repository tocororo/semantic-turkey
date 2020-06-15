package it.uniroma2.art.semanticturkey.settings.alignmentservices;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RemoteAlignmentServiceProjectSettings implements Settings {

	@Override
	public String getShortName() {
		return "Remote alignment service project settings";
	}

	@STProperty(description = "The identifier of a system-level configuration for an alignment service", displayName = "Configuration identifier")
	public String configID;

}
