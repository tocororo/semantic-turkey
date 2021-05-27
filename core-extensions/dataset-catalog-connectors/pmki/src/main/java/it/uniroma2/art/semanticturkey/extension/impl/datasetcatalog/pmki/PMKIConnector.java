package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.pmki;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.i18n.I18NConstants;
import it.uniroma2.art.semanticturkey.mvc.RequestMappingHandlerAdapterPostProcessor;
import it.uniroma2.art.semanticturkey.showvoc.ShowVocConstants;

/**
 * An {@link DatasetCatalogConnector} for <a href="https://data.europa.eu/euodp">Public Multilingual Knowledge
 * Infrastructure</a> (PMKI).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PMKIConnector implements DatasetCatalogConnector {

	private static final Logger logger = LoggerFactory.getLogger(PMKIConnector.class);

	private PMKIConnectorConfiguration conf;

	private ExtensionPointManager exptManager;

	public PMKIConnector(PMKIConnectorConfiguration conf, ExtensionPointManager exptManager) {
		this.conf = conf;
		this.exptManager = exptManager;
	}

	protected PMKIClient createPmkiClient() throws MalformedURLException {
		return new PMKIClient(exptManager, conf.apiBaseURL, ShowVocConstants.SHOWVOC_VISITOR_EMAIL,
				ShowVocConstants.SHOWVOC_VISITOR_PWD);
	}

	@Override
	public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
			Map<String, List<String>> facets, int page) throws IOException {
		if (page < 0) {
			throw new IllegalArgumentException("Page number is negative: " + page);
		}

		try (PMKIClient pmkiClient = createPmkiClient()) {
			try {
				pmkiClient.doLogin();

				return pmkiClient.searchDataset(query, facets, page);

			} finally {
				pmkiClient.doLogout();
			}
		}
	}

	@Override
	public DatasetDescription describeDataset(String id) throws IOException {
		try (PMKIClient pmkiClient = createPmkiClient()) {
			try {
				pmkiClient.doLogin();

				return pmkiClient.describeDataset(id, conf.apiBaseURL, conf.frontendBaseURL);

			} finally {
				pmkiClient.doLogout();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		PMKIConnectorConfiguration conf = new PMKIConnectorConfiguration();
		conf.apiBaseURL = "http://localhost:1979/semanticturkey/";
		conf.frontendBaseURL = "http://localhost:1979/pmki/";
		PMKIConnector connector = new PMKIConnectorFactory().createInstance(conf);

		SearchResultsPage<DatasetSearchResult> results = connector.searchDataset("test",
				Collections.emptyMap(), 0);
		System.out.println(results);
	}

}

class PMKIClient implements AutoCloseable {

	public static final String AUTH_SERVICE_CLASS = "Auth";

	public static final String CORE_SERVICES_PATH = "it.uniroma2.art.semanticturkey/st-core-services/";

	public static final String PMKI_SERVICE_CLASS = "PMKI";

	private String apiBaseURL;
	private String email;
	private String password;

	private CloseableHttpClient httpClient;
	private ObjectMapper objectMapper;

	public PMKIClient(ExtensionPointManager exptManager, String apiBaseURL, String email, String password)
			throws MalformedURLException {
		this.apiBaseURL = apiBaseURL;
		this.email = email;
		this.password = password;

		URL apiBaseURLobj = new URL(apiBaseURL);
		BasicCookieStore cookieStore = new BasicCookieStore();
		BasicClientCookie cookie = new BasicClientCookie(I18NConstants.LANG_COOKIE_NAME,
				LocaleContextHolder.getLocale().toString());
		cookie.setDomain(apiBaseURLobj.getHost());
		cookie.setPath(apiBaseURLobj.getPath());
		cookieStore.addCookie(cookie);
		this.httpClient = HttpClientBuilder.create().useSystemProperties().setDefaultCookieStore(cookieStore)
				.build();
		this.objectMapper = RequestMappingHandlerAdapterPostProcessor.createObjectMapper(exptManager);
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}

	@SuppressWarnings("unchecked")
	private <T> T execute(HttpUriRequest httpRequest, JavaType valueType)
			throws ClientProtocolException, IOException {
		try (CloseableHttpResponse httpReponse = httpClient.execute(httpRequest)) {
			int statusCode = httpReponse.getStatusLine().getStatusCode();
			if ((statusCode / 100) != 2) {
				throw new IOException(
						"The request to the remote machine failed: " + httpReponse.getStatusLine());
			}

			@Nullable
			HttpEntity entity = httpReponse.getEntity();

			if (entity != null) {
				ContentType contentType = ContentType.getOrDefault(entity);
				String mimeType = contentType.getMimeType();

				if (ContentType.APPLICATION_JSON.getMimeType().equals(mimeType)) {
					try (InputStreamReader reader = new InputStreamReader(entity.getContent(),
							Optional.ofNullable(contentType.getCharset()).orElse(StandardCharsets.UTF_8))) {
						JsonNode reponseTree = this.objectMapper.readTree(reader);
						JsonNode stresponseJson = reponseTree.get("stresponse");

						if (stresponseJson != null) {
							JsonNode exceptionJson = stresponseJson.get("exception");
							JsonNode msgJson = stresponseJson.get("msg");
							if (exceptionJson != null && msgJson != null) {
								throw new IOException("Remote machine error: " + msgJson.textValue());
							}
						}

						JsonNode responseContentJson = reponseTree.get("result");

						if (responseContentJson != null) {
							if (valueType.hasRawClass(ObjectNode.class)) {
								return (T) (ObjectNode) responseContentJson;
							}

							return (T) objectMapper.readValue(objectMapper.treeAsTokens(responseContentJson),
									valueType);

						} else {
							if (!valueType.equals(TypeFactory.defaultInstance().constructType(void.class))) {
								throw new IOException(

										"The response from the remote machine doesn't contain a result");
							} else {
								return null;
							}
						}

					}
				} else {
					throw new IOException(
							"The remote machine returned an unexpected content type: " + contentType);
				}
			} else {
				throw new IOException("No entity returned by the remote machine");
			}
		}
	}

	public ObjectNode doLogin() throws IOException {
		String requestURL = UriComponentsBuilder.fromHttpUrl(apiBaseURL).path(CORE_SERVICES_PATH)
				.path(AUTH_SERVICE_CLASS).path("/login").build().toUriString();

		HttpPost httpPost = new HttpPost(requestURL);
		List<NameValuePair> params = new ArrayList<>(2);
		params.add(new BasicNameValuePair("email", email));
		params.add(new BasicNameValuePair("password", password));
		httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		ObjectNode reponse = execute(httpPost, TypeFactory.defaultInstance().constructType(ObjectNode.class));

		return reponse;
	}

	public void doLogout() throws IOException {
		String requestURL = UriComponentsBuilder.fromHttpUrl(apiBaseURL).path(CORE_SERVICES_PATH)
				.path(AUTH_SERVICE_CLASS).path("/logout").build().toUriString();
		HttpPost httpPost = new HttpPost(requestURL);
		execute(httpPost, TypeFactory.defaultInstance().constructType(void.class));
	}

	public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
			Map<String, List<String>> facets, int page) throws IOException {
		String requestURL = UriComponentsBuilder.fromHttpUrl(apiBaseURL).path(CORE_SERVICES_PATH)
				.path(PMKI_SERVICE_CLASS).path("/searchDataset").queryParam("query", query)
				.queryParam("facets", objectMapper.writeValueAsString(facets)).queryParam("page", page)
				.build(false).encode().toUriString();
		HttpGet httpGet = new HttpGet(requestURL);
		SearchResultsPage<DatasetSearchResult> resultsPage = execute(httpGet, TypeFactory.defaultInstance()
				.constructParametricType(SearchResultsPage.class, DatasetSearchResult.class));
		return resultsPage;
	}

	public DatasetDescription describeDataset(String id, @Nullable String apiBaseURL,
			@Nullable String frontendBaseURL) throws IOException {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiBaseURL);
		uriBuilder.path(CORE_SERVICES_PATH).path(PMKI_SERVICE_CLASS).path("/describeDataset").queryParam("id",
				id);
		if (apiBaseURL != null) {
			uriBuilder.queryParam("apiBaseURL", apiBaseURL);
		}

		if (frontendBaseURL != null) {
			uriBuilder.queryParam("frontendBaseURL", frontendBaseURL);
		}

		String requestURL = uriBuilder.build(false).encode().toUriString();
		HttpGet httpGet = new HttpGet(requestURL);
		DatasetDescription datasetMetadata = execute(httpGet,
				TypeFactory.defaultInstance().constructType(DatasetDescription.class));

		return datasetMetadata;
	}

}