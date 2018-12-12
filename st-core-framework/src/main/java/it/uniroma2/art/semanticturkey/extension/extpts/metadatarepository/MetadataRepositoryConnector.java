package it.uniroma2.art.semanticturkey.extension.extpts.metadatarepository;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.annotation.AnnotationUtils;

import it.uniroma2.art.semanticturkey.extension.Extension;

/**
 * Extension point for the connection to (remote) metadata repositories.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public interface MetadataRepositoryConnector extends Extension {

	SearchResultsPage<DatasetSearchResult> searchDataset(String query, Map<String, List<String>> facets,
			int page) throws IOException;

	DatasetDescription describeDataset(String id) throws IOException;

	default SearchFacet[] getDatasetSearchFacets() {
		try {
			Method m = this.getClass().getMethod("searchDataset", String.class, Map.class, int.class);
			SearchFacetList searchFacetList = AnnotationUtils.findAnnotation(m, SearchFacetList.class);
			if (searchFacetList != null) {
				return searchFacetList.value();
			} else {
				return EMPTY_FACET_ARRAY;
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	static List<Pair<String, String>> processFacets(MetadataRepositoryConnector connector,
			Map<String, List<String>> facets) {
		SearchFacet[] searchFacetList = connector.getDatasetSearchFacets();
		Map<String, SearchFacet> name2facetDef = Arrays.stream(searchFacetList)
				.collect(Collectors.toMap(SearchFacet::name, Function.identity()));

		List<Pair<String, String>> parameters = new ArrayList<>(facets.keySet().size());

		for (Map.Entry<String, List<String>> facet : facets.entrySet()) {
			String facetName = facet.getKey();
			List<String> facetValues = facet.getValue();

			SearchFacet facetDef = name2facetDef.get(facetName);
			if (facetDef == null) {
				throw new IllegalArgumentException("Unrecognized facet: " + facetName);
			}

			String facetReducedValue;
			if (facetValues.isEmpty()) {
				continue;
			}

			if (facetValues.size() > 1) {
				if (!facetDef.allowsMultipleValues()) {
					throw new IllegalArgumentException("Multiple values not allowed for facet: " + facetName);
				}

				String delimiter = facetDef.processedUsing().joinUsingDelimiter();
				if (delimiter.isEmpty()) {
					throw new IllegalArgumentException("Unknown delimiter for facet: " + facetName);
				}

				facetReducedValue = facetValues.stream().collect(Collectors.joining(delimiter));
			} else {
				facetReducedValue = facetValues.iterator().next();
			}

			parameters.add(ImmutablePair.of(facetName, facetReducedValue));
		}

		return parameters;
	}

	static final SearchFacet[] EMPTY_FACET_ARRAY = new SearchFacet[0];
}