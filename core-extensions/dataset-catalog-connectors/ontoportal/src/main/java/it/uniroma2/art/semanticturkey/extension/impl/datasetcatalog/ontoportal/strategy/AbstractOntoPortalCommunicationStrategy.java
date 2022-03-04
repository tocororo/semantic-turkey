package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy;

import com.google.common.collect.ImmutableMap;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.AbstractOntoPortalConnectorConfiguration;
import java.util.Map;

/**
 * A partial implementation of {@link ServerCommunicationStrategy} addressing the common aspects of the
 * interaction with an OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
abstract class AbstractOntoPortalCommunicationStrategy<T extends AbstractOntoPortalConnectorConfiguration>
		implements ServerCommunicationStrategy {

	protected T conf;

	public AbstractOntoPortalCommunicationStrategy(T conf) {
		this.conf = conf;
	}

	@Override
	public Map<String, String> getHttpRequestHeaders() {
		return ImmutableMap.of("Authorization", String.format("apikey token=%s", conf.apiKey));
	}

}
