package it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon;

import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEnginePUSettings;

public class OntoLexLemonRenderingEnginePUSettings extends BaseRenderingEnginePUSettings {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.ontolexlemon.OntoLexLemonRenderingEnginePUSettings";

		public static final String shortName = keyBase + ".shortName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
