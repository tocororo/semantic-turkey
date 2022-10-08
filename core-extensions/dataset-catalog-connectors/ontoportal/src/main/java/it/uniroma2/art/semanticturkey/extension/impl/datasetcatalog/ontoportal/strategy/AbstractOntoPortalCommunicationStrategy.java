package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.AbstractOntoPortalConnectorConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

/**
 * A partial implementation of {@link ServerCommunicationStrategy} addressing the common aspects of the
 * interaction with an OntoPortal server.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
abstract class AbstractOntoPortalCommunicationStrategy<T extends AbstractOntoPortalConnectorConfiguration>
		extends ServerCommunicationStrategy {

	private String siteProvidedKey;
	private String siteProvidedAPIBaseURL;

	protected T conf;

	public AbstractOntoPortalCommunicationStrategy(T conf) {
		this.conf = conf;
	}

	@Override
	public void initialize(CloseableHttpClient httpClient) throws IOException {

		if (StringUtils.isNoneBlank(getAPIBaseURL(), getAPIKey())) return; // check if there is need to fetch /site_config

		HttpGet httpRequest = new HttpGet(StringUtils.stripEnd(StringUtils.trim(getFrontendBaseURL()), "/") + "/site_config");
		try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
			StatusLine statusLine = httpResponse.getStatusLine();
			if (statusLine.getStatusCode() / 200 != 1) {
				throw new IOException("HTTP failure when retrieving the site config.\n" + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
			}
			JsonNode config_node = new ObjectMapper().readTree(EntityUtils.toString(httpResponse.getEntity()));

			JsonNode apiKeyNode = config_node.get("apikey");
			if (apiKeyNode != null && apiKeyNode.isTextual()) {
				siteProvidedKey = apiKeyNode.textValue();
			} else {
				throw new IOException("API key not found in site configuration");
			}

			JsonNode restUrlNode = config_node.get("rest_url");
			if (restUrlNode != null && restUrlNode.isTextual()) {
				siteProvidedAPIBaseURL = restUrlNode.textValue();
			} else {
				throw new IOException("REST Base URL not found in site configuration");
			}
		}
	}

	@Override
	public String getFrontendBaseURL() {
		return conf.frontendBaseURL;
	}

	@Override
	public String getAPIBaseURL() {
		String providedURL = StringUtils.trim(this.conf.apiBaseURL);
		if (StringUtils.isNotBlank(providedURL)) {
			return providedURL;
		} else {
			return siteProvidedAPIBaseURL;
		}
	}

	@Override
	public String getAPIKey() {
		String providedKey = StringUtils.trim(this.conf.apiKey);
		if (StringUtils.isNotBlank(providedKey))	{
			return providedKey;
		} else {
			return siteProvidedKey;
		}
	}

	@Override
	public Map<String, String> getHttpRequestHeaders() throws IOException {
		String apiKey = getAPIKey();
		return ImmutableMap.of("Authorization", String.format("apikey token=%s", apiKey));
	}

}
