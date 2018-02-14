package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.extension.extpts.collaboration.CollaborationBackendException;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.extpts.CollaborationBackend;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesChecker;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services for collaboration (e.g. connection to an issue tracker).
 * 
 */
@STService
public class Collaboration extends STServiceAdapter {

	private static final String PROJ_PROP_BACKEND = "plugins.optional.collaboration.factoryID";
	private static Logger logger = LoggerFactory.getLogger(Collaboration.class);
	
	
	@STServiceOperation
	public JsonNode getCollaborationSystemStatus(String backendId) throws STPropertyAccessException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);
		Project project = getProject();
		
		STProperties settings = pluginFactory.getProjectSettings(project);
		boolean settingsConfigured = STPropertiesChecker.getModelConfigurationChecker(settings).isValid();
		
		STProperties preferences = pluginFactory.getProjectPreferences(project, UsersManager.getLoggedUser());
		boolean preferencesConfigured = STPropertiesChecker.getModelConfigurationChecker(preferences).isValid();
		
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
	
	@STServiceOperation
	public STProperties getProjectSettings(String backendId) throws STPropertyAccessException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);
		return pluginFactory.getProjectSettings(getProject());
	}

	@STServiceOperation
	public STProperties getProjectPreferences(String backendId) throws STPropertyAccessException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);

		return pluginFactory.getProjectPreferences(getProject(), UsersManager.getLoggedUser());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	public void activateCollaboratioOnProject(String backendId, Map<String, Object> projectSettings,
			Map<String, Object> currentUserPreferences) throws STPropertyAccessException,
			STPropertyUpdateException, ProjectUpdateException, ReservedPropertyUpdateException, 
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, IOException, 
			CollaborationBackendException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);
		Project project = getProject();
		pluginFactory.storeProjectSettings(project, projectSettings);
		pluginFactory.storeProjectPreferences(project, UsersManager.getLoggedUser(), currentUserPreferences);
		project.setProperty(PROJ_PROP_BACKEND, backendId);
		
		//TODO check the parameters (url sbagliato, credenziali sbagliate, parametri progetto sbagliati)
		getCollaborationBackend().checkPrjConfiguration();
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void addPreferenceiesForCurrentUser(String backendId, Map<String, Object> currentUserPreferences)
			throws STPropertyAccessException, STPropertyUpdateException, ProjectUpdateException,
			ReservedPropertyUpdateException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);
		Project project = getProject();
		pluginFactory.storeProjectPreferences(project, UsersManager.getLoggedUser(), currentUserPreferences);
		project.setProperty(PROJ_PROP_BACKEND, backendId);
	}

	private CollaborationBackend getCollaborationBackend() {
		String backendId = getProject().getProperty(PROJ_PROP_BACKEND);

		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);
		CollaborationBackend instance = (CollaborationBackend) pluginFactory
				.createInstance(pluginFactory.createDefaultPluginConfiguration());
		instance.bind2project(getProject());
		return instance;
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void createIssue(IRI resource, String summary, @Optional String description, 
			@Optional String assignee, @Optional String issueId) throws STPropertyAccessException, IOException, CollaborationBackendException {
		getCollaborationBackend().createIssue(resource.stringValue(), summary, description, assignee, issueId);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void assignProject(String projectName, String projectKey, String projectId)
			throws STPropertyAccessException, IOException, CollaborationBackendException, STPropertyUpdateException {
		getCollaborationBackend().assignProject(projectName, projectKey, projectId);
		//TODO check the parameters (url sbagliato, credenziali sbagliate, parametri progetto sbagliati)
		getCollaborationBackend().checkPrjConfiguration();
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void createProject(String projectName, String projectKey)
			throws STPropertyAccessException, JsonProcessingException, IOException, CollaborationBackendException,
			STPropertyUpdateException {
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
	public JsonNode listUsers()
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listUsers();
	}
	
	@STServiceOperation(method = RequestMethod.GET)
	public JsonNode listProjects()
			throws STPropertyAccessException, IOException, CollaborationBackendException {
		return getCollaborationBackend().listProjects();
	}

}