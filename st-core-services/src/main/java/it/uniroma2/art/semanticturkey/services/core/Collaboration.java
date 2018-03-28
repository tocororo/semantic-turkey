package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
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
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
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
	public JsonNode getCollaborationSystemStatus(String backendId)
			throws STPropertyAccessException, NoSuchSettingsManager {

		STUser user = UsersManager.getLoggedUser();
		Project project = getProject();

		STProperties settings = exptManager.getSettings(project, user, backendId, Scope.PROJECT);
		boolean settingsConfigured = STPropertiesChecker.getModelConfigurationChecker(settings).isValid();

		STProperties preferences = exptManager.getSettings(project, user, backendId, Scope.PROJECT_USER);
		boolean preferencesConfigured = STPropertiesChecker.getModelConfigurationChecker(preferences)
				.isValid();

		boolean collaborationEnabled = project.getProperty(PROJ_PROP_BACKEND) != null;

		JsonNodeFactory jf = JsonNodeFactory.instance;
		ObjectNode respNode = jf.objectNode();
		respNode.set("enabled", jf.booleanNode(collaborationEnabled));
		respNode.set("settingsConfigured", jf.booleanNode(settingsConfigured));
		respNode.set("preferencesConfigured", jf.booleanNode(preferencesConfigured));
		boolean projectLinked = collaborationEnabled ? getCollaborationBackend().isProjectLinked() : false;
		respNode.set("linked", jf.booleanNode(projectLinked));

		return respNode;
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void activateCollaboratioOnProject(String backendId, ObjectNode projectSettings,
			ObjectNode currentUserPreferences) throws STPropertyAccessException,
			STPropertyUpdateException, ProjectUpdateException, ReservedPropertyUpdateException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, IOException,
			CollaborationBackendException, NoSuchSettingsManager, WrongPropertiesException {
		Project project = getProject();
		STUser user = UsersManager.getLoggedUser();

		exptManager.storeSettings(backendId, project, user, Scope.PROJECT, projectSettings);
		exptManager.storeSettings(backendId, project, user, Scope.PROJECT_USER, currentUserPreferences);

		project.setProperty(PROJ_PROP_BACKEND, backendId);

		// TODO check the parameters (url sbagliato, credenziali sbagliate, parametri progetto sbagliati)
		getCollaborationBackend().checkPrjConfiguration();
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

	@STServiceOperation(method = RequestMethod.POST)
	public void createIssue(IRI resource, String summary, @Optional String description,
			@Optional String assignee, @Optional String issueId)
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		getCollaborationBackend().createIssue(resource.stringValue(), summary, description, assignee,
				issueId);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void assignProject(String projectName, String projectKey, String projectId)
			throws STPropertyAccessException, IOException, CollaborationBackendException,
			STPropertyUpdateException {
		getCollaborationBackend().assignProject(projectName, projectKey, projectId);
		// TODO check the parameters (url sbagliato, credenziali sbagliate, parametri progetto sbagliati)
		getCollaborationBackend().checkPrjConfiguration();
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void createProject(String projectName, String projectKey) throws STPropertyAccessException,
			JsonProcessingException, IOException, CollaborationBackendException, STPropertyUpdateException {
		getCollaborationBackend().createProject(projectName, projectKey);
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
	public JsonNode listIssues()
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listIssues();
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