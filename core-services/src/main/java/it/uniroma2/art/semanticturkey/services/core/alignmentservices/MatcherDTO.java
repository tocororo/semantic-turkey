package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MatcherDTO {
	@JsonProperty(required = true)
	public String id;
	@JsonProperty(required = true)
	public String description;
	public SettingsDTO settings;

}
