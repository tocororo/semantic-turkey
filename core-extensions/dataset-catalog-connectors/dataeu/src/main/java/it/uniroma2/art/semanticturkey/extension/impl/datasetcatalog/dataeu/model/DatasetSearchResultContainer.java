package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasetSearchResultContainer {
	private int count;
	private List<SearchFacet> facets;
	private List<DATAEUDatasetSearchResult> results;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<SearchFacet> getFacets() {
		return facets;
	}

	public void setFacets(List<SearchFacet> facets) {
		this.facets = facets;
	}

	public List<DATAEUDatasetSearchResult> getResults() {
		return results;
	}

	public void setResults(List<DATAEUDatasetSearchResult> results) {
		this.results = results;
	}
}
