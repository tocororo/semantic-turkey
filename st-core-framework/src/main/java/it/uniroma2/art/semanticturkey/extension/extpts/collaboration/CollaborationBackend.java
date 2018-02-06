package it.uniroma2.art.semanticturkey.extension.extpts.collaboration;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;

/**
 * Extension point for the collaboration subsystem. An implementation of this extension point is required to
 * support a specific collaboration managemenet system (e.g. JIRA). *
 */
public interface CollaborationBackend extends Extension {

	public void checkPrjConfiguration() throws STPropertyAccessException, IOException, CollaborationBackendException ;
	
	public void createIssue(String resource, String summary, String description, String assignee, String issueId) 
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	public void assignProject(String projectName, String projectKey, String ProjectId)
			throws STPropertyAccessException, IOException, CollaborationBackendException, STPropertyUpdateException;

	public void createProject(String projectName, String projectKey)
			throws STPropertyAccessException, IOException, JsonProcessingException, CollaborationBackendException,
			STPropertyUpdateException;

	public void assignResourceToIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	void bind2project(Project project);

	public JsonNode listIssuesAssignedToResource(IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	public JsonNode listIssues()
			throws STPropertyAccessException, IOException, CollaborationBackendException;
	
	public JsonNode listUsers()
			throws STPropertyAccessException, IOException, CollaborationBackendException;
	
	public JsonNode listProjects()
			throws STPropertyAccessException, IOException, CollaborationBackendException;
	
	/**
	 * Tells if a Collaboration project is linked to the VB project
	 * @return
	 * @throws STPropertyAccessException
	 */
	public boolean isProjectLinked() throws STPropertyAccessException;
}