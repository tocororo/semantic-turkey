package it.uniroma2.art.semanticturkey.project;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.settings.project.ProjectFacets;

import java.util.Map;

public class ProjectInfo {

	private final String name;
	private final boolean open;
	private final String baseURI;
	private final String defaultNamespace;
	private final String model;
	private final String lexicalizationModel;
	private final boolean historyEnabled;
	private final boolean validationEnabled;
	private final boolean shaclEnabled;
	private final AccessResponse accessible;
	private final RepositoryLocation repositoryLocation;
	private final ProjectStatus status;
	private final Map<String, String> facets;
	private final String description;
	private final ProjectFacets facets2;

	@JsonCreator
	public ProjectInfo(@JsonProperty("name") String name, @JsonProperty("open") boolean open,
			@JsonProperty("baseURI") String baseURI,
			@JsonProperty("defaultNamespace") String defaultNamespace, @JsonProperty("model") String model,
			@JsonProperty("lexicalizationModel") String lexicalizationModel,
			@JsonProperty("historyEnabled") boolean historyEnabled,
			@JsonProperty("validationEnabled") boolean validationEnabled,
			@JsonProperty("shaclEnabled") boolean shaclEnabled,
			@JsonProperty("facets") Map<String, String> facets,
			@JsonProperty("accessible") AccessResponse accessible,
			@JsonProperty("repositoryLocation") RepositoryLocation repositoryLocation,
			@JsonProperty("status") ProjectStatus status, @JsonProperty("description") String description,
			ProjectFacets facets2) {
		this.name = name;
		this.open = open;
		this.baseURI = baseURI;
		this.defaultNamespace = defaultNamespace;
		this.model = model;
		this.lexicalizationModel = lexicalizationModel;
		this.historyEnabled = historyEnabled;
		this.validationEnabled = validationEnabled;
		this.shaclEnabled = shaclEnabled;
		this.facets = facets;
		this.accessible = accessible;
		this.repositoryLocation = repositoryLocation;
		this.status = status;
		this.description = description;
		this.facets2 = facets2;
	}

	public String getName() {
		return name;
	}

	public boolean isOpen() {
		return open;
	}

	public String getBaseURI() {
		return this.baseURI;
	}

	public String getDefaultNamespace() {
		return this.defaultNamespace;
	}

	public String getModel() {
		return model;
	}

	public String getLexicalizationModel() {
		return lexicalizationModel;
	}

	public boolean isHistoryEnabled() {
		return historyEnabled;
	}

	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	@JsonProperty("shaclEnabled")
	public boolean isSHACLEnabled() {
		return shaclEnabled;
	}

	public Map<String, String> getFacets() {
		return facets;
	}

	public AccessResponse getAccessible() {
		return accessible;
	}

	public RepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	public ProjectStatus getStatus() {
		return this.status;
	}

	public String getDescription() {
		return description;
	}

	public ProjectFacets getFacets2() {
		return facets2;
	}
}
