package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.json.JSONException;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectGroupBinding;
import it.uniroma2.art.semanticturkey.user.ProjectGroupBindingsManager;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import it.uniroma2.art.semanticturkey.user.UsersGroupException;
import it.uniroma2.art.semanticturkey.user.UsersGroupsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
public class UsersGroups extends STServiceAdapter {

	/**
	 * Returns all the groups
	 * @return
	 */
	@STServiceOperation
	public JsonNode listGroups() {
		Collection<UsersGroup> groups = UsersGroupsManager.listGroups();
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode groupsArrayNode = jsonFactory.arrayNode();
		for (UsersGroup g : groups) {
			groupsArrayNode.add(g.getAsJsonObject());
		}
		return groupsArrayNode;
	}
	
	/**
	 * Returns the description of the group identified by the id
	 * @return
	 */
	@STServiceOperation
	public JsonNode getGroup(IRI groupIri) {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		return group.getAsJsonObject();
	}
	
	/**
	 * Creates a new users group
	 * @param shortName
	 * @param fullName
	 * @param description
	 * @param webPage
	 * @param logoUrl
	 * @param iri
	 * @throws UsersGroupException
	 * @throws ProjectBindingException 
	 * @throws ProjectAccessException 
	 */
	@PreAuthorize("@auth.isAdmin()")
	@STServiceOperation(method = RequestMethod.POST)
	public ObjectNode createGroup(String shortName, @Optional String fullName, @Optional String description,
			@Optional String webPage, @Optional String logoUrl, @Optional IRI iri) throws UsersGroupException, ProjectAccessException, ProjectBindingException {
		UsersGroup group = new UsersGroup(iri, shortName);
		if (fullName != null) {
			group.setFullName(fullName);
		}
		if (description != null) {
			group.setDescription(description);
		}
		if (webPage != null) {
			group.setWebPage(webPage);
		}
		if (logoUrl != null) {
			group.setLogoUrl(logoUrl);
		}
		UsersGroupsManager.createGroup(group);
		ProjectGroupBindingsManager.createPGBindingsOfGroup(group);
		return group.getAsJsonObject();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public ObjectNode updateGroupShortName(IRI groupIri, String shortName) throws UsersGroupException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		
		if (UsersGroupsManager.getGroupByShortName(shortName) != null) {
			throw new UsersGroupException("Name " + shortName + " already used by another group");
		}
		group = UsersGroupsManager.updateShortName(group, shortName);
		return group.getAsJsonObject();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public ObjectNode updateGroupFullName(IRI groupIri, String fullName) throws UsersGroupException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		group = UsersGroupsManager.updateFullName(group, fullName);
		return group.getAsJsonObject();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public ObjectNode updateGroupDescription(IRI groupIri, String description) throws UsersGroupException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		group = UsersGroupsManager.updateDescription(group, description);
		return group.getAsJsonObject();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public ObjectNode updateGroupWebPage(IRI groupIri, String webPage) throws UsersGroupException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		group = UsersGroupsManager.updateWebPage(group, webPage);
		return group.getAsJsonObject();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public ObjectNode updateGroupLogoUrl(IRI groupIri, String logoUrl) throws UsersGroupException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		group = UsersGroupsManager.updateLogoUrl(group, logoUrl);
		return group.getAsJsonObject();
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void deleteGroup(IRI groupIri) throws Exception {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new IllegalArgumentException("Group not found");
		}
		UsersGroupsManager.deleteGroup(group);
		// delete the bindings
		ProjectGroupBindingsManager.deletePGBindingsOfGroup(group);
		//delete the group from every project-user binding that contains it
		for (ProjectUserBinding pub : ProjectUserBindingsManager.listPUBindings()) {
			UsersGroup puGroup = pub.getGroup();
			if (puGroup != null && puGroup.getIRI().equals(groupIri)) {
				ProjectUserBindingsManager.removeGroupFromPUBinding(pub.getUser(), pub.getProject());
			}
		}
	}
	
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, group)', 'U')")
	public void assignGroupToUser(String projectName, String email, IRI groupIri) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new ProjectBindingException("Group not found");
		}
		ProjectUserBindingsManager.assignGroupToPUBinding(user, project, group);
	}
	
	/**
	 * Changes the application of the limitations of a group in a user-project binding
	 * @param projectName
	 * @param email
	 * @param groupIri
	 * @param limitations
	 * @throws ProjectBindingException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, group)', 'U')")
	public void setGroupLimitationsToUser(String projectName, String email, IRI groupIri, boolean limitations) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new ProjectBindingException("Group not found");
		}
		ProjectUserBindingsManager.setGroupLimitationsToPUBinding(user, project, group, limitations);
	}
	
	/**
	 * Remove the group assigned to the user in the given project
	 * @param projectName
	 * @param email
	 * @throws ProjectBindingException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, group)', 'U')")
	public void removeGroupFromUser(String projectName, String email) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser user = UsersManager.getUserByEmail(email);
		if (user == null) {
			throw new ProjectBindingException("No user found with email " + email);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		ProjectUserBindingsManager.removeGroupFromPUBinding(user, project);
	}
	
	/**
	 * 
	 * @param projectName
	 * @param groupIri
	 * @param scheme
	 * @throws ProjectBindingException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, group)', 'U')")
	public void addOwnedSchemeToGroup(String projectName, IRI groupIri, IRI scheme) 
			throws ProjectBindingException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new ProjectBindingException("No group found with iri " + groupIri.stringValue());
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		ProjectGroupBindingsManager.addSchemeToPGBinding(group, project, scheme);
	}
	
	/**
	 * 
	 * @param projectName
	 * @param groupIri
	 * @param scheme
	 * @throws ProjectBindingException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project, group)', 'U')")
	public void removeOwnedSchemeFromGroup(String projectName, IRI groupIri, IRI scheme) 
			throws ProjectBindingException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new ProjectBindingException("No group found with iri " + groupIri.stringValue());
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		ProjectGroupBindingsManager.removeSchemeFromPGBinding(group, project, scheme);
	}
	
	/**
	 * @throws ProjectBindingException
	 * @throws JSONException 
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 * @throws InvalidProjectNameException 
	 * @throws STPropertyAccessException 
	 */
	@STServiceOperation
	public JsonNode getProjectGroupBinding(String projectName, IRI groupIri) throws ProjectBindingException, JSONException, 
		InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, STPropertyAccessException {
		UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIri);
		if (group == null) {
			throw new ProjectBindingException("No group found " + groupIri);
		}
		Project project = ProjectManager.getProjectDescription(projectName);
		if (project == null) {
			throw new ProjectBindingException("Project " + projectName + " doesn't exist");
		}
		ProjectGroupBinding pgBinding = ProjectGroupBindingsManager.getPGBinding(group, project);
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode bindingNode = jsonFactory.objectNode();
		bindingNode.set("groupIri", jsonFactory.textNode(pgBinding.getGroup().getIRI().stringValue()));
		bindingNode.set("projectName", jsonFactory.textNode(pgBinding.getProject().getName()));
		ArrayNode schemesArrayNode = jsonFactory.arrayNode();
		for (IRI scheme: pgBinding.getOwnedSchemes()) {
			schemesArrayNode.add(scheme.stringValue());
		}
		bindingNode.set("ownedSchemes", schemesArrayNode);
		return bindingNode;
	}
	
}
