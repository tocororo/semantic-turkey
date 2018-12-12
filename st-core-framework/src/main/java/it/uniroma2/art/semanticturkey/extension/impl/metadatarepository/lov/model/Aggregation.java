package it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lov.model;

import java.io.IOException;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Aggregation {
	@JsonProperty("doc_count_error_upper_bound")
	private int docCountErrorUpperBound;

	@JsonProperty("sum_other_doc_count")
	private int sumOtherDocCount;

	@JsonDeserialize(using = BucketsDeserializer.class)
	private LinkedHashMap<String, Integer> buckets;

	public void setDocCountErrorUpperBound(int docCountErrorUpperBound) {
		this.docCountErrorUpperBound = docCountErrorUpperBound;
	}

	public int getDocCountErrorUpperBound() {
		return docCountErrorUpperBound;
	}

	public void setSumOtherDocCount(int sumOtherDocCount) {
		this.sumOtherDocCount = sumOtherDocCount;
	}

	public int getSumOtherDocCount() {
		return sumOtherDocCount;
	}

	public void setBuckets(LinkedHashMap<String, Integer> buckets) {
		this.buckets = buckets;
	}

	public LinkedHashMap<String, Integer> getBuckets() {
		return buckets;
	}

	public static class BucketsDeserializer extends StdDeserializer<LinkedHashMap<String, Integer>> {

		private static final long serialVersionUID = -8587762181575097706L;

		public BucketsDeserializer() {
			super(LinkedHashMap.class);
		}

		@Override
		public LinkedHashMap<String, Integer> deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			LinkedHashMap<String, Integer> aggregationBuckets = new LinkedHashMap<>();
			ArrayNode arrayNode = p.readValueAsTree();
			arrayNode.elements().forEachRemaining(n -> {
				aggregationBuckets.put(n.get("key").asText(), n.get("doc_count").asInt());
			});

			return aggregationBuckets;
		}

	}
}
