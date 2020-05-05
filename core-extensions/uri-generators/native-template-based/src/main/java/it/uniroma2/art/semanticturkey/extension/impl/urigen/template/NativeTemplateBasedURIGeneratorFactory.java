package it.uniroma2.art.semanticturkey.extension.impl.urigen.template;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

public class NativeTemplateBasedURIGeneratorFactory
		implements NonConfigurableExtensionFactory<NativeTemplateBasedURIGenerator>,
		ProjectSettingsManager<NativeTemplateBasedURIGeneratorSettings> {

	@Override
	public String getName() {
		return "Native Template-based URI Generator";
	}

	@Override
	public String getDescription() {
		return "A URI Generator that instantiates templates associated with different xRoles";
	}

	@Override
	public NativeTemplateBasedURIGenerator createInstance() {
		NativeTemplateBasedURIGeneratorSettings settings = new NativeTemplateBasedURIGeneratorSettings();
		return new NativeTemplateBasedURIGenerator(settings);
	}

}
