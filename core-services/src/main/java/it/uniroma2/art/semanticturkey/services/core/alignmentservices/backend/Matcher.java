package it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Matcher {
	@JsonProperty(required = true)
	public String id;
	@JsonProperty(required = true)
	public String description;
	public ObjectNode settings;
}
