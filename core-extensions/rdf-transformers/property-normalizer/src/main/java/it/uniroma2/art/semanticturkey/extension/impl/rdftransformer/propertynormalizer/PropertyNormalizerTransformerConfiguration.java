package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A {@link Configuration} for {@link PropertyNormalizerTransformer}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PropertyNormalizerTransformerConfiguration implements Configuration {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer.PropertyNormalizerTransformerConfiguration";

		public static final String shortName = keyBase + ".shortName";
		public static final String normalizingProperty$description = keyBase + ".normalizingProperty.description";
		public static final String normalizingProperty$displayName = keyBase + ".normalizingProperty.displayName";
		public static final String propertiesBeingNormalized$description = keyBase + ".propertiesBeingNormalized.description";
		public static final String propertiesBeingNormalized$displayName = keyBase + ".propertiesBeingNormalized.displayName";
	}
	
	@Override
	public String getShortName() {
		return "{" + MessageKeys.shortName + "}";
	}

	@STProperty(description = "{" + MessageKeys.normalizingProperty$description + "}", displayName = "{" + MessageKeys.normalizingProperty$displayName + "}")
	@Required
	@HasRole(RDFResourceRole.property)
	public IRI normalizingProperty;

	@STProperty(description = "{" + MessageKeys.propertiesBeingNormalized$description + "}", displayName = "{" + MessageKeys.propertiesBeingNormalized$displayName + "}")
	@Required
	public Set<@HasRole(RDFResourceRole.property) IRI> propertiesBeingNormalized;
}
