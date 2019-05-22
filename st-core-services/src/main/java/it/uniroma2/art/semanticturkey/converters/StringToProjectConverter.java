package it.uniroma2.art.semanticturkey.converters;

import org.springframework.core.convert.converter.Converter;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;

public class StringToProjectConverter implements Converter<String, Project> {

	@Override
	public Project convert(String projectName) {
		Project proj = ProjectManager.getProject(projectName);
		if (proj == null) {
			throw new IllegalArgumentException("Not an open project: " + projectName);
		}
		return proj;
	}

}
