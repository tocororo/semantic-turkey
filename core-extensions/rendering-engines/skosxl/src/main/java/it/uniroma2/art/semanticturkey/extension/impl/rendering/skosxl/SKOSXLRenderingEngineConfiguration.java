package it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl;

import it.uniroma2.art.semanticturkey.extension.impl.rendering.AbstractLabelBasedRenderingEngineConfiguration;

public class SKOSXLRenderingEngineConfiguration extends AbstractLabelBasedRenderingEngineConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skosxl.SKOSXLRenderingEngineConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
