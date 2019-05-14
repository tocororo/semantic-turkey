package it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

public class FacetAggregation {
	public static class Bucket {
		private String name;
		private String displayName;
		private int count;

		public Bucket(String name, @Nullable String diplayName, int count) {
			this.name = name;
			this.displayName = diplayName;
			this.count = count;
		}

		public String getName() {
			return name;
		}

		public String getDisplayName() {
			return displayName;
		}

		public int getCount() {
			return count;
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

	public FacetAggregation(String name, @Nullable String displayName, SelectionMode selectionMode,
			List<Bucket> buckets, boolean others) {
		this.name = name;
		this.displayName = displayName;
		this.buckets = buckets;
		this.others = others;
		this.selectionMode = selectionMode;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public SelectionMode getSelectionMode() {
		return selectionMode;
	}

	public List<Bucket> getBuckets() {
		return buckets;
	}

	public boolean getOthers() {
		return others;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", name).add("displayName", displayName)
				.add("selectionMode", selectionMode).add("buckets", buckets).add("others", others).toString();
	}
}
