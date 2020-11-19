package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.strategy;

import it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal.OntoPortalDeployerConfiguration;

/**
 * An implementation of {@link ServerCommunicationStrategy} to interact with a generic OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
class OntoPortalCommunicationStrategy
		extends AbstractOntoPortalCommunicationStrategy<OntoPortalDeployerConfiguration>
		implements ServerCommunicationStrategy {

	public OntoPortalCommunicationStrategy(OntoPortalDeployerConfiguration conf) {
		super(conf);
	}

	@Override
	public String getAPIBaseURL() {
		return conf.apiBaseURL.trim();
	}

}
