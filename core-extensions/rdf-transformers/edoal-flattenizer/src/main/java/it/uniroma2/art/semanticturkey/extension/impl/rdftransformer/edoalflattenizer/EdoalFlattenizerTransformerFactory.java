package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalflattenizer;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link EdoalFlattenizerTransformer}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class EdoalFlattenizerTransformerFactory implements
		ConfigurableExtensionFactory<EdoalFlattenizerTransformer, EdoalFlattenizerTransformerConfiguration>,
		PUScopedConfigurableComponent<EdoalFlattenizerTransformerConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.edoalflattenizer.EdoalFlattenizerTransformerFactory";
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
	public EdoalFlattenizerTransformer createInstance(
			EdoalFlattenizerTransformerConfiguration conf) {
		return new EdoalFlattenizerTransformer(conf);
	}

	@Override
	public Collection<EdoalFlattenizerTransformerConfiguration> getConfigurations() {
		return Arrays.asList(new EdoalFlattenizerTransformerConfiguration());
	}

}
