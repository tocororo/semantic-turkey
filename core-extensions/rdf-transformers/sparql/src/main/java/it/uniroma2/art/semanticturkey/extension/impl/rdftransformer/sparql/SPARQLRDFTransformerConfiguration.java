package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SPARQLRDFTransformerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.sparql.SPARQLRDFTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String filter$description = keyBase + ".filter.description";
		public static final String filter$displayName = keyBase + ".filter.displayName";
		public static final String sliced$description = keyBase + ".sliced.description";
		public static final String sliced$displayName = keyBase + ".sliced.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}
	
	@STProperty(description = "{" + MessageKeys.filter$description + "}", displayName = "{" + MessageKeys.filter$displayName + "}")
	@Required
	public String filter;
	
	@STProperty(description = "{" + MessageKeys.sliced$description + "}", displayName = "{"	+ MessageKeys.sliced$displayName + "}")
	public boolean sliced = true;
}
