package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacetItem {
	private String id;
	@JsonDeserialize(using=LocalizedStringDeserializer.class)
	private Map<String, String> title;
	private int count;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, String> getTitle() {
		return title;
	}

	public void setTitle(Map<String, String> title) {
		this.title = title;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("id", id).add("title", title)
				.add("count", count).toString();
	}

}
