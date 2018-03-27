package it.uniroma2.art.semanticturkey.plugin.impls.collaboration;

import it.uniroma2.art.semanticturkey.plugin.configuration.AbstractPluginConfiguration;

/**
 * Base configuration class for {@link JiraBackend}.
 *
 */
public class JiraBackendConfiguration extends AbstractPluginConfiguration {

	@Override
	public String getShortName() {
		return "JIRA Collaboratoon Backend";
	}

}
