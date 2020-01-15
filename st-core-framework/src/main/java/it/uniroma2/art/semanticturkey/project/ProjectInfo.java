package it.uniroma2.art.semanticturkey.project;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;

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
	
	public ProjectInfo(String name, boolean open, String baseURI, String defaultNamespace,
			String model, String lexicalizationModel, boolean historyEnabled, boolean validationEnabled, 
			boolean shaclEnabled, AccessResponse accessible, RepositoryLocation repositoryLocation, ProjectStatus status) {
		this.name = name;
		this.open = open;
		this.baseURI = baseURI;
		this.defaultNamespace = defaultNamespace;
		this.model = model;
		this.lexicalizationModel = lexicalizationModel;
		this.historyEnabled = historyEnabled;
		this.validationEnabled = validationEnabled;
		this.shaclEnabled = shaclEnabled;
		this.accessible = accessible;
		this.repositoryLocation = repositoryLocation;
		this.status = status;
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
	
	public AccessResponse getAccessible() {
		return accessible;
	}
	
	public RepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	public ProjectStatus getStatus() {
		return this.status;
	}
	
}
