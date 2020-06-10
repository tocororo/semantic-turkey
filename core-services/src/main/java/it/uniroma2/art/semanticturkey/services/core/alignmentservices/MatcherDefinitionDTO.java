package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MatcherDefinitionDTO {
	@JsonProperty(required = true)
	public String id;

	@JsonProperty(required = false)
	@Nullable
	public ObjectNode settings;
}
