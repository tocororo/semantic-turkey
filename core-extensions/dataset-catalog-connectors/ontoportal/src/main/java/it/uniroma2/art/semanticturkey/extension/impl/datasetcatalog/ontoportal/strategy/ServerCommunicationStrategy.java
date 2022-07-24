package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.AbstractOntoPortalConnectorConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.EcoPortalConnectorConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.OntoPortalConnectorConfiguration;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.aop.support.AopUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * A strategy interface declaring the required operations to interact with an OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class ServerCommunicationStrategy {

	/**
	 * Returns the fronted base URL to use in the request.
	 */
	public abstract String getFrontendBaseURL();

	/**
	 * Returns the API base URL to use in the request.
	 */
	public abstract String getAPIBaseURL();

	/**
	 * The headers (key,value) that will be included in the request
	 *
	 * @return
	 */
	public abstract Map<String, String> getHttpRequestHeaders() throws IOException;

	/**
	 * Initializes the strategy, fetching potentially useful settings from /site_config at the frontend base URL
	 * @param httpClient
	 * @throws IOException
	 */
	public void initialize(CloseableHttpClient httpClient) throws IOException {

	}

	/**
	 * Returns the concrete strategy suitable for the provided configuration
	 *
	 * @param httpClient
	 * @param conf
     * @return
	 */
	public static ServerCommunicationStrategy getStrategy(CloseableHttpClient httpClient, AbstractOntoPortalConnectorConfiguration conf) throws IOException {
		Class<?> clazz = AopUtils.getTargetClass(conf);

		ServerCommunicationStrategy strategy;
		if (Objects.equals(clazz, OntoPortalConnectorConfiguration.class)) {
			strategy = new OntoPortalCommunicationStrategy((OntoPortalConnectorConfiguration) conf);
		} else if (Objects.equals(clazz, EcoPortalConnectorConfiguration.class)) {
			strategy = new EcoPortalCommunicationStrategy((EcoPortalConnectorConfiguration) conf);
		} else {
			throw new IllegalArgumentException("Unrecognized configuration type");
		}
		strategy.initialize(httpClient);
		return strategy;
	}

}
