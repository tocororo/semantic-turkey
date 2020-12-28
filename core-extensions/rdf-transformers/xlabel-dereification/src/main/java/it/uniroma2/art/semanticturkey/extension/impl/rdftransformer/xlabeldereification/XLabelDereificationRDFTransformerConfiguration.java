package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class XLabelDereificationRDFTransformerConfiguration implements Configuration {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification.XLabelDereificationRDFTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String preserveReifiedLabels$description = keyBase + ".preserveReifiedLabels.description";
		public static final String preserveReifiedLabels$displayName = keyBase + ".preserveReifiedLabels.displayName";
	}


	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.preserveReifiedLabels$description + "}", displayName = "{" + MessageKeys.preserveReifiedLabels$displayName + "}")
	public boolean preserveReifiedLabels = true;
}
