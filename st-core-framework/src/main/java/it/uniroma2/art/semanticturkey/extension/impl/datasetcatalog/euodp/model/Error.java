package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.euodp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class Error {
	private String message;
	@JsonProperty("__type")
	private String type;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("message", message).add("type", type).toString();
	}
}
