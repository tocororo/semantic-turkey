package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy;

import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.EcoPortalConnectorConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * An implementation of {@link ServerCommunicationStrategy} to interact with EcoPortal.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
class EcoPortalCommunicationStrategy
		extends AbstractOntoPortalCommunicationStrategy<EcoPortalConnectorConfiguration> {

	public static final String DEFAULT_FRONTEND_BASE_URL = "http://ecoportal.lifewatch.eu/";

	public EcoPortalCommunicationStrategy(EcoPortalConnectorConfiguration conf) {
		super(conf);
	}

	@Override
	public String getFrontendBaseURL() {
		return DEFAULT_FRONTEND_BASE_URL;
	}
}
