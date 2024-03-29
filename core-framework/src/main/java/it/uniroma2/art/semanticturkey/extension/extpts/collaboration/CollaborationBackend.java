package it.uniroma2.art.semanticturkey.extension.extpts.collaboration;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.extension.Extension;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;

/**
 * This extension point allows for connecting Semantic Turkey to a collaboration management platform.
 */
public interface CollaborationBackend extends Extension {

	public void checkPrjConfiguration()
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	public STProperties getCreateIssueForm();

	public void createIssue(String resource, ObjectNode issueCreationForm)
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	public void assignProject(ObjectNode projectJson) throws STPropertyAccessException, IOException,
			CollaborationBackendException, STPropertyUpdateException;

	public void createProject(ObjectNode projectJson) throws STPropertyAccessException, IOException,
			JsonProcessingException, CollaborationBackendException, STPropertyUpdateException;

	public void assignResourceToIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	public void removeResourceFromIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	void bind2project(Project project);

	public JsonNode listIssuesAssignedToResource(IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	public JsonNode listIssues(int pageOffset)
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	//public JsonNode listUsers() throws STPropertyAccessException, IOException, CollaborationBackendException;

	public JsonNode listProjects()
			throws STPropertyAccessException, IOException, CollaborationBackendException;

	/**
	 * Tells if a Collaboration project is linked to the VB project
	 * 
	 * @return
	 * @throws STPropertyAccessException
	 */
	public boolean isProjectLinked() throws STPropertyAccessException;
}