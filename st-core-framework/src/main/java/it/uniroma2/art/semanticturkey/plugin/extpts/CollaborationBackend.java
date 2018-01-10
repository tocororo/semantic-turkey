package it.uniroma2.art.semanticturkey.plugin.extpts;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.plugin.Plugin;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;

/**
 * Extension point for the collaboration subsystem. An implementation of this extension point is required to
 * support a specific collaboration managemenet system (e.g. JIRA). * 
 */
public interface CollaborationBackend extends Plugin {
	
	public void createIssue(/*..*/) throws STPropertyAccessException;
	
	public void createProject(String projectName, String projectType) throws STPropertyAccessException;

	public void assignResourceToIssue(String issue, IRI resource) throws STPropertyAccessException;

	public void assignIssueToResource(String issue, IRI resource) throws STPropertyAccessException;

	void bind2project(Project project);
}