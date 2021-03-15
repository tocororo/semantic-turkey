package it.uniroma2.art.semanticturkey.extension.impl.rendering.skos;

import it.uniroma2.art.semanticturkey.extension.impl.rendering.AbstractLabelBasedRenderingEngineConfiguration;

public class SKOSRenderingEngineConfiguration extends AbstractLabelBasedRenderingEngineConfiguration {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.skos.SKOSRenderingEngineConfiguration";

		public static final String shortName = keyBase + ".shortName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
