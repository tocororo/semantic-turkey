package it.uniroma2.art.semanticturkey.extension.impl.urigen.template;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

public class NativeTemplateBasedURIGeneratorFactory
		implements NonConfigurableExtensionFactory<NativeTemplateBasedURIGenerator>,
		ProjectSettingsManager<NativeTemplateBasedURIGeneratorSettings> {

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
	public NativeTemplateBasedURIGenerator createInstance() {
		NativeTemplateBasedURIGeneratorSettings settings = new NativeTemplateBasedURIGeneratorSettings();
		return new NativeTemplateBasedURIGenerator(settings);
	}

}
