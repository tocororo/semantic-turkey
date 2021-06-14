package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SelectionMode;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model.SearchFacet;
import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
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
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model.DatasetSearchResultPage;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model.DATAEUDatasetSearchResult;

/**
 * An {@link DatasetCatalogConnector} for <a href="https://data.europa.eu/euodp">European Union Open Data
 * Portal</a> (EU ODP).
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DataEUConnector implements DatasetCatalogConnector {

    private static final Logger logger = LoggerFactory.getLogger(DataEUConnector.class);
    private static final ObjectMapper om = new ObjectMapper();

    private static final String EUODP_V3_ENDPOINT = "https://data.europa.eu/api/hub/search/";
    private static final String DATASET_SEARCH_PATH = "search";

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

    public static void main(String[] args) throws IOException {
        DataEUConnector connector = new DataEUConnector();

        Map<String, List<String>> facets = new HashMap<>();
        // // facets.put("tag", Arrays.asList("Time", "IoT"));
        // facets.put("res_format",
        // Arrays.asList("http://publications.europa.eu/resource/authority/file-type/HTML"));
        // facets.put("vocab_language",
        // Arrays.asList("http://publications.europa.eu/resource/authority/language/ENG"));
        // // facets.put("unknown", Arrays.asList("English"));
        //
        SearchResultsPage<DatasetSearchResult> results = connector.searchDataset("treaty", facets, 0);

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

    public SearchResultsPage<DatasetSearchResult> searchDataset(String query, Map<String, List<String>> facets, int page) throws IOException {
        ObjectNode facetsNode = om.createObjectNode();
        ArrayNode facetArray = JsonNodeFactory.instance.arrayNode();
        facets.forEach((fn, fvs) -> {
            facetsNode.putPOJO(fn, fvs.stream().reduce(om.createArrayNode(), ArrayNode::add, ArrayNode::addAll));
        });
        facetsNode.putPOJO("format", RDF_FILE_TYPES.stream().map(s -> StringUtils.substringAfterLast(s, "/")).reduce(om.createArrayNode(), ArrayNode::add, ArrayNode::addAll));
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(EUODP_V3_ENDPOINT)
                .path(DATASET_SEARCH_PATH);
        uriBuilder.queryParam("q", query);
        uriBuilder.queryParam("filter", "dataset");
        uriBuilder.queryParam("page", page);
        uriBuilder.queryParam("limit", DEFAULT_PAGE_SIZE);
        uriBuilder.queryParam("facets", om.writeValueAsString(facetsNode));
        uriBuilder.queryParam("sort", "relevance+desc, modification_date+desc, title.en+asc");
        URI searchURL = uriBuilder.build().toUri();

        logger.debug("Search URL = {}", searchURL);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(searchURL))) {
                StatusLine statusLine = response.getStatusLine();
                if ((statusLine.getStatusCode() / 200) != 1) {
                    throw new IOException(
                            "HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
                }

                ObjectMapper objectMapper = new ObjectMapper();
                DatasetSearchResultPage euodpSearchResultPage = objectMapper
                        .readValue(response.getEntity().getContent(), DatasetSearchResultPage.class);

                if (!euodpSearchResultPage.isSuccess()) {
                    String message = euodpSearchResultPage.getMessage();

                    throw new IOException("Unsuccessful search: " + message);
                }
                List<DatasetSearchResult> pageContent = new ArrayList<>();

                for (DATAEUDatasetSearchResult result : euodpSearchResultPage.getResult().getResults()) {
                    String id = result.getId();
                    IRI ontologyIRI = null;
                    double score = 0;
                    URL datasetPage = new URL("https://data.europa.eu/data/datasets/" + id);
                    List<Literal> titles = result.getTitle().entrySet().stream().map(entry -> entry.getKey().isEmpty() ?
                            Values.literal(entry.getValue()) :
                            Values.literal(entry.getValue(), entry.getKey())).collect(Collectors.toList());
                    List<Literal> descriptions = result.getDescription().entrySet().stream().map(entry -> entry.getKey().isEmpty() ?
                            Values.literal(entry.getValue()) :
                            Values.literal(entry.getValue(), entry.getKey())).collect(Collectors.toList());

                    Map<String, List<String>> datasetFacets = Collections.emptyMap();

                    pageContent.add(new DatasetSearchResult(id, ontologyIRI, score, datasetPage, titles,
                            descriptions, datasetFacets));
                }

                List<FacetAggregation> facetAggregations = new ArrayList<>();


                List<SearchFacet> aggregations = euodpSearchResultPage.getResult().getFacets();
                // skip the format facet, which is controlled by the connector
                aggregations.stream().filter(sf -> !Objects.equals(sf.getId(), "format")).forEach(searchFacet -> {
                    facetAggregations.add(new FacetAggregation(searchFacet.getId(),
                            searchFacet.getTitle().getOrDefault("en", searchFacet.getTitle().getOrDefault("", searchFacet.getTitle().values().iterator().next())), SelectionMode.multiple,
                            searchFacet.getItems().stream()
                                    .map(item -> new FacetAggregation.Bucket(item.getId(),
                                            item.getTitle().getOrDefault("en", item.getTitle().getOrDefault("", item.getTitle().values().iterator().next())), item.getCount()))
                                    .collect(Collectors.toList()),
                            false));
                });
                return new SearchResultsPage<>(euodpSearchResultPage.getResult().getCount(),
                        DEFAULT_PAGE_SIZE, page + 1, pageContent, facetAggregations);
            }
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
            titles.sort(Comparator.comparing((Literal l) -> l.getLanguage().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(l -> l.getLabel()));
            descriptions.addAll(Models.getPropertyLiterals(triples, dataset, DCTERMS.DESCRIPTION));
            descriptions.sort(Comparator.comparing((Literal l) -> l.getLanguage().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(l -> l.getLabel()));

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

}
