package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasetSearchResultContainer {
	private int count;
	private String sort;
	private List<ODPDatasetSearchResult> results;
	@JsonProperty("search_facets")
	private Map<String, SearchFacet> searchFacets;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public List<ODPDatasetSearchResult> getResults() {
		return results;
	}

	public void setResults(List<ODPDatasetSearchResult> results) {
		this.results = results;
	}

	public Map<String, SearchFacet> getSearchFacets() {
		return searchFacets;
	}

	public void setSearchFacets(Map<String, SearchFacet> searchFacets) {
		this.searchFacets = searchFacets;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("count", count).add("sort", sort).add("results", results)
				.add("searchFacets", searchFacets).toString();
	}
}
