package it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl;

import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEnginePUSettings;

public class SKOSXLRenderingEnginePUSettings extends BaseRenderingEnginePUSettings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl.SKOSXLRenderingEnginePUSettings";

		public static final String shortName = keyBase + ".shortName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
