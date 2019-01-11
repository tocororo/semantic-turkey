package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.FacetAggregation;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacetProcessor;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov.model.Aggregation;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov.model.Version;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov.model.VocabularyInfo;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov.model.VocabularySearchResultPage;

/**
 * An {@link DatasetCatalogConnector} for <a href="https://lov.linkeddata.es/">LOV</a> (Linked Open
 * Vocabularies)
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class LOVConnector implements DatasetCatalogConnector {

	private static final String LOV_V2_ENDPOINT = "https://lov.linkeddata.es/dataset/lov/api/v2/";
	private static final String VOCABULARY_SEARCH_PATH = "vocabulary/search";
	private static final String VOCABULARY_INFO_PATH = "vocabulary/info";

	private static final String LOV_VOCABS_BASE = "https://lov.linkeddata.es/dataset/lov/vocabs/";

	@SearchFacet(name = "tag", description = "filter by tag", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(joinUsingDelimiter = ","))
	@SearchFacet(name = "lang", description = "filter by language", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(joinUsingDelimiter = ","))
	public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
			Map<String, List<String>> facets, int page) throws IOException {
		List<Pair<String, String>> facetsQueryParams = DatasetCatalogConnector.processFacets(this,
				facets);

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(LOV_V2_ENDPOINT)
				.path(VOCABULARY_SEARCH_PATH);
		uriBuilder.queryParam("q", query);
		uriBuilder.queryParam("page", page);
		System.out.println("facet query params = " + facetsQueryParams);
		if (facetsQueryParams != null && !facetsQueryParams.isEmpty()) {
			facetsQueryParams.forEach(nv -> uriBuilder.queryParam(nv.getKey(), nv.getValue()));
		}
		URI searchURL = uriBuilder.build().toUri();

		System.out.println("Search URL = " + searchURL);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			try (CloseableHttpResponse response = httpClient.execute(new HttpGet(searchURL))) {
				StatusLine statusLine = response.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				ObjectMapper objectMappter = new ObjectMapper();
				VocabularySearchResultPage searchResultPage = objectMappter
						.readValue(response.getEntity().getContent(), VocabularySearchResultPage.class);

				List<DatasetSearchResult> pageContent = searchResultPage.getResults().stream().map(r -> {
					try {
						SimpleValueFactory vf = SimpleValueFactory.getInstance();
						return new DatasetSearchResult(r.getSource().getPrefix(), vf.createIRI(r.getId()),
								r.getScore(), new URL(LOV_VOCABS_BASE + r.getSource().getPrefix()),
								r.getSource().getTitles().entrySet().stream()
										.map(entry -> vf.createLiteral(entry.getValue(), entry.getKey()))
										.collect(Collectors.toList()),
								r.getSource().getDescriptions().entrySet().stream()
										.map(entry -> vf.createLiteral(entry.getValue(), entry.getKey()))
										.collect(Collectors.toList()),
								ImmutableMap.of("tag", r.getSource().getTags(), "lang",
										r.getSource().getLangs()));
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				}).collect(Collectors.toList());

				LinkedHashMap<String, FacetAggregation> facet2Aggregation = new LinkedHashMap<>();

				Map<String, Aggregation> aggregations = searchResultPage.getAggregations().getAggregations();
				Aggregation langs = aggregations.get("langs");
				if (langs != null) {
					facet2Aggregation.put("lang",
							new FacetAggregation(langs.getBuckets(), langs.getSumOtherDocCount()));
				}

				Aggregation tags = aggregations.get("tags");
				if (tags != null) {
					facet2Aggregation.put("tag",
							new FacetAggregation(tags.getBuckets(), tags.getSumOtherDocCount()));
				}

				return new SearchResultsPage<>(searchResultPage.getTotalResults(),
						searchResultPage.getPageSize(), searchResultPage.getPage(), pageContent,
						facet2Aggregation);
			}
		}

	}

	@Override
	public DatasetDescription describeDataset(String id) throws IOException {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(LOV_V2_ENDPOINT)
				.path(VOCABULARY_INFO_PATH);
		uriBuilder.queryParam("vocab", id);
		URI searchURL = uriBuilder.build().toUri();

		System.out.println("Info URL = " + searchURL);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			try (CloseableHttpResponse response = httpClient.execute(new HttpGet(searchURL))) {
				StatusLine statusLine = response.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				ObjectMapper objectMappter = new ObjectMapper();
				VocabularyInfo vocabularyInfo = objectMappter.readValue(response.getEntity().getContent(),
						VocabularyInfo.class);

				ValueFactory vf = SimpleValueFactory.getInstance();

				IRI ontologyIRI = vf.createIRI(vocabularyInfo.getUri());
				URL datasetPage = new URL(LOV_VOCABS_BASE + vocabularyInfo.getPrefix());
				List<Literal> titles = vocabularyInfo.getTitles();
				List<Literal> descriptions = vocabularyInfo.getDescriptions();
				Map<String, List<String>> facets = new LinkedHashMap<>();
				String uriPrefix = vocabularyInfo.getNamespace();
				URL dataDump = null;
				URL sparqlEndpoint = null;
				IRI model = null;
				IRI lexicalizationModel = null;

				List<Version> vocabularyVersions = vocabularyInfo.getVersions();
				if (!vocabularyVersions.isEmpty()) {
					Version latestVersion = vocabularyVersions.get(vocabularyVersions.size() - 1);
					List<String> languageIds = latestVersion.getLanguageIds();
					facets.put("lang", languageIds);
					dataDump = latestVersion.getFileURL();
				} else {
					facets.put("lang", Collections.emptyList());
				}

				facets.put("tag", vocabularyInfo.getTags());

				DatasetDescription datasetDescription = new DatasetDescription(id, ontologyIRI, datasetPage,
						titles, descriptions, facets, uriPrefix, dataDump, sparqlEndpoint, model,
						lexicalizationModel);

				return datasetDescription;
			}
		}
	}

	public static void main(String[] args) throws IOException {
		LOVConnector connector = new LOVConnector();

		System.out.println(Arrays.toString(connector.getDatasetSearchFacets()));

		Map<String, List<String>> facets = new HashMap<>();
		// facets.put("tag", Arrays.asList("Time", "IoT"));
		facets.put("lang", Arrays.asList("English"));
		// facets.put("unknown", Arrays.asList("English"));

		SearchResultsPage<DatasetSearchResult> results = connector.searchDataset("people", facets, 1);

		System.out.println(results);

		DatasetDescription datasetDecription = connector.describeDataset("time");

		System.out.println(datasetDecription);
	}

}
