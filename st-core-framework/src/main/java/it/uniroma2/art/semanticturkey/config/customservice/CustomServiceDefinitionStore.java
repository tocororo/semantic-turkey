package it.uniroma2.art.semanticturkey.config.customservice;

import it.uniroma2.art.semanticturkey.extension.SystemScopedConfigurableComponent;

/**
 * A storage for custom service definitions.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * 
 */
public class CustomServiceDefinitionStore implements SystemScopedConfigurableComponent<CustomServiceDefinition> {

	@Override
	public String getId() {
		return CustomServiceDefinitionStore.class.getName();
	}

}
