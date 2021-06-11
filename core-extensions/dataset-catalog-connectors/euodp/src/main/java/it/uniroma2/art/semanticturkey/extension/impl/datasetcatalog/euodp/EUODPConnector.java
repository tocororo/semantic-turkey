package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DownloadDescription;
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

	private static final String EUODP_SPARQL_ENDPOINT = "http://data.europa.eu/sparql";

	private static final int DEFAULT_PAGE_SIZE = 20;

	// See: http://publications.europa.eu/resource/authority/file-type
	private static final Set<String> RDF_FILE_TYPES = Sets.newHashSet(
			"http://publications.europa.eu/resource/authority/file-type/RDF",
			"http://publications.europa.eu/resource/authority/file-type/RDFA",
			"http://publications.europa.eu/resource/authority/file-type/RDF_N_QUADS",
			"http://publications.europa.eu/resource/authority/file-type/RDF_N_TRIPLES",
			"http://publications.europa.eu/resource/authority/file-type/RDF_TRIG",
			"http://publications.europa.eu/resource/authority/file-type/RDF_TURTLE",
			"http://publications.europa.eu/resource/authority/file-type/RDF_XML",
			"http://publications.europa.eu/resource/authority/file-type/JSON_LD");

	private static final IRI SPARQLQ_FILE_TYPE = SimpleValueFactory.getInstance()
			.createIRI("http://publications.europa.eu/resource/authority/file-type/SPARQLQ");
	private static final IRI DOWNLOADABLE_FILE_DISTRIBUTION_TYPE = SimpleValueFactory.getInstance().createIRI(
			"http://publications.europa.eu/resource/authority/distribution-type/DOWNLOADABLE_FILE");

	private static final String RDF_FILE_TYPE_FACET;

	// facet names have been guessed by the parameters used in the links within the web portal
	private static final LinkedHashMap<String, String> FACETS = new LinkedHashMap<>();

	private static final String FACET_FIELD_ARGUMENT;

	static {
		RDF_FILE_TYPE_FACET = "res_format:" + RDF_FILE_TYPES.stream().map(s -> "\"" + s + "\"")
				.collect(Collectors.joining(" OR ", "(", ")"));

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

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {
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
		rep.init();
		try (RepositoryConnection conn = rep.getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			IRI ontologyIRI = null;
			URL datasetPage = UriComponentsBuilder
					.fromHttpUrl("https://data.europa.eu/data/datasets/{id}")
					.buildAndExpand(ImmutableMap.of("id", id)).toUri().toURL();
			List<Literal> titles = new ArrayList<>();
			List<Literal> descriptions = new ArrayList<>();
			Map<String, List<String>> facets = Collections.emptyMap();
			List<DownloadDescription> dataDumps = new ArrayList<>();
			URL sparqlEndpoint = null;
			IRI model = null;
			IRI lexicalizationModel = null;

			// we need to skip "relative IRIs (e.g. multilingual-assets). The inclusion of a FILTER testing
			// whether object IRIs contain a colon (:) causes the server blocking the query for security
			// reasons. Consequently, I've encoded the test as an arithmetic expression.
			TupleQuery showQuery = conn.prepareTupleQuery(
			//@formatter:off
			    "prefix dcat: <http://www.w3.org/ns/dcat#>         \n" +
				"select ?s ?p ?o {                                 \n" + 
				"  ?dataset dcat:distribution? ?s .                \n" + 
				"  ?s ?p ?o .                                      \n" +
				"  BIND(isIRI(?o) as ?i)                           \n" +
				"  FILTER(-10 * (1 - ?i) + CONTAINS(STR(?o), \":\") != 0)          \n" +
				"}                                                 \n"
				//@formatter:on
			);

			IRI dataset = vf.createIRI("https://europeandataportal.eu/set/data/", id);
			showQuery.setBinding("dataset", dataset);
			Model triples = QueryResults.stream(showQuery.evaluate())
					.map(bs -> SimpleValueFactory.getInstance().createStatement((Resource) bs.getValue("s"),
							(IRI) bs.getValue("p"), bs.getValue("o")))
					.collect(() -> new LinkedHashModel(), Model::add, Model::addAll);

			titles.addAll(Models.getPropertyLiterals(triples, dataset, DCTERMS.TITLE));
			descriptions.addAll(Models.getPropertyLiterals(triples, dataset, DCTERMS.DESCRIPTION));
			for (Resource distribution : Models
					.objectResources(triples.filter(dataset, DCAT.HAS_DISTRIBUTION, null))) {
				if (triples.contains(distribution, RDF.TYPE, SPARQLQ_FILE_TYPE)) {
					String sparqlEndpointT = Models.getProperty(triples, distribution, DCAT.ACCESS_URL)
							.map(Value::stringValue).orElse(null);
					if (sparqlEndpointT != null) {
						sparqlEndpoint = new URL(sparqlEndpointT);
					}
				} else if (triples.contains(distribution, DCTERMS.TYPE,
						DOWNLOADABLE_FILE_DISTRIBUTION_TYPE)) {
					if (Models.getProperty(triples, distribution, DCTERMS.FORMAT)
							.map(f -> RDF_FILE_TYPES.contains(f.stringValue())).orElse(Boolean.FALSE)) {
						URL accessURL = new URL(Models.getProperty(triples, distribution, DCAT.ACCESS_URL)
								.get().stringValue());
						List<Literal> distrTitles = new ArrayList<>(
								Models.getPropertyLiterals(triples, distribution, DCTERMS.TITLE));
						List<Literal> distrDescriptions = new ArrayList<>(
								Models.getPropertyLiterals(triples, distribution, DCTERMS.DESCRIPTION));

						dataDumps.add(
								new DownloadDescription(accessURL, distrTitles, distrDescriptions, null));
					}
				}
			}

			return new DatasetDescription(id, ontologyIRI, datasetPage, titles, descriptions, facets, null,
					dataDumps, sparqlEndpoint, model, lexicalizationModel);
		} finally {
			rep.shutDown();
		}
	}

	public static void main(String[] args) throws IOException {
		EUODPConnector connector = new EUODPConnector();

		 Map<String, List<String>> facets = new HashMap<>();
		// // facets.put("tag", Arrays.asList("Time", "IoT"));
		// facets.put("res_format",
		// Arrays.asList("http://publications.europa.eu/resource/authority/file-type/HTML"));
		// facets.put("vocab_language",
		// Arrays.asList("http://publications.europa.eu/resource/authority/language/ENG"));
		// // facets.put("unknown", Arrays.asList("English"));
		//
		 SearchResultsPage<DatasetSearchResult> results = connector.searchDataset("vocabulary", facets, 0);

		  System.out.println(results);
		//
		// System.out.println("-----");
		//
		// System.out.println("@@ facetAggregations" + results.getFacetAggregations());
		// System.out.println("----");
		//
		DatasetDescription datasetDescription = connector.describeDataset("place");

		System.out.println("@@ dataset description = " + datasetDescription);
		//
		// System.out.println(datasetDescription);
		//
		// DatasetDescription datasetDescription2 = connector.describeDataset("place");
		//
		// System.out.println(datasetDescription2);

	}

}
