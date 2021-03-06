package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;

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
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services for collaboration (e.g. connection to an issue tracker).
 * 
 */
@STService
public class Collaboration extends STServiceAdapter {

	private static final String PROJ_PROP_BACKEND = "plugins.optional.collaboration.factoryID";
	private static Logger logger = LoggerFactory.getLogger(Collaboration.class);

	@Autowired
	private ExtensionPointManager exptManager;

	@STServiceOperation
	public JsonNode getCollaborationSystemStatus()
			throws STPropertyAccessException, NoSuchSettingsManager {

		STUser user = UsersManager.getLoggedUser();
		Project project = getProject();
		
		String csBackendId = project.getProperty(PROJ_PROP_BACKEND);
		
		boolean csEnabled = csBackendId != null; //CS factoryID assigned to project
		boolean csProjSettingsConfigured = false; //proj settings of the CS configured
		boolean csUserSettingsConfigured = false; //user settings of the CS configured
		boolean csLinked = csEnabled ? getCollaborationBackend().isProjectLinked() : false; //CS project linked
		
		if (csEnabled) {
			STProperties projSettings = exptManager.getSettings(project, user, csBackendId, Scope.PROJECT);
			csProjSettingsConfigured = STPropertiesChecker.getModelConfigurationChecker(projSettings).isValid();

			STProperties userSettings = exptManager.getSettings(project, user, csBackendId, Scope.PROJECT_USER);
			csUserSettingsConfigured = STPropertiesChecker.getModelConfigurationChecker(userSettings).isValid();
		}

		JsonNodeFactory jf = JsonNodeFactory.instance;
		ObjectNode respNode = jf.objectNode();
		respNode.set("enabled", jf.booleanNode(csEnabled));
		respNode.set("backendId", jf.textNode(csBackendId));
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
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'U')")
	public void resetCollaborationOnProject() throws ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = getProject();
		project.removeProperty(PROJ_PROP_BACKEND, null);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void addPreferencesForCurrentUser(String backendId, ObjectNode currentUserPreferences)
			throws STPropertyAccessException, STPropertyUpdateException, ProjectUpdateException,
			ReservedPropertyUpdateException, NoSuchSettingsManager, WrongPropertiesException {
		Project project = getProject();
		STUser user = UsersManager.getLoggedUser();

		exptManager.storeSettings(backendId, project, user, Scope.PROJECT_USER, currentUserPreferences);

		project.setProperty(PROJ_PROP_BACKEND, backendId);
	}

	private CollaborationBackend getCollaborationBackend() throws IllegalStateException {
		String backendId = getProject().getProperty(PROJ_PROP_BACKEND);

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
	public STProperties getIssueCreationForm() {
		return getCollaborationBackend().getCreateIssueForm();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	public void createIssue(IRI resource, ObjectNode issueCreationForm)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		getCollaborationBackend().createIssue(resource.stringValue(), issueCreationForm);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'C')")
	public void assignProject(ObjectNode projectJson)
			throws STPropertyAccessException, IOException, CollaborationBackendException,
			STPropertyUpdateException {
		getCollaborationBackend().assignProject(projectJson);
		getCollaborationBackend().checkPrjConfiguration();
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, collaboration)', 'C')")
	public void createProject(ObjectNode projectJson) throws STPropertyAccessException,
			JsonProcessingException, IOException, CollaborationBackendException, STPropertyUpdateException {
		getCollaborationBackend().createProject(projectJson);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void assignResourceToIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		getCollaborationBackend().assignResourceToIssue(issue, resource);
	}

	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listIssuesAssignedToResource(IRI resource)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listIssuesAssignedToResource(resource);
	}

	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listIssues(int pageOffset)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listIssues(pageOffset);
	}

	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listUsers() throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listUsers();
	}

	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listProjects()
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listProjects();
	}

}