package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lodcloud;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DownloadDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lodcloud.model.LODDatasetDescription;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lodcloud.model.Resource;

/**
 * An {@link DatasetCatalogConnector} for <a href="https://lod-cloud.net/">Linked Open Data Cloud</a>.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LODCloudConnector implements DatasetCatalogConnector {

	private static final Logger logger = LoggerFactory.getLogger(LODCloudConnector.class);

	private static final String LOD_CLOUD_ENDPOINT = "https://lod-cloud.net/";
	private static final String DATASET_SEARCH_PATH = "datasets";
	private static final String DATASET_JSON_PATH = "json/{id}";
	private static final String DATASET_PAGE_PATH = "dataset";

	private static final String DATASET_SEARCH_RESULT_SCRAPING_PREFIX = "function data() { return";
	private static final String DATASET_SEARCH_RESULT_SCRAPING_SUFFIX = "var app = new Vue({";

	public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
			Map<String, List<String>> facets, int page) throws IOException {
		@SuppressWarnings("unused")
		List<Pair<String, String>> facetsQueryParams = DatasetCatalogConnector.processFacets(this, facets);

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(LOD_CLOUD_ENDPOINT)
				.path(DATASET_SEARCH_PATH);
		uriBuilder.queryParam("search", query);
		URI searchURL = uriBuilder.build().toUri();

		logger.debug("Search URL = {}", searchURL);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {
			try (CloseableHttpResponse response = httpClient.execute(new HttpGet(searchURL))) {
				StatusLine statusLine = response.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				Charset charset = ContentType.get(response.getEntity()).getCharset();
				if (charset == null) {
					charset = StandardCharsets.UTF_8;
				}

				String pageSource = IOUtils.toString(response.getEntity().getContent(), charset);
				int begin = pageSource.indexOf(DATASET_SEARCH_RESULT_SCRAPING_PREFIX);

				if (begin == -1) {
					throw new IOException(
							"Unable to find the start of the area of interest in the source HTML");
				}

				begin += DATASET_SEARCH_RESULT_SCRAPING_PREFIX.length();

				int end = pageSource.indexOf(DATASET_SEARCH_RESULT_SCRAPING_SUFFIX, begin);

				if (end == -1) {
					throw new IOException(
							"Unable to find the end of the area of interest in the source HTML");
				}

				end = pageSource.lastIndexOf("}", end); // find the closing brace of the function data()

				if (end == -1) {
					throw new IOException(
							"Unable to adjust the end of the area of interest in the source HTML");
				}

				String areaOfInterest = pageSource.substring(begin, end);

				ObjectMapper mapper = new ObjectMapper();
				JsonNode tree = mapper.readTree(areaOfInterest);

				if (!tree.isObject()) {
					throw new IOException("Scraped JSON value is not an object");
				}

				JsonNode datasetsNode = tree.get("datasets");

				if (!datasetsNode.isArray()) {
					throw new IOException(
							"The field \"datasets\" of the scraped JSON object is not an array");
				}

				List<DatasetSearchResult> results = new ArrayList<>(datasetsNode.size());

				Iterator<JsonNode> it = datasetsNode.elements();
				while (it.hasNext()) {
					JsonNode datasetObj = it.next();

					String id = datasetObj.get("_id").asText();
					String title = datasetObj.get("title").asText();

					URL datasetPage = new URL(LOD_CLOUD_ENDPOINT + DATASET_PAGE_PATH + "/" + id);

					results.add(new DatasetSearchResult(id, null, 0, datasetPage,
							Arrays.asList(SimpleValueFactory.getInstance().createLiteral(title, "en")),
							Collections.emptyList(), Collections.emptyMap()));
				}
				return new SearchResultsPage<>(results.size(), results.size(), 1, results,
						Collections.emptyList());
			}
		}

	}

	@Override
	public DatasetDescription describeDataset(String id) throws IOException {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(LOD_CLOUD_ENDPOINT)
				.path(DATASET_JSON_PATH);
		URI infoURL = uriBuilder.buildAndExpand(ImmutableMap.of("id", id)).toUri();

		logger.debug("Info URL = {}", infoURL);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {
			try (CloseableHttpResponse response = httpClient.execute(new HttpGet(infoURL))) {
				StatusLine statusLine = response.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				ObjectMapper objectMappter = new ObjectMapper();
				LODDatasetDescription lodDatasetDescription = objectMappter
						.readValue(response.getEntity().getContent(), LODDatasetDescription.class);

				ValueFactory vf = SimpleValueFactory.getInstance();

				IRI ontologyIRI = null;
				URL datasetPage = new URL(LOD_CLOUD_ENDPOINT + DATASET_PAGE_PATH + "/" + id);
				List<Literal> titles = Arrays
						.asList(vf.createLiteral(lodDatasetDescription.getTitle(), "en"));
				List<Literal> descriptions = lodDatasetDescription.getDescription().entrySet().stream()
						.map(entry -> vf.createLiteral(entry.getValue(), entry.getKey()))
						.collect(Collectors.toList());
				Map<String, List<String>> facets = new LinkedHashMap<>();
				facets.put("domain", Arrays.asList(lodDatasetDescription.getDomain()));
				facets.put("keyword", lodDatasetDescription.getKeywords());

				String uriPrefix = lodDatasetDescription.getNamespace();
				List<DownloadDescription> dataDumps = lodDatasetDescription.getFullDownload().stream()
						.filter(r -> Objects.equals(r.getStatus(), "OK")).map(r -> {
							return new DownloadDescription(r.getDownloadURL(),
									Optional.ofNullable(r.getTitle())
											.map(l -> Collections.singletonList(vf.createLiteral(l, "en")))
											.orElse(Collections.emptyList()),
									Optional.ofNullable(r.getDescription())
											.map(l -> Collections.singletonList(vf.createLiteral(l, "en")))
											.orElse(Collections.emptyList()),
									r.getMediaType());
						}).collect(toList());
				URL sparqlEndpoint = lodDatasetDescription.getSparql().stream()
						.filter(r -> Objects.equals(r.getStatus(), "OK")).map(Resource::getAccessURL)
						.findAny().orElse(null);
				IRI model = null;
				IRI lexicalizationModel = null;

				DatasetDescription datasetDescription = new DatasetDescription(id, ontologyIRI, datasetPage,
						titles, descriptions, facets, uriPrefix, dataDumps, sparqlEndpoint, model,
						lexicalizationModel);

				return datasetDescription;
			}
		}
	}

	public static void main(String[] args) throws IOException {
		LODCloudConnector connector = new LODCloudConnector();

		Map<String, List<String>> facets = new HashMap<>();
		SearchResultsPage<DatasetSearchResult> results = connector.searchDataset("agris", facets, 1);

		System.out.println(results);

		DatasetDescription datasetDecription = connector.describeDataset("agris");

		System.out.println(datasetDecription);
	}

}
