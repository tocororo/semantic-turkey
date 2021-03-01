package it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs;

import it.uniroma2.art.semanticturkey.extension.impl.rendering.AbstractLabelBasedRenderingEngineConfiguration;

public class RDFSRenderingEngineConfiguration extends AbstractLabelBasedRenderingEngineConfiguration {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs.RDFSRenderingEngineConfiguration";

		public static final String shortName = keyBase + ".shortName";

	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

}
