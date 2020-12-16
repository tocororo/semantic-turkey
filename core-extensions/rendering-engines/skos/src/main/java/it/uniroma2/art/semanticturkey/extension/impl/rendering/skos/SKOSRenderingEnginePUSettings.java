package it.uniroma2.art.semanticturkey.extension.impl.rendering.skos;

import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEnginePUSettings;

public class SKOSRenderingEnginePUSettings extends BaseRenderingEnginePUSettings {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skos.SKOSRenderingEnginePUSettings";

		public static final String shortName = keyBase + ".shortName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
