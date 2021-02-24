package it.uniroma2.art.semanticturkey.extension.impl.urigen.template;

import java.util.Arrays;
import java.util.Collection;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

public class NativeTemplateBasedURIGeneratorFactory implements
		ConfigurableExtensionFactory<NativeTemplateBasedURIGenerator, NativeTemplateBasedURIGeneratorConfiguration> {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGeneratorFactory";
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
	public NativeTemplateBasedURIGenerator createInstance(NativeTemplateBasedURIGeneratorConfiguration conf)
			throws InvalidConfigurationException {
		return new NativeTemplateBasedURIGenerator(conf);
	}

	@Override
	public Collection<NativeTemplateBasedURIGeneratorConfiguration> getConfigurations() {
		return Arrays.asList(new NativeTemplateBasedURIGeneratorConfiguration());
	}

}
