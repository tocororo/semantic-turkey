package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.properties.STProperties;

public class SettingsDTO {
	@JsonProperty(required = true)
	public ObjectNode originalSchema;

	@Nullable
	public STProperties stProperties;

	@Nullable
	public String conversionException;
}