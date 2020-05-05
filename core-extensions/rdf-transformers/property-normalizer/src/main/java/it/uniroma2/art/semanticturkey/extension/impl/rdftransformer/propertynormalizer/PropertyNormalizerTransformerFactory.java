package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;

/**
 * Factory for the instantiation of {@link PropertyNormalizerTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PropertyNormalizerTransformerFactory implements
		ConfigurableExtensionFactory<PropertyNormalizerTransformer, PropertyNormalizerTransformerConfiguration>,
		PUScopedConfigurableComponent<PropertyNormalizerTransformerConfiguration> {

	@Override
	public String getName() {
		return "Property Normalizer Transformer";
	}

	@Override
	public String getDescription() {
		return "An RDF transformer that normalizes a set of properties replacing their occurrences as predicate in triples with a provided property";
	}

	@Override
	public PropertyNormalizerTransformer createInstance(PropertyNormalizerTransformerConfiguration conf) {
		return new PropertyNormalizerTransformer(conf);
	}

	@Override
	public Collection<PropertyNormalizerTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new PropertyNormalizerTransformerConfiguration());
	}

}
