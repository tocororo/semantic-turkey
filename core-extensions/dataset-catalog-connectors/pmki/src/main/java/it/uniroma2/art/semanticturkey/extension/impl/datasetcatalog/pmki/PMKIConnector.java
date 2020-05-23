package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.pmki;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DownloadDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.mdr.core.CatalogRecord;
import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;
import it.uniroma2.art.semanticturkey.mvc.RequestMappingHandlerAdapterPostProcessor;

/**
 * An {@link DatasetCatalogConnector} for <a href="https://data.europa.eu/euodp">Public Multilingual Knowledge
 * Infrastructure</a> (PMKI).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PMKIConnector implements DatasetCatalogConnector {

	public static final int PAGE_SIZE = 10;

	private static final Logger logger = LoggerFactory.getLogger(PMKIConnector.class);

	private PMKIConnectorConfiguration conf;

	public PMKIConnector(PMKIConnectorConfiguration conf) {
		this.conf = conf;

	}

	@Override
	public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
			Map<String, List<String>> facets, int page) throws IOException {
		if (page < 0) {
			throw new IllegalArgumentException("Page number is negative: " + page);
		}

		try (PMKIClient pmkiClient = new PMKIClient(conf.apiBaseURL, conf.email, conf.password)) {
			pmkiClient.doLogin();

			query = query.trim();
			if (StringUtils.isAllBlank(query)) {
				return new SearchResultsPage<>(0, PAGE_SIZE, page, Collections.emptyList(),
						Collections.emptyList());
			}

			List<String> tokens = Arrays.stream(query.split("\\s+")).map(String::toLowerCase)
					.collect(Collectors.toList());

			Collection<CatalogRecord> catalogRecords = pmkiClient.getCatalogRecords();

			List<DatasetSearchResult> searchResults = new ArrayList<>();

			for (CatalogRecord record : catalogRecords) {
				DatasetMetadata datasetMetadata = record.getAbstractDataset();
				String title = datasetMetadata.getTitle().map(String::toLowerCase).orElse("");
				String description = datasetMetadata.getDescription().map(String::toLowerCase).orElse("");

				int matches = 0;
				for (String token : tokens) {
					if (title.contains(token) || description.contains(token)) {
						matches += 1;
					}
				}

				if (matches > 0) {
					IRI ontologyIRI = null;
					double score = (double) matches / tokens.size();

					URL datasetPage = null;
					List<Literal> titles = datasetMetadata.getTitle().map(
							s -> Collections.singletonList(SimpleValueFactory.getInstance().createLiteral(s)))
							.orElse(Collections.emptyList());
					List<Literal> descriptions = Collections.emptyList();
					Map<String, List<String>> facets2 = Collections.emptyMap();
					String id = datasetMetadata.getIdentity().toString();

					DatasetSearchResult datasetSearchResult = new DatasetSearchResult(id, ontologyIRI, score,
							datasetPage, titles, descriptions, facets2);

					searchResults.add(datasetSearchResult);
				}
			}

			int totalResults = searchResults.size();
			List<DatasetSearchResult> searchResultsPage = searchResults.stream().skip(page * PAGE_SIZE).limit(PAGE_SIZE)
					.collect(Collectors.toList());
			return new SearchResultsPage<>(totalResults, PAGE_SIZE, page, searchResultsPage,
					Collections.emptyList());
		}
	}

	@Override
	public DatasetDescription describeDataset(String id) throws IOException {
		try (PMKIClient pmkiClient = new PMKIClient(conf.apiBaseURL, conf.email, conf.password)) {
			pmkiClient.doLogin();
			IRI datasetIRI = SimpleValueFactory.getInstance().createIRI(id);

			DatasetMetadata datasetMetadata = pmkiClient.getDatasetMetadata(datasetIRI);
			IRI lexicalizationModel = pmkiClient.getComputedLexicalizationModel(datasetIRI);

			IRI ontologyIRI = null;
			URL datasetPage = null;
			List<Literal> titles = datasetMetadata.getTitle()
					.map(s -> Collections.singletonList(SimpleValueFactory.getInstance().createLiteral(s)))
					.orElse(Collections.emptyList());
			List<Literal> descriptions = Collections.emptyList();
			Map<String, List<String>> facets = Collections.emptyMap();
			String uriPrefix = datasetMetadata.getUriSpace().orElse(null);
			List<DownloadDescription> dataDumps = Collections.emptyList();
			URL sparqlEndpoint = datasetMetadata.getSparqlEndpoint().map(iri -> {
				try {
					return URI.create(iri.toString()).toURL();
				} catch (MalformedURLException e) {
					throw new RuntimeException("Malformed SPARQL Endpoint URL: " + iri.toString(), e);
				}
			}).orElse(null);
			IRI model = SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2004/02/skos/core");
			DatasetDescription datasetDescription = new DatasetDescription(id, ontologyIRI, datasetPage,
					titles, descriptions, facets, uriPrefix, dataDumps, sparqlEndpoint, model,
					lexicalizationModel);

			return datasetDescription;
		}
	}

	public static void main(String[] args) throws IOException {
		PMKIConnector connector = new PMKIConnectorFactory().createInstance();

		SearchResultsPage<DatasetSearchResult> results = connector.searchDataset("test agrovoc",
				Collections.emptyMap(), 0);
		System.out.println(results);
	}

}

class PMKIClient implements AutoCloseable {

	private static final String AUTH_SERVICE_CLASS = "Auth";

	private static final String CORE_SERVICES_PATH = "it.uniroma2.art.semanticturkey/st-core-services/";

	private static final String METADATA_REGISTRY_SERVICE_CLASS = "MetadataRegistry";

	private static final String METADATA_REGISTRY_SERVICES_PATH = "it.uniroma2.art.semanticturkey/st-metadata-registry-services/";

	private String apiBaseURL;
	private String email;
	private String password;

	private CloseableHttpClient httpClient;
	private ObjectMapper objectMapper;

	public PMKIClient(String apiBaseURL, String email, String password) {
		this.apiBaseURL = apiBaseURL;
		this.email = email;
		this.password = password;

		this.httpClient = HttpClientBuilder.create().build();
		this.objectMapper = RequestMappingHandlerAdapterPostProcessor.createObjectMapper();
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
							throw new IOException(
									"The response from the remote machine doesn't contain a result");
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

	public DatasetMetadata getDatasetMetadata(IRI dataset) throws IOException {
		String requestURL = UriComponentsBuilder.fromHttpUrl(apiBaseURL).path(METADATA_REGISTRY_SERVICES_PATH)
				.path(METADATA_REGISTRY_SERVICE_CLASS).path("/getDatasetMetadata")
				.queryParam("dataset", NTriplesUtil.toNTriplesString(dataset)).build(false).encode()
				.toUriString();
		HttpGet httpGet = new HttpGet(requestURL);
		DatasetMetadata datasetMetadata = execute(httpGet,
				TypeFactory.defaultInstance().constructType(DatasetMetadata.class));

		return datasetMetadata;
	}

	public IRI getComputedLexicalizationModel(IRI dataset) throws IOException {
		String requestURL = UriComponentsBuilder.fromHttpUrl(apiBaseURL).path(METADATA_REGISTRY_SERVICES_PATH)
				.path(METADATA_REGISTRY_SERVICE_CLASS).path("/getComputedLexicalizationModel")
				.queryParam("dataset", NTriplesUtil.toNTriplesString(dataset)).build(false).encode()
				.toUriString();
		HttpGet httpGet = new HttpGet(requestURL);
		IRI lexicalizationModel = execute(httpGet, TypeFactory.defaultInstance().constructType(IRI.class));

		return lexicalizationModel;
	}

	public Collection<CatalogRecord> getCatalogRecords() throws ClientProtocolException, IOException {
		String requestURL = UriComponentsBuilder.fromHttpUrl(apiBaseURL).path(METADATA_REGISTRY_SERVICES_PATH)
				.path(METADATA_REGISTRY_SERVICE_CLASS).path("/getCatalogRecords").build(false).encode()
				.toUriString();
		HttpGet httpGet = new HttpGet(requestURL);
		Collection<CatalogRecord> catalogRecords = execute(httpGet,
				TypeFactory.defaultInstance().constructCollectionType(Collection.class, CatalogRecord.class));

		return catalogRecords;
	}

}