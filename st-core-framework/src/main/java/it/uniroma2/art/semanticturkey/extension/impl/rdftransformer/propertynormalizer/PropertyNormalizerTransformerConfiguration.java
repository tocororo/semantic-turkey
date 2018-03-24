package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperty;

/**
 * A {@link Configuration} for {@link PropertyNormalizerTransformer}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PropertyNormalizerTransformerConfiguration implements Configuration {

	@Override
	public String getShortName() {
		return "Property Normalizer Transformer";
	}

	@STProperty(description = "Replacement property")
	@Required
	public IRI normalizingProperty;

	@STProperty(description = "Properties that are replaced in the output")
	@Required
	public Set<IRI> propertiesBeingNormalized;
}
