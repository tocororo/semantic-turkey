package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

/**
 * Factory for the instantiation of {@link JiraBackend}.
 */
public class JiraBackendFactory implements NonConfigurableExtensionFactory<JiraBackend>,
		ProjectSettingsManager<JiraBackendProjectSettings>, PUSettingsManager<JiraBackendPUSettings> {
	
	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira.JiraBackendFactory";
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
	public JiraBackend createInstance() {
		return new JiraBackend(this);
	}

}
