package it.uniroma2.art.semanticturkey.data.access;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.uniroma2.art.semanticturkey.project.Project;

public class LocalResourcePosition extends ResourcePosition {
	private Project project;

	public LocalResourcePosition(Project project) {
		this.project = project;
	}

	@Override
	public String getPosition() {
		return "local:" + project.getName();
	}

	@JsonIgnore
	public Project getProject() {
		return project;
	}

}
