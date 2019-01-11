package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.MoreObjects;

public class SearchResultsPage<T> {
	private int totalResults;
	private int pageSize;
	private int page;
	private boolean tail;
	private Collection<T> content;
	private Map<String, FacetAggregation> facetAggregation;

	private SearchResultsPage(int totalResults, int pageSize, int page, boolean tail, Collection<T> content,
			Map<String, FacetAggregation> facetAggregation) {
		this.totalResults = totalResults;
		this.pageSize = pageSize;
		this.page = page;
		this.tail = tail;
		this.content = content;
		this.facetAggregation = facetAggregation;
	}

	public SearchResultsPage(int totalResults, int pageSize, int page, Collection<T> content,
			Map<String, FacetAggregation> facetAggregation) {
		this(totalResults, pageSize, page, pageSize * page >= totalResults, content, facetAggregation);
	}

	public SearchResultsPage(int pageSize, int page, boolean tail, Collection<T> content,
			Map<String, FacetAggregation> facetAggregation) {
		this(-1, pageSize, page, tail, content, facetAggregation);
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

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("totalResults", totalResults).add("tail", tail)
				.add("content", content).add("facetAggregation", facetAggregation).toString();
	}
}
