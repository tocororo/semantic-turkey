package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lov.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VocabularySearchResultPage {
	@JsonProperty("total_results")
	private int total_results;
	private int page;

	@JsonProperty("page_size")
	private int pageSize;
	private String queryString;
	private Aggregations aggregations;
	private List<VocabularySearchResult> results;

	public int getTotalResults() {
		return total_results;
	}

	public void setTotalResults(int totalResults) {
		this.total_results = totalResults;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public void setAggregations(Aggregations aggregations) {
		this.aggregations = aggregations;
	}

	public Aggregations getAggregations() {
		return aggregations;
	}

	public List<VocabularySearchResult> getResults() {
		return results;
	}

	public void setResults(List<VocabularySearchResult> results) {
		this.results = results;
	}

}
