package it.uniroma2.art.semanticturkey.config.template;

import it.uniroma2.art.semanticturkey.extension.SystemScopedConfigurableComponent;

public class TemplateStore implements SystemScopedConfigurableComponent<StoredTemplateConfiguration> {

	@Override
	public String getId() {
		return TemplateStore.class.getName();
	}

}
