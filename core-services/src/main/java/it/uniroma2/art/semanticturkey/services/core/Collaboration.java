package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;

import it.uniroma2.art.semanticturkey.user.*;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackend;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesChecker;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services for collaboration (e.g. connection to an issue tracker).
 * 
 */
@STService
public class Collaboration extends STServiceAdapter {

	private static final String PROJ_PROP_BACKEND = "plugins.optional.collaboration.factoryID";
	private static final String PROJ_PROP_BACKEND_ACTIVE = "plugins.optional.collaboration.active";
	private static Logger logger = LoggerFactory.getLogger(Collaboration.class);

	@Autowired
	private ExtensionPointManager exptManager;

	@STServiceOperation
	public JsonNode getCollaborationSystemStatus()
			throws STPropertyAccessException, NoSuchSettingsManager, CollaborationBackendException {

		STUser user = UsersManager.getLoggedUser();
		Project project = getProject();
		UsersGroup group = ProjectUserBindingsManager.getUserGroup(user, project);
		String csBackendId = project.getProperty(PROJ_PROP_BACKEND);
		String csActive = project.getProperty(PROJ_PROP_BACKEND_ACTIVE);
		
		boolean isCsActive = csActive==null ? false : Boolean.parseBoolean(csActive);
		boolean csProjSettingsConfigured = false; //proj settings of the CS configured
		boolean csUserSettingsConfigured = false; //user settings of the CS configured
		boolean csLinked = false; //CS project linked
		
		if (csBackendId != null) {
			STProperties projSettings = exptManager.getSettings(project, user, group, csBackendId, Scope.PROJECT);
			csProjSettingsConfigured = STPropertiesChecker.getModelConfigurationChecker(projSettings).isValid();

			STProperties userSettings = exptManager.getSettings(project, user, group, csBackendId, Scope.PROJECT_USER);
			csUserSettingsConfigured = STPropertiesChecker.getModelConfigurationChecker(userSettings).isValid();

			csLinked = getCollaborationBackend(false).isProjectLinked();
		}

		JsonNodeFactory jf = JsonNodeFactory.instance;
		ObjectNode respNode = jf.objectNode();
		respNode.set("backendId", jf.textNode(csBackendId));
		respNode.set("csActive", jf.booleanNode(isCsActive));
		respNode.set("projSettingsConfigured", jf.booleanNode(csProjSettingsConfigured));
		respNode.set("userSettingsConfigured", jf.booleanNode(csUserSettingsConfigured));
		respNode.set("linked", jf.booleanNode(csLinked));

		return respNode;
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'C')")
	public void activateCollaboratioOnProject(String backendId)
			throws ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = getProject();
		project.setProperty(PROJ_PROP_BACKEND, backendId);
		activate(project);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'U')")
	public void setCollaborationSystemActive(boolean active) throws ProjectUpdateException, ReservedPropertyUpdateException {
		if(active){
			Project project = getProject();
			activate(project);
		} else {
			Project project = getProject();
			deactivate(project);
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'U')")
	public void resetCollaborationOnProject() throws ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = getProject();
		project.removeProperty(PROJ_PROP_BACKEND, null);
		deactivate(project);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void addPreferencesForCurrentUser(String backendId, ObjectNode currentUserPreferences)
			throws STPropertyAccessException, STPropertyUpdateException, ProjectUpdateException,
			ReservedPropertyUpdateException, NoSuchSettingsManager, WrongPropertiesException {
		Project project = getProject();
		STUser user = UsersManager.getLoggedUser();

		exptManager.storeSettings(backendId, project, user, null, Scope.PROJECT_USER, currentUserPreferences);

		project.setProperty(PROJ_PROP_BACKEND, backendId);
	}

	private CollaborationBackend getCollaborationBackend(boolean checkIfActive) throws IllegalStateException, CollaborationBackendException {
		String backendId = getProject().getProperty(PROJ_PROP_BACKEND);
		boolean isCsActive = isActive(getProject());
		if(checkIfActive && !isCsActive){
			throw new CollaborationBackendException("The Collaboration System is not active for this project");
		}

		CollaborationBackend instance;
		try {
			instance = exptManager.instantiateExtension(CollaborationBackend.class,
					new PluginSpecification(backendId, null, null, null));
		} catch (IllegalArgumentException | NoSuchExtensionException | WrongPropertiesException
				| STPropertyAccessException | InvalidConfigurationException e) {
			throw new IllegalStateException(e);
		}
		instance.bind2project(getProject());
		return instance;
	}

	@STServiceOperation(method = RequestMethod.GET)
	public STProperties getIssueCreationForm() throws CollaborationBackendException {
		return getCollaborationBackend(true).getCreateIssueForm();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	public void createIssue(IRI resource, ObjectNode issueCreationForm)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		getCollaborationBackend(true).createIssue(resource.stringValue(), issueCreationForm);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'C')")
	public void assignProject(ObjectNode projectJson)
			throws STPropertyAccessException, IOException, CollaborationBackendException,
			STPropertyUpdateException {
		getCollaborationBackend(true).assignProject(projectJson);
		getCollaborationBackend(true).checkPrjConfiguration();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'C')")
	public void createProject(ObjectNode projectJson) throws STPropertyAccessException,
			JsonProcessingException, IOException, CollaborationBackendException, STPropertyUpdateException {
		getCollaborationBackend(true).createProject(projectJson);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void assignResourceToIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		getCollaborationBackend(true).assignResourceToIssue(issue, resource);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void removeResourceFromIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		getCollaborationBackend(true).removeResourceFromIssue(issue, resource);
	}

	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listIssuesAssignedToResource(IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend(true).listIssuesAssignedToResource(resource);
	}

	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listIssues(int pageOffset)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend(true).listIssues(pageOffset);
	}

	/*
	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listUsers() throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listUsers();
	}
	 */

	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listProjects()
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend(true).listProjects();
	}


	private void activate(Project project) throws ProjectUpdateException, ReservedPropertyUpdateException {
		project.setProperty(PROJ_PROP_BACKEND_ACTIVE, "true");
	}

	private void deactivate(Project project) throws ProjectUpdateException, ReservedPropertyUpdateException {
		project.setProperty(PROJ_PROP_BACKEND_ACTIVE, "false");
	}

	private boolean isActive(Project project){
		String csActive = project.getProperty(PROJ_PROP_BACKEND_ACTIVE);
		boolean isCsActive = csActive==null ? false : Boolean.parseBoolean(csActive);
		return  isCsActive;
	}

}