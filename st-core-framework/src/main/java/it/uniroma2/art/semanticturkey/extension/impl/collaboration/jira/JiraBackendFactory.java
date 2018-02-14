package it.uniroma2.art.semanticturkey.extension.impl.collaboration.jira;

import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.settings.PUSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.ProjectSettingsManager;

/**
 * Factory for the instantiation of {@link JiraBackend}.
 */
public class JiraBackendFactory implements NonConfigurableExtensionFactory<JiraBackend>,
		ProjectSettingsManager<JiraBackendProjectSettings>, PUSettingsManager<JiraBackendPUSettings> {

	@Override
	public String getName() {
		return "Jira Backend";
	}

	@Override
	public String getDescription() {
		return "Use Atlassian Jira as a collaboration backend";
	}

	@Override
	public JiraBackend createInstance() {
		return new JiraBackend(this);
	}

}
