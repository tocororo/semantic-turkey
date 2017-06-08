package it.uniroma2.art.semanticturkey.project;

import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;

public class ProjectInfo {
	
	private final String name;
	private final boolean open;
	private final String model;
	private final String lexicalizationModel;
	private final String type;
	private final boolean historyEnabled;
	private final boolean validationEnabled;
	private final AccessResponse accessible;
	private final ProjectStatus status;
	
	public ProjectInfo(String name, boolean open, String model, String lexicalizationModel, String type, 
			boolean historyEnabled, boolean validationEnabled, AccessResponse accessible, ProjectStatus status) {
		this.name = name;
		this.open = open;
		this.model = model;
		this.lexicalizationModel = lexicalizationModel;
		this.type = type;
		this.historyEnabled = historyEnabled;
		this.validationEnabled = validationEnabled;
		this.accessible = accessible;
		this.status = status;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public String getModel() {
		return model;
	}
	
	public String getLexicalizationModel() {
		return lexicalizationModel;
	}
	
	public String getType() {
		return type;
	}
	
	public boolean isHistoryEnabled() {
		return historyEnabled;
	}
	
	public boolean isValidationEnabled() {
		return validationEnabled;
	}
	
	public AccessResponse getAccessible() {
		return accessible;
	}
	
	public ProjectStatus getStatus() {
		return this.status;
	}
	
}
