package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class FacetAggregation {
	public static class Bucket {
		private String name;
		private String displayName;
		private int count;

		@JsonCreator
		public Bucket(@JsonProperty("name") String name,
				@JsonProperty("displayName") @Nullable String displayName, @JsonProperty("count") int count) {
			this.name = name;
			this.displayName = displayName;
			this.count = count;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("name", name).add("displayName", displayName)
					.add("count", count).toString();
		}

	}

	private String name;
	private String displayName;
	private SelectionMode selectionMode;
	private List<Bucket> buckets;
	private boolean others;

	@JsonCreator
	public FacetAggregation(@JsonProperty("name") String name,
			@JsonProperty("displayName") @Nullable String displayName,
			@JsonProperty("selectionMode") SelectionMode selectionMode,
			@JsonProperty("buckets") List<Bucket> buckets, @JsonProperty("others") boolean others) {
		this.name = name;
		this.displayName = displayName;
		this.buckets = buckets;
		this.others = others;
		this.selectionMode = selectionMode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public SelectionMode getSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(SelectionMode selectionMode) {
		this.selectionMode = selectionMode;
	}

	public List<Bucket> getBuckets() {
		return buckets;
	}

	public void setBuckets(List<Bucket> buckets) {
		this.buckets = buckets;
	}

	public boolean getOthers() {
		return others;
	}

	public void setOthers(boolean others) {
		this.others = others;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", name).add("displayName", displayName)
				.add("selectionMode", selectionMode).add("buckets", buckets).add("others", others).toString();
	}
}
