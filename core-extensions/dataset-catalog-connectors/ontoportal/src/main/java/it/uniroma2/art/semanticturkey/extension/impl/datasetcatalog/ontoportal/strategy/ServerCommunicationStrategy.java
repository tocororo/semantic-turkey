package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy;

import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.AbstractOntoPortalConnectorConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.EcoPortalConnectorConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.OntoPortalConnectorConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;

import java.util.Map;
import java.util.Objects;

/**
 * A strategy interface declaring the required operations to interact with an OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public interface ServerCommunicationStrategy {

	/**
	 * Returns the API base URL to use in the request.
	 */
	String getAPIBaseURL();

	/**
	 * The headers (key,value) that will be included in the request
	 *
	 * @return
	 */
	Map<String, String> getHttpRequestHeaders();

	/**
	 * Returns the concrete strategy suitable for the provided configuration
	 *
	 * @param conf
	 * @return
	 */
	static ServerCommunicationStrategy getStrategy(AbstractOntoPortalConnectorConfiguration conf) {
		Class<?> clazz = AopUtils.getTargetClass(conf);

		if (Objects.equals(clazz, OntoPortalConnectorConfiguration.class)) {
			return new OntoPortalCommunicationStrategy((OntoPortalConnectorConfiguration) conf);
		} else if (Objects.equals(clazz, EcoPortalConnectorConfiguration.class)) {
			return new EcoPortalCommunicationStrategy((EcoPortalConnectorConfiguration) conf);
		} else {
			throw new IllegalArgumentException("Unrecognized configuration type");
		}
	}

}
