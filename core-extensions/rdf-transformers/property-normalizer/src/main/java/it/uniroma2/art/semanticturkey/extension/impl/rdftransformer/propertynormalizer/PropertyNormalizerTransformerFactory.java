package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link PropertyNormalizerTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PropertyNormalizerTransformerFactory implements
		ConfigurableExtensionFactory<PropertyNormalizerTransformer, PropertyNormalizerTransformerConfiguration>,
		PUScopedConfigurableComponent<PropertyNormalizerTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.propertynormalizer.PropertyNormalizerTransformerFactory";
		private static final String name = keyBase + ".name";
		private static final String description = keyBase + ".description";
	}

	@Override
	public String getName() {
		return STMessageSource.getMessage(MessageKeys.name);
	}

	@Override
	public String getDescription() {
		return STMessageSource.getMessage(MessageKeys.description);
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
