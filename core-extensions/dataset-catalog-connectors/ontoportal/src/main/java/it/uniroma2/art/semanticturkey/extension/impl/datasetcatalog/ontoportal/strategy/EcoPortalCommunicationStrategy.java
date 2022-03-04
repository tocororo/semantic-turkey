package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy;

import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.EcoPortalConnectorConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * An implementation of {@link ServerCommunicationStrategy} to interact with EcoPortal.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
class EcoPortalCommunicationStrategy
		extends AbstractOntoPortalCommunicationStrategy<EcoPortalConnectorConfiguration>
		implements ServerCommunicationStrategy {

	public static final String DEFAULT_API_BASE_URL = "http://ecoportal.lifewatchitaly.eu:8080/";

	public EcoPortalCommunicationStrategy(EcoPortalConnectorConfiguration conf) {
		super(conf);
	}

	@Override
	public String getAPIBaseURL() {
		return StringUtils.appendIfMissing(ObjectUtils.firstNonNull(StringUtils.trim(conf.apiBaseURL), DEFAULT_API_BASE_URL), "/");
	}

}
