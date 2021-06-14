package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DATAEUDatasetSearchResult {
	private String id;
	private Map<String, String> title;
	private Map<String, String> description;

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

	public Map<String, String> getDescription() {
		return description;
	}

	public void setDescription(Map<String, String> description) {
		this.description = description;
	}
}
