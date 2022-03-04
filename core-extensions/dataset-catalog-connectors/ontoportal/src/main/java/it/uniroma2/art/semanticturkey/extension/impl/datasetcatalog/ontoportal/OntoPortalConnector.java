package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DownloadDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.FacetAggregation;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacet;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchFacetProcessor;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SelectionMode;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.model.Submission;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.model.Ontology;
import it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.ontoportal.strategy.ServerCommunicationStrategy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link DatasetCatalogConnector} extension point that uses the OntoPortal REST API.
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see <a href="https://ontoportal.org/">OntoPortal Alliance</a>
 * @see <a href="http://data.bioontology.org/documentation#Ontology">Ontology</a>
 */
public class OntoPortalConnector implements DatasetCatalogConnector {

    private static final Logger logger = LoggerFactory.getLogger(OntoPortalConnector.class);

    private static final String FACET_TYPE = "type";
    private static final String FACET_CATEGORY = "category";
    private static final String FACET_GROUP = "group";
    private static final String FACET_FORMAT = "format";
    private static final String FACET_UPDATED = "updated";

    private static final String ONTOLOGY_COLLECTION_ENDPOINT = "ontologies";
    private static final String ONTOLOGY_ENDPOINT = "ontologies/{acronym}";
    private static final String DOWNLOAD_ENDPOINT = ONTOLOGY_ENDPOINT + "/download";
    private static final String LATEST_SUBMISSION_ENDPOINT = "ontologies/{acronym}/latest_submission";

    private final AbstractOntoPortalConnectorConfiguration conf;
    private final ServerCommunicationStrategy strategy;

    public OntoPortalConnector(AbstractOntoPortalConnectorConfiguration conf) {
        this.conf = conf;
        this.strategy = ServerCommunicationStrategy.getStrategy(conf);
    }

    @SearchFacet(name = "type", description = "filter by entry type", allowsMultipleValues = false, processedUsing = @SearchFacetProcessor(joinUsingDelimiter = ","))
    @SearchFacet(name = "category", description = "filter by category", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(joinUsingDelimiter = ","))
    @SearchFacet(name = "group", description = "filter by group", allowsMultipleValues = true, processedUsing = @SearchFacetProcessor(joinUsingDelimiter = ","))
    public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
                                                                Map<String, List<String>> facets, int page) throws IOException {

        /* List<Pair<String, String>> facetsQueryParams = */ DatasetCatalogConnector.processFacets(this, facets);

        URI ontologiesURL = getUriComponentsBuilder().path(ONTOLOGY_COLLECTION_ENDPOINT)
                .queryParam("include", "ontologyType,group,hasDomain,name,acronym")
                .queryParam("include_views", "true")
                .queryParam("display_links", "false")
                .queryParam("display_context", "false")
                .build().toUri();

        List<String> queryTokens = Arrays.stream(query.split("\\s+")).filter(StringUtils::isNoneEmpty).collect(Collectors.toList());

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {
            HttpGet httpRequest = new HttpGet(ontologiesURL);
            strategy.getHttpRequestHeaders().forEach(httpRequest::addHeader);
            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                StatusLine statusLine = response.getStatusLine();
                if ((statusLine.getStatusCode() / 200) != 1) {
                    throw new IOException(
                            "HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
                }

                ObjectMapper objectMapper = new ObjectMapper();
                List<Ontology> searchResultPage = objectMapper
                        .readValue(response.getEntity().getContent(), new TypeReference<List<Ontology>>() {
                        });

                List<DatasetSearchResult> pageContent = searchResultPage.stream().map(ont -> {
                    Map<String, List<String>> resultFacets = new HashMap<>();
                    extractOntologyFacets(ont, resultFacets);
                    return new DatasetSearchResult(ont.getAcronym(), null,
                            1.0, null, Collections.singletonList(Values.literal(ont.getName())),
                            Collections.emptyList(), resultFacets);
                }).filter(r -> {
                    if (queryTokens.stream().noneMatch(tk -> StringUtils.containsIgnoreCase(r.getId(), tk)
                            || r.getTitles().stream().allMatch(l -> StringUtils.containsIgnoreCase(l.getLabel(), tk)))) return false;

                    return matchesFacets(facets, r);
                }).collect(Collectors.toList());

                Map<String, MutableInt> type2count = new HashMap<>();
                Map<String, MutableInt> category2count = new HashMap<>();
                Map<String, MutableInt> group2count = new HashMap<>();

                for (DatasetSearchResult r : pageContent) {
                    r.getFacets().get(FACET_TYPE).forEach(count -> {
                        type2count.putIfAbsent(count, new MutableInt(0));
                        type2count.get(count).increment();
                    });
                    r.getFacets().get(FACET_CATEGORY).forEach(category -> {
                        category2count.putIfAbsent(category, new MutableInt(0));
                        category2count.get(category).increment();
                    });
                    r.getFacets().get(FACET_GROUP).forEach(group -> {
                        group2count.putIfAbsent(group, new MutableInt(0));
                        group2count.get(group).increment();
                    });
                }
                FacetAggregation categoryFacetAggregation = new FacetAggregation(FACET_CATEGORY,
                        "category",
                        SelectionMode.multiple,
                        category2count.entrySet().stream().map(
                                bucket ->
                                        new FacetAggregation.Bucket(
                                                bucket.getKey(),
                                                StringUtils.substringAfterLast(bucket.getKey(), "/"),
                                                bucket.getValue().getValue())
                        ).collect(Collectors.toList()), false);
                FacetAggregation groupFacetAggregation = new FacetAggregation(FACET_GROUP,
                        "group",
                        SelectionMode.multiple,
                        group2count.entrySet().stream().map(
                                bucket ->
                                        new FacetAggregation.Bucket(
                                                bucket.getKey(),
                                                StringUtils.substringAfterLast(bucket.getKey(), "/"),
                                                bucket.getValue().getValue())
                        ).collect(Collectors.toList()), false);
                FacetAggregation typeFacetAggregation = new FacetAggregation(FACET_TYPE,
                        "type",
                        SelectionMode.single,
                        type2count.entrySet().stream().map(
                                bucket ->
                                        new FacetAggregation.Bucket(
                                                bucket.getKey(),
                                                StringUtils.substringAfterLast(bucket.getKey(), bucket.getKey()),
                                                bucket.getValue().getValue())
                        ).collect(Collectors.toList()), false);
                List<FacetAggregation> facetAggregations = new ArrayList<>(2);
                facetAggregations.add(typeFacetAggregation);
                facetAggregations.add(categoryFacetAggregation);
                facetAggregations.add(groupFacetAggregation);
                return new SearchResultsPage<>(pageContent.size(),
                        pageContent.size(), 1, pageContent,
                        facetAggregations);
            }
        }

    }


    @Override
    public DatasetDescription describeDataset(String id) throws IOException {
        URI ontologyURL = getUriComponentsBuilder().path(ONTOLOGY_ENDPOINT)
                .queryParam("include", "ontologyType,group,hasDomain,name,acronym")
                .queryParam("include_views", "true")
                .queryParam("display_links", "false")
                .queryParam("display_context", "false")
                .buildAndExpand(ImmutableMap.of("acronym", id)).toUri();
        URI latestSubmissionURL = getUriComponentsBuilder().path(LATEST_SUBMISSION_ENDPOINT)
                .queryParam("include", "submissionId,hasOntologyLanguage,description,creationDate")
                .queryParam("include_views", "true")
                .queryParam("display_links", "false")
                .queryParam("display_context", "false")
                .buildAndExpand(ImmutableMap.of("acronym", id)).toUri();
        URL downloadURL = getUriComponentsBuilder().path(DOWNLOAD_ENDPOINT)
                .queryParam("apikey", conf.apiKey)
                .buildAndExpand(ImmutableMap.of("acronym", id)).toUri().toURL();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build()) {
            IRI ontologyIRI = null;
            URL datasetPage = null;
            List<Literal> titles = new ArrayList<>();
            List<Literal> descriptions = new ArrayList<>();
            Map<String, List<String>> facets = new HashMap<>();
            String uriPrefix = null;
            List<DownloadDescription> dataDumps = new ArrayList<>();
            URL sparqlEndpoint = null;
            IRI model = null;
            IRI lexicalizationModel = null;

            HttpGet httpRequest = new HttpGet(ontologyURL);
            strategy.getHttpRequestHeaders().forEach(httpRequest::addHeader);

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                StatusLine statusLine = response.getStatusLine();
                if ((statusLine.getStatusCode() / 200) != 1) {
                    throw new IOException(
                            "HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
                }

                ObjectMapper objectMapper = new ObjectMapper();
                Ontology ontologyObject = objectMapper.readValue(response.getEntity().getContent(),
                        new TypeReference<Ontology>() {});

                titles.add(Values.literal(ontologyObject.getName()));
                dataDumps.add(new DownloadDescription(downloadURL,
                        Collections.emptyList(), Collections.emptyList(), null));

                extractOntologyFacets(ontologyObject, facets);

            }

            httpRequest = new HttpGet(latestSubmissionURL);
            strategy.getHttpRequestHeaders().forEach(httpRequest::addHeader);

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                StatusLine statusLine = response.getStatusLine();
                if ((statusLine.getStatusCode() / 200) != 1) {
                    throw new IOException(
                            "HTTP Error: " + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase());
                }

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                Submission submissionObject = objectMapper.readValue(response.getEntity().getContent(),
                        new TypeReference<Submission>() {});

                if (submissionObject.getSubmissionId() != Long.MIN_VALUE) {

                    if (StringUtils.isNotBlank(submissionObject.getDescription())) {
                        descriptions.add(Values.literal(submissionObject.getDescription()));
                    }

                    model = submissionObject.getHasOntologyLanguage().getModel().orElse(null);

                    extractSubmissionFacets(submissionObject, facets);
                }

            }

            renderFacetValuesInPlace(facets);
            return new DatasetDescription(id, ontologyIRI, datasetPage, titles, descriptions, facets, uriPrefix,
                    dataDumps, sparqlEndpoint, model, lexicalizationModel);
        }
    }

    protected UriComponentsBuilder getUriComponentsBuilder() {
        return UriComponentsBuilder.fromHttpUrl(strategy.getAPIBaseURL());
    }

    private void extractOntologyFacets(Ontology ont, Map<String, List<String>> facets) {
        facets.put(FACET_TYPE, Collections.singletonList(StringUtils.isNotBlank(ont.getViewOf()) ? "view" : "ontology"));
        facets.put(FACET_CATEGORY, ObjectUtils.firstNonNull(ont.getHasDomain(), Collections.emptyList()));
        facets.put(FACET_GROUP, ObjectUtils.firstNonNull(ont.getGroup(), Collections.emptyList()));
    }

    private void extractSubmissionFacets(Submission sub, Map<String, List<String>> facets) {
        facets.put(FACET_FORMAT, Collections.singletonList(sub.getHasOntologyLanguage().toString()));
        facets.put(FACET_UPDATED, Collections.singletonList(sub.getCreationDate().toString()));
    }

    protected void renderFacetValuesInPlace(Map<String, List<String>> facets) {
        ZonedDateTime now = ZonedDateTime.now();

        CollectionUtils.transform(facets.getOrDefault(FACET_CATEGORY, Collections.emptyList()), v -> StringUtils.substringAfterLast(v, "/"));
        CollectionUtils.transform(facets.getOrDefault(FACET_GROUP, Collections.emptyList()), v -> StringUtils.substringAfterLast(v, "/"));
    }

    protected boolean matchesFacets(Map<String, List<String>> facets, DatasetSearchResult dataset) {
        if (!matchesFacet(facets, dataset, FACET_TYPE)) return false;
        if (!matchesFacet(facets, dataset, FACET_CATEGORY)) return false;
        if (!matchesFacet(facets, dataset, FACET_GROUP)) return false;

        return true;
    }

    protected boolean matchesFacet(Map<String, List<String>> facets, DatasetSearchResult dataset, String facetType) {
        return CollectionUtils.subtract(CollectionUtils.emptyIfNull(facets.get(facetType)), dataset.getFacets().get(facetType)).isEmpty();
    }

}
