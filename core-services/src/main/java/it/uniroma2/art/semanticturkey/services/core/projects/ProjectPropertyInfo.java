package it.uniroma2.art.semanticturkey.services.core.projects;

public class ProjectPropertyInfo {
	private final String name;
	private final String value;

	public ProjectPropertyInfo(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

}
