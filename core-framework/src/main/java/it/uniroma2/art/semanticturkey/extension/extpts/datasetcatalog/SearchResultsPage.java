package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class SearchResultsPage<T> {
	private int totalResults;
	private int pageSize;
	private int page;
	private boolean tail;
	private Collection<T> content;
	private List<FacetAggregation> facetAggregations;

	@JsonCreator
	private SearchResultsPage(@JsonProperty("totalResults") int totalResults,
			@JsonProperty("pageSize") int pageSize, @JsonProperty("page") int page,
			@JsonProperty("tail") boolean tail, @JsonProperty("content") Collection<T> content,
			@JsonProperty("facetAggregations") List<FacetAggregation> facetAggregations) {
		this.totalResults = totalResults;
		this.pageSize = pageSize;
		this.page = page;
		this.tail = tail;
		this.content = content;
		this.facetAggregations = facetAggregations;
	}

	public SearchResultsPage(int totalResults, int pageSize, int page, Collection<T> content,
			List<FacetAggregation> facetAggregations) {
		this(totalResults, pageSize, page, pageSize * page >= totalResults, content, facetAggregations);
	}

	public SearchResultsPage(int pageSize, int page, boolean tail, Collection<T> content,
			List<FacetAggregation> facetAggregations) {
		this(-1, pageSize, page, tail, content, facetAggregations);
	}

	public Collection<T> getContent() {
		return content;
	}

	public int getTotalResults() {
		return totalResults;
	}

	public int getPageSize() {
		return pageSize;
	}

	public int getPage() {
		return page;
	}

	public boolean isTail() {
		return tail;
	}

	public List<FacetAggregation> getFacetAggregations() {
		return facetAggregations;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("totalResults", totalResults).add("tail", tail)
				.add("content", content).add("facetAggregations", facetAggregations).toString();
	}
}
