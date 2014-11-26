package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.semanticturkey.project.Project;

public class LocalResourcePosition extends ResourcePosition {
	private Project<?> project;

	public LocalResourcePosition(Project<?> project) {
		this.project = project;
	}
	

	public Project<?> getProject() {
		return project;
	}

}
