package it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lov.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Aggregations {
	private Map<String, Aggregation> aggregations = new LinkedHashMap<>();

	@JsonAnySetter
	public void setAggregations(String facet, Aggregation aggregation) {
		this.aggregations.put(facet, aggregation);
	}

	@JsonAnyGetter
	public Map<String, Aggregation> getAggregations() {
		return aggregations;
	}
}
