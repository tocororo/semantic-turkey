package it.uniroma2.art.semanticturkey.config.sheet2rdf;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;
import it.uniroma2.art.sheet2rdf.header.NodeConversion;

public class StoredAdvancedGraphApplicationConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.config.sheet2rdf.StoredAdvancedGraphApplicationConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String pattern$description = keyBase + ".pattern.description";
		public static final String pattern$displayName = keyBase + ".pattern.displayName";
		public static final String nodes$description = keyBase + ".nodes.description";
		public static final String nodes$displayName = keyBase + ".nodes.displayName";
		public static final String prefixMapping$description = keyBase + ".prefixMapping.description";
		public static final String prefixMapping$displayName = keyBase + ".prefixMapping.displayName";
		public static final String defaultPredicate$description = keyBase + ".defaultPredicate.description";
		public static final String defaultPredicate$displayName = keyBase + ".defaultPredicate.displayName";
	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}
	
	@STProperty(description = "{" + MessageKeys.pattern$description + "}", displayName = "{" + MessageKeys.pattern$displayName + "}")
	@Required
	public String pattern;
	
	@STProperty(description = "{" + MessageKeys.nodes$description + "}", displayName = "{" + MessageKeys.nodes$displayName + "}")
	@Required
	public List<NodeConversion> nodes;
	
	@STProperty(description = "{" + MessageKeys.prefixMapping$description + "}", displayName = "{" + MessageKeys.prefixMapping$displayName + "}")
	public Map<String, String> prefixMapping;
	
	@STProperty(description = "{" + MessageKeys.defaultPredicate$description + "}", displayName = "{" + MessageKeys.defaultPredicate$displayName + "}")
	public IRI defaultPredicate;

}
