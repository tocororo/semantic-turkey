package it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lov.model;

import java.util.List;

import org.eclipse.rdf4j.model.Literal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VocabularyInfo {
	private String prefix;
	@JsonProperty("nsp")
	private String namespace;
	private String uri;
	private List<String> tags;
	@JsonDeserialize(contentUsing = JsonLDLiteralDeserializer.class)
	private List<Literal> titles;
	@JsonDeserialize(contentUsing = JsonLDLiteralDeserializer.class)
	private List<Literal> descriptions;

	private List<Version> versions;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<Literal> getTitles() {
		return titles;
	}

	public void setTitles(List<Literal> titles) {
		this.titles = titles;
	}

	public List<Literal> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<Literal> descriptions) {
		this.descriptions = descriptions;
	}

	public List<Version> getVersions() {
		return versions;
	}

	public void setVersions(List<Version> versions) {
		this.versions = versions;
	}

}
