package it.uniroma2.art.semanticturkey.plugin.extpts;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.uniroma2.art.semanticturkey.exceptions.HTTPJiraException;
import it.uniroma2.art.semanticturkey.plugin.Plugin;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;

/**
 * Extension point for the collaboration subsystem. An implementation of this extension point is required to
 * support a specific collaboration managemenet system (e.g. JIRA). *
 */
public interface CollaborationBackend extends Plugin {

	public void createIssue(String resource, String summary, String description, String assignee) 
			throws STPropertyAccessException, IOException, HTTPJiraException;

	public void assignProject(String projectName, String projectKey, String ProjectId)
			throws STPropertyAccessException, IOException, HTTPJiraException, STPropertyUpdateException;

	public void createProject(String projectName, String projectKey)
			throws STPropertyAccessException, IOException, JsonProcessingException, HTTPJiraException,
			STPropertyUpdateException;

	public void assignResourceToIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, HTTPJiraException;

	void bind2project(Project project);

	public JsonNode listIssuesAssignedToResource(IRI resource)
			throws STPropertyAccessException, IOException, HTTPJiraException;

	public JsonNode listIssues()
			throws STPropertyAccessException, IOException, HTTPJiraException;
	
	public JsonNode listUsers()
			throws STPropertyAccessException, IOException, HTTPJiraException;
	
	public JsonNode listProjects()
			throws STPropertyAccessException, IOException, HTTPJiraException;
}