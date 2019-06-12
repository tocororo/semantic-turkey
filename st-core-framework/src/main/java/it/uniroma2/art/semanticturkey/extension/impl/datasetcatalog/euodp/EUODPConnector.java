package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.FacetAggregation;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacetProcessor;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SelectionMode;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model.DatasetSearchResultPage;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model.Error;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model.ODPDatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model.SearchFacet;

/**
 * An {@link DatasetCatalogConnector} for <a href="https://data.europa.eu/euodp">European Union Open Data
 * Portal</a> (EU ODP).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class EUODPConnector implements DatasetCatalogConnector {

	private static final Logger logger = LoggerFactory.getLogger(EUODPConnector.class);

	private static final String EUODP_V3_ENDPOINT = "http://data.europa.eu/euodp/data/api/3/";
	private static final String DATASET_SEARCH_PATH = "action/package_search";

	private static final String EUODP_SPARQL_ENDPOINT = "http://data.europa.eu/euodp/sparqlep";

	private static final int DEFAULT_PAGE_SIZE = 20;

	// See: http://publications.europa.eu/resource/authority/file-type
	private static final String[] RDF_FILE_TYPES = {
			"http://publications.europa.eu/resource/authority/file-type/RDF",
			"http://publications.europa.eu/resource/authority/file-type/RDFA",
			"http://publications.europa.eu/resource/authority/file-type/RDF_N_QUADS",
			"http://publications.europa.eu/resource/authority/file-type/RDF_N_TRIPLES",
			"http://publications.europa.eu/resource/authority/file-type/RDF_TRIG",
			"http://publications.europa.eu/resource/authority/file-type/RDF_TURTLE",
			"http://publications.europa.eu/resource/authority/file-type/RDF_XML",
			"http://publications.europa.eu/resource/authority/file-type/JSON_LD" };

	private static final String RDF_FILE_TYPE_FACET;
	private static final String RDF_FILE_SPARQL_COLLECTION;

	// facet names have been guessed by the parameters used in the links within the web portal
	private static final LinkedHashMap<String, String> FACETS = new LinkedHashMap<>();

	private static final String FACET_FIELD_ARGUMENT;

	static {
		RDF_FILE_TYPE_FACET = "res_format:" + Arrays.stream(RDF_FILE_TYPES).map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(" OR ", "(", ")"));
		RDF_FILE_SPARQL_COLLECTION = Arrays.stream(RDF_FILE_TYPES)
				.map(s -> RenderUtils.toSPARQL(SimpleValueFactory.getInstance().createIRI(s)))
				.collect(Collectors.joining(", ", "(", ")"));
		
		FACETS.put("vocab_theme", "theme");
		FACETS.put("groups", "group");
		FACETS.put("organization", "publisher");
		FACETS.put("vocab_concepts_eurovoc", "EuroVoc concept");
		FACETS.put("tags", "keyword");
		FACETS.put("res_format", "resource format");
		FACETS.put("vocab_geographical_coverage", "geographical coverage");
		FACETS.put("vocab_language", "language");
		
		FACET_FIELD_ARGUMENT = FACETS.keySet().stream().map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(", ", "[", "]"));
	}


	private String uriPrefix;

	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "vocab_theme", description = "filter by theme", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "groups", description = "filter by group", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "organization", description = "filter by publisher", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "vocab_concepts_eurovoc", description = "filter by EuroVoc concept", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "tags", description = "filter by keyword", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "res_format", description = "filter by resource format", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "vocab_geographical_coverage", description = "filter by geographical coverage", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	@it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet(name = "vocab_language", description = "filter by language", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(aggregateUsing = SolrFacetsAggregator.class))
	public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
			Map<String, List<String>> facets, int page) throws IOException {
		List<Pair<String, String>> facetsQueryParams = DatasetCatalogConnector.processFacets(this, facets);

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(EUODP_V3_ENDPOINT)
				.path(DATASET_SEARCH_PATH);
		uriBuilder.queryParam("q", query);
		uriBuilder.queryParam("start", page * DEFAULT_PAGE_SIZE);
		uriBuilder.queryParam("rows", DEFAULT_PAGE_SIZE);
		uriBuilder.queryParam("facet.field", FACET_FIELD_ARGUMENT);
		uriBuilder.queryParam("fq", RDF_FILE_TYPE_FACET + " "
				+ facetsQueryParams.stream().map(p -> p.getKey() + ":" + p.getValue()).collect(joining(" ")));

		URI searchURL = uriBuilder.build().toUri();

		logger.debug("Search URL = {}", searchURL);

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			try (CloseableHttpResponse response = httpClient.execute(new HttpGet(searchURL))) {
				StatusLine statusLine = response.getStatusLine();
				if ((statusLine.getStatusCode() / 200) != 1) {
					throw new IOException(
							"HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
				}

				ObjectMapper objectMappter = new ObjectMapper();
				DatasetSearchResultPage euodpSearchResultPage = objectMappter
						.readValue(response.getEntity().getContent(), DatasetSearchResultPage.class);

				if (!euodpSearchResultPage.isSuccess()) {
					Error error = euodpSearchResultPage.getError();

					throw new IOException(error.getType() + ":" + error.getMessage());
				}
				List<DatasetSearchResult> pageContent = new ArrayList<>(
						euodpSearchResultPage.getResult().getResults().size());

				for (ODPDatasetSearchResult result : euodpSearchResultPage.getResult().getResults()) {
					Model euodpResultModel = result.getRdf();

					IRI catalogRecord = Models
							.subjectIRI(euodpResultModel.filter(null, RDF.TYPE, DCAT.CATALOG_RECORD))
							.orElseThrow(() -> new IOException("dcat:CatalogRecord not found"));

					IRI dataset = Models.getPropertyIRI(euodpResultModel, catalogRecord, FOAF.PRIMARY_TOPIC)
							.orElseThrow(() -> new IOException("foaf:primaryTopic not found"));

					String id = dataset.getLocalName();
					IRI ontologyIRI = null;
					double score = 0;
					URL datasetPage = new URL(dataset.toString());
					List<Literal> titles = new ArrayList<>(
							Models.getPropertyLiterals(euodpResultModel, dataset, DCTERMS.TITLE));
					List<Literal> descriptions = new ArrayList<>(
							Models.getPropertyLiterals(euodpResultModel, dataset, DCTERMS.DESCRIPTION));

					Map<String, List<String>> datasetFacets = Collections.emptyMap();

					pageContent.add(new DatasetSearchResult(id, ontologyIRI, score, datasetPage, titles,
							descriptions, datasetFacets));
				}

				List<FacetAggregation> facetAggregations = new ArrayList<>();

				Map<String, SearchFacet> aggregations = euodpSearchResultPage.getResult().getSearchFacets();
				aggregations.forEach((facetName, facetObj) -> {
					facetAggregations.add(new FacetAggregation(facetName,
							FACETS.getOrDefault(facetName, facetName), SelectionMode.multiple,
							facetObj.getItems().stream()
									.map(item -> new FacetAggregation.Bucket(item.getName(),
											item.getDisplayName(), item.getCount()))
									.collect(Collectors.toList()),
							false));
				});

				return new SearchResultsPage<>(euodpSearchResultPage.getResult().getCount(),
						DEFAULT_PAGE_SIZE, page + 1, pageContent, facetAggregations);
			}
		}

	}

	public static class SolrFacetsAggregator implements Function<List<String>, String> {

		@Override
		public String apply(List<String> t) {
			if (t.size() == 1) {
				return quote(t.iterator().next());
			} else if (t.size() > 1) {
				return t.stream().map(SolrFacetsAggregator::quote).collect(joining(" OR ", "(", ")"));
			} else {
				throw new IllegalArgumentException("Empty list is not supported");
			}
		}

		private static String quote(String value) {
			return "\"" + value + "\"";
		}
	}

	@Override
	public DatasetDescription describeDataset(String id) throws IOException {
		SPARQLRepository rep = new SPARQLRepository(EUODP_SPARQL_ENDPOINT);
		rep.initialize();
		try (RepositoryConnection conn = rep.getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			IRI ontologyIRI = null;
			URL datasetPage = UriComponentsBuilder
					.fromHttpUrl("http://data.europa.eu/euodp/data/dataset/{id}")
					.buildAndExpand(ImmutableMap.of("id", id)).toUri().toURL();
			List<Literal> titles = new ArrayList<>();
			List<Literal> descriptions = new ArrayList<>();
			Map<String, List<String>> facets = Collections.emptyMap();
			URL dataDump = null;
			URL sparqlEndpoint = null;
			IRI model = null;
			IRI lexicalizationModel = null;

			TupleQuery showQuery = conn.prepareTupleQuery(
			//@formatter:off
				"PREFIX dct: <http://purl.org/dc/terms/>                                              \n" +
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>                                            \n" +
				"SELECT ?datasetPage ?title ?description ?dataDump ?sparqlEndpoint WHERE {            \n" +
				"  ?dataset a dcat:Dataset .                                                          \n" +
				"  {                                                                                  \n" +
				"    ?dataset dct:title ?title .                                                      \n" +
				"  } UNION {                                                                          \n" +
				"    ?dataset dct:title ?description .                                                \n" +
				"  } UNION {                                                                          \n" +
				"    ?dataset dcat:distribution ?distribution .                                       \n" +
				"    ?distribution dct:format ?distributionFormat .                                   \n" +
				"    FILTER(?distributionFormat IN " + RDF_FILE_SPARQL_COLLECTION+ ")                 \n" +
				"    ?distribution dct:type <http://publications.europa.eu/resource/authority/distribution-type/DOWNLOADABLE_FILE> . \n" +
				"    ?distribution dcat:accessURL ?dataDump .                                         \n" +
				"  } UNION {                                                                          \n" +
				"    ?dataset dcat:distribution ?distribution .                                       \n" +
				"    ?distribution dct:format <http://publications.europa.eu/resource/authority/file-type/SPARQLQ>  . \n" +
				"    ?distribution dcat:accessURL ?sparqlEndpoint .                                   \n" +
				"  }                                                                                  \n" +
		        "}                                                                                    \n"
				//@formatter:on
			);
			showQuery.setBinding("dataset", vf.createIRI("http://data.europa.eu/88u/dataset/", id));
			try (TupleQueryResult result = showQuery.evaluate()) {
				while (result.hasNext()) {
					BindingSet bs = result.next();

					if (bs.hasBinding("title")) {
						titles.add((Literal) bs.getValue("title"));
					} else if (bs.hasBinding("description")) {
						descriptions.add((Literal) bs.getValue("description"));
					} else if (bs.hasBinding("dataDump")) {
						dataDump = new URL(bs.getValue("dataDump").stringValue());
					} else if (bs.hasBinding("sparqlEndpoint")) {
						dataDump = new URL(bs.getValue("sparqlEndpoint").stringValue());
					}
				}
			}

			return new DatasetDescription(id, ontologyIRI, datasetPage, titles, descriptions, facets,
					uriPrefix, dataDump, sparqlEndpoint, model, lexicalizationModel);
		} finally {
			rep.shutDown();
		}
	}

	public static void main(String[] args) throws IOException {
		EUODPConnector connector = new EUODPConnector();

		Map<String, List<String>> facets = new HashMap<>();
		// facets.put("tag", Arrays.asList("Time", "IoT"));
		facets.put("res_format",
				Arrays.asList("http://publications.europa.eu/resource/authority/file-type/HTML"));
		facets.put("vocab_language",
				Arrays.asList("http://publications.europa.eu/resource/authority/language/ENG"));
		// facets.put("unknown", Arrays.asList("English"));

		SearchResultsPage<DatasetSearchResult> results = connector.searchDataset("vocabulary", facets, 0);

		// System.out.println(results);

		System.out.println("-----");

		System.out.println("@@ facetAggregations" + results.getFacetAggregations());
		System.out.println("----");

		// DatasetDescription datasetDescription = connector.describeDataset("place");
		//
		// System.out.println(datasetDescription);
		//
		// DatasetDescription datasetDescription2 = connector.describeDataset("place");
		//
		// System.out.println(datasetDescription2);

	}

}
