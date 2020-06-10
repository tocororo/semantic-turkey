package it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend;

import java.net.URL;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServiceMetadata {
	public static class Contact {
		@JsonProperty(required = true)
		public String name;
		@JsonProperty(required = true)
		public String email;
	}

	@JsonProperty(required = true)
	public String service;

	@JsonProperty(required = true)
	public String version;

	@JsonProperty(required = true)
	public String status;

	@JsonProperty(required = true)
	public URL[] specs;

	@Nullable
	public Contact contact;
	
	@Nullable
	public URL documentation;

	@Nullable
	public ObjectNode settings;
}
