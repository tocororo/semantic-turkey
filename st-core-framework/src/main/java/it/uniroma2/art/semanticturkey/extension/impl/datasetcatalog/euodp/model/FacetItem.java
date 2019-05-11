package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class FacetItem {
	private int count;
	@JsonProperty("display_name")
	private String displayName;
	private String name;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("count", count).add("displayName", displayName)
				.add("name", name).toString();
	}
}
