package it.uniroma2.art.semanticturkey.extension.impl.urigen.coda;

import it.uniroma2.art.coda.converters.impl.TemplateBasedRandomIdGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.CODAURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * Configuration class for {@link CODAURIGeneratorFactory} that uses the converter
 * {@link TemplateBasedRandomIdGenerator}.
 *
 */
public class CODATemplateBasedURIGeneratorConfiguration extends CODAURIGeneratorConfiguration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.urigen.coda.CODATemplateBasedURIGeneratorConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String concept$description = keyBase + ".concept.description";
		public static final String concept$displayName = keyBase + ".concept.displayName";
		public static final String xLabel$description = keyBase + ".xLabel.description";
		public static final String xLabel$displayName = keyBase + ".xLabel.displayName";
		public static final String xNote$description = keyBase + ".xNote.description";
		public static final String xNote$displayName = keyBase + ".xNote.displayName";
		public static final String fallback$description = keyBase + ".fallback.description";
		public static final String fallback$displayName = keyBase + ".fallback.displayName";

	}

	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.concept$description + "}", displayName = "{"
			+ MessageKeys.concept$displayName + "}")
	public String concept = "c_${rand()}";

	@STProperty(description = "{" + MessageKeys.xLabel$description + "}", displayName = "{"
			+ MessageKeys.xLabel$displayName + "}")
	public String xLabel = "xl_${lexicalForm.language}_${rand()}";

	@STProperty(description = "{" + MessageKeys.xNote$description + "}", displayName = "{"
			+ MessageKeys.xNote$displayName + "}")
	public String xNote = "xNote_${rand()}";

	@STProperty(description = "{" + MessageKeys.fallback$description + "}", displayName = "{"
			+ MessageKeys.fallback$displayName + "}")
	public String fallback = "${xRole}_${rand()}";

}