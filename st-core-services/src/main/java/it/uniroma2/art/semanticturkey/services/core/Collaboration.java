package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.uniroma2.art.semanticturkey.exceptions.HTTPJiraException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.extpts.CollaborationBackend;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STProperties;
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
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public STProperties getProjectSettings(String backendId) throws STPropertyAccessException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);

		return pluginFactory.getProjectSettings(getProject());
	}

	@STServiceOperation
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public STProperties getProjectPreferences(String backendId) throws STPropertyAccessException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);

		return pluginFactory.getProjectPreferences(getProject(), UsersManager.getLoggedUser());
	}

	// This is a stub implementation that just writes the project settings/preferences. Indeed, it depends on
	// the fact that the backend is stateless and that it can be recreated on-demand.
	@STServiceOperation(method = RequestMethod.POST)
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public void activateCollaboratioOnProject(String backendId, Map<String, Object> projectSettings,
			Map<String, Object> currentUserPreferences, String projectName) throws STPropertyAccessException,
			STPropertyUpdateException, ProjectUpdateException, ReservedPropertyUpdateException, 
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);
		Project project = ProjectManager.getProjectDescription(projectName);
		pluginFactory.storeProjectSettings(project, projectSettings);
		pluginFactory.storeProjectPreferences(project, UsersManager.getLoggedUser(), currentUserPreferences);
		project.setProperty(PROJ_PROP_BACKEND, backendId);
	}

	@STServiceOperation(method = RequestMethod.POST)
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public void addPreferenceiesForCurrentUser(String backendId, Map<String, Object> currentUserPreferences)
			throws STPropertyAccessException, STPropertyUpdateException, ProjectUpdateException,
			ReservedPropertyUpdateException {
		PluginFactory<?, ?, ?, ?, ?> pluginFactory = PluginManager.getPluginFactory(backendId);
		Project project = getProject();
		pluginFactory.storeProjectPreferences(project, UsersManager.getLoggedUser(), currentUserPreferences);
		project.setProperty(PROJ_PROP_BACKEND, backendId);
		// For this test, we exploit the fact
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
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public void createIssue(IRI resource, String summary) throws STPropertyAccessException, IOException, HTTPJiraException {
		getCollaborationBackend().createIssue(resource.stringValue(), summary);
	}

	@STServiceOperation(method = RequestMethod.POST)
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public void assignProject(String projectName, String projectKey, @Optional String projectId)
			throws STPropertyAccessException, IOException, HTTPJiraException, STPropertyUpdateException {
		getCollaborationBackend().assignProject(projectName, projectKey, projectId);
	}

	@STServiceOperation(method = RequestMethod.POST)
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public void createProject(String projectName, String projectKey)
			throws STPropertyAccessException, JsonProcessingException, IOException, HTTPJiraException,
			STPropertyUpdateException {
		getCollaborationBackend().createProject(projectName, projectKey);
	}

	@STServiceOperation(method = RequestMethod.POST)
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public void assignResourceToIssue(String issue, IRI resource)
			throws STPropertyAccessException, IOException, HTTPJiraException {
		getCollaborationBackend().assignResourceToIssue(issue, resource);
	}

	@STServiceOperation(method = RequestMethod.GET)
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public JsonNode listIssuesAssignedToResource(IRI resource)
			throws STPropertyAccessException, IOException, HTTPJiraException {
		return getCollaborationBackend().listIssuesAssignedToResource(resource);
	}
	
	@STServiceOperation(method = RequestMethod.GET)
	// @PreAuthorize("@auth.isAuthorized('rdf(concept, taxonomy)', 'R')")
	public JsonNode listProjects()
			throws STPropertyAccessException, IOException, HTTPJiraException {
		return getCollaborationBackend().listProjects();
	}

}