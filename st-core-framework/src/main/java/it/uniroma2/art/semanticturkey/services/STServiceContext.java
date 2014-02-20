package it.uniroma2.art.semanticturkey.services;

import it.uniroma2.art.semanticturkey.project.Project;

public interface STServiceContext {
	Project<?> getProject();
	Project<?> getProject(int index);
}
