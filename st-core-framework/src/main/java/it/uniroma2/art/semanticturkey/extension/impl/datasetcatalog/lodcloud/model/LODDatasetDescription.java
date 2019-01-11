package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.lodcloud.model;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LODDatasetDescription {
	@JsonProperty("_id")
	private String id;

	private URL website;

	@JsonProperty("full_download")
	private List<Resource> fullDownload;

	private String domain;

	private long triples;

	private List<Linkset> links;

	private String license;

	private String title;

	private String image;

	private List<Resource> sparql;

	private String namespace;

	private List<Resource> otherDownload;

	private List<String> keywords;

	private List<Resource> example;

	private Map<String, String> description;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public URL getWebsite() {
		return website;
	}

	public void setWebsite(URL website) {
		this.website = website;
	}

	public List<Resource> getFullDownload() {
		return fullDownload;
	}

	public void setFullDownload(List<Resource> fullDownload) {
		this.fullDownload = fullDownload;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public long getTriples() {
		return triples;
	}

	public void setTriples(long triples) {
		this.triples = triples;
	}

	public List<Linkset> getLinks() {
		return links;
	}

	public void setLinks(List<Linkset> links) {
		this.links = links;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public List<Resource> getSparql() {
		return sparql;
	}

	public void setSparql(List<Resource> sparql) {
		this.sparql = sparql;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public List<Resource> getOtherDownload() {
		return otherDownload;
	}

	public void setOtherDownload(List<Resource> otherDownload) {
		this.otherDownload = otherDownload;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}

	public List<Resource> getExample() {
		return example;
	}

	public void setExample(List<Resource> example) {
		this.example = example;
	}

	public Map<String, String> getDescription() {
		return description;
	}

	public void setDescription(Map<String, String> description) {
		this.description = description;
	}

}
