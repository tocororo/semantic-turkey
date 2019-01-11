package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lodcloud.model;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Linkset {
	@JsonProperty(value = "_id", required = false)
	private String id;
	private String target;
	private long value;

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}
}
