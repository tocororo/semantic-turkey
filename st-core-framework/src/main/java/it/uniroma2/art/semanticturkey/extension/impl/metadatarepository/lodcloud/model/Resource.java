package it.uniroma2.art.semanticturkey.extension.impl.metadatarepository.lodcloud.model;

import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource {
	private String status;

	@JsonProperty("access_url")
	private URL accessURL;

	@JsonProperty(value = "download_url", required = false)
	private URL downloadURL;

	private String description;

	private String title;

	@JsonProperty(value = "media_type", required = false)
	private String mediaType;

	@JsonProperty(required = false)
	private List<URL> mirror;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public URL getAccessURL() {
		return accessURL;
	}

	public void setAccessURL(URL accessURL) {
		this.accessURL = accessURL;
	}

	public URL getDownloadURL() {
		return downloadURL;
	}

	public void setDownloadURL(URL downloadURL) {
		this.downloadURL = downloadURL;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
