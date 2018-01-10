package it.uniroma2.art.semanticturkey.plugin.impls.collaboration;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.plugin.AbstractPlugin;
import it.uniroma2.art.semanticturkey.plugin.extpts.CollaborationBackend;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * A {@link CollaborationBackend} for the <a href="https://jira.atlassian.com/">Atlassian Jira</a>
 *
 */
public class JiraBackend extends
		AbstractPlugin<STProperties, JiraBackendSettings, STProperties, JiraBackendPreferences, JiraBackendFactory>
		implements CollaborationBackend {

	private Project project;

	public JiraBackend(JiraBackendFactory factory) {
		super(factory);
	}

	@Override
	public void bind2project(Project project) {
		this.project = project;
	}

	@Override
	public void createIssue() throws STPropertyAccessException {
		if (project == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(project);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(project,
				UsersManager.getLoggedUser());

		System.out.println(String.format(
				"@@@ [project = %s / serverURL = %s / username = %s / password = %s ] Issue created",
				project.getName(), projectSettings.serverURL, projectPreferences.username,
				projectPreferences.password));
	}

	@Override
	public void createProject(String projectName, String projectType) throws STPropertyAccessException {
		if (project == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(project);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(project,
				UsersManager.getLoggedUser());

		System.out.println(String.format(
				"@@@ [project = %s / serverURL = %s / username = %s / password = %s ] Project created: projectType = %s",
				project.getName(), projectSettings.serverURL, projectPreferences.username,
				projectPreferences.password, projectType));
	}

	@Override
	public void assignResourceToIssue(String issue, IRI resource) throws STPropertyAccessException {
		if (project == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(project);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(project,
				UsersManager.getLoggedUser());

		System.out.println(String.format(
				"@@@ [project = %s / serverURL = %s / username = %s / password = %s ] Resource %s assigned to issue %s",
				project.getName(), projectSettings.serverURL, projectPreferences.username,
				projectPreferences.password, resource.toString(), issue));
	}

	@Override
	public void assignIssueToResource(String issue, IRI resource) throws STPropertyAccessException {
		if (project == null) {
			throw new NullPointerException("Jira Backend not bound to a project");
		}
		JiraBackendSettings projectSettings = getClassLevelProjectSettings(project);
		JiraBackendPreferences projectPreferences = getClassLevelProjectPreferences(project,
				UsersManager.getLoggedUser());

		System.out.println(String.format(
				"@@@ [project = %s / serverURL = %s / username = %s / password = %s ] Issue %s assigned to resource %s",
				project.getName(), projectSettings.serverURL, projectPreferences.username,
				projectPreferences.password, issue, resource.toString()));
	}

}
