package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy;

import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.OntoPortalConnectorConfiguration;
import org.apache.commons.lang3.StringUtils;

/**
 * An implementation of {@link ServerCommunicationStrategy} to interact with a generic OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
class OntoPortalCommunicationStrategy
		extends AbstractOntoPortalCommunicationStrategy<OntoPortalConnectorConfiguration>
		implements ServerCommunicationStrategy {

	public OntoPortalCommunicationStrategy(OntoPortalConnectorConfiguration conf) {
		super(conf);
	}

	@Override
	public String getAPIBaseURL() {
		return StringUtils.appendIfMissing(conf.apiBaseURL.trim(), "/");
	}

}