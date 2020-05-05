package it.uniroma2.art.semanticturkey.extension.extpts.rendering;

import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

public class RenderingEnginePUSettings implements Settings {

	@Override
	public String getShortName() {
		return "Project user settings for RenderingEngine";
	}

	@STProperty(description = "Comma-separated list of languages that should be used to compute a rendering. An asterisk \"*\" means all languages")
	@Required
	public String languages = "*";
}
