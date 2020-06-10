package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

import java.net.URL;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend.ServiceMetadata;

public class ServiceMetadataDTO {

	@JsonProperty(required = true)
	public String service;

	@JsonProperty(required = true)
	public String version;

	@JsonProperty(required = true)
	public String status;

	@JsonProperty(required = true)
	public URL[] specs;

	@Nullable
	public ServiceMetadata.Contact contact;

	@Nullable
	public URL documentation;

	@Nullable
	public SettingsDTO settings;
}
