package it.uniroma2.art.semanticturkey.extension.extpts.metadatarepository;

import java.util.Map;

import com.google.common.base.MoreObjects;

public class FacetAggregation {
	private Map<String, Integer> buckets;
	private int others;

	public FacetAggregation(Map<String, Integer> buckets, int others) {
		this.buckets = buckets;
		this.others = others;
	}

	public Map<String, Integer> getBuckets() {
		return buckets;
	}

	public int getOthers() {
		return others;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("buckets", buckets).add("others", others).toString();
	}
}
