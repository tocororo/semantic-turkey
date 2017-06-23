package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.CorruptedProject;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectInfo;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.project.ProjectStatus;
import it.uniroma2.art.semanticturkey.project.ProjectStatus.Status;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
public class Projects2 extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Projects2.class);

	// TODO understand how to specify remote repository / different sail configurations
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'C')")
	public JsonNode createProject(ProjectConsumer consumer, String projectName, IRI model,
			IRI lexicalizationModel, String baseURI, boolean historyEnabled, boolean validationEnabled,
			RepositoryAccess repositoryAccess, String coreRepoID,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.PredefinedRepositoryImplConfigurerFactory\"}") PluginSpecification coreRepoSailConfigurerSpecification,
			String supportRepoID,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.repositoryimplconfigurer.PredefinedRepositoryImplConfigurerFactory\"}") PluginSpecification supportRepoSailConfigurerSpecification,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory\"}") PluginSpecification uriGeneratorSpecification,
			@Optional PluginSpecification renderingEngineSpecification, @Optional(defaultValue="<http://purl.org/dc/terms/created>") IRI creationDateProperty,
			@Optional(defaultValue="<http://purl.org/dc/terms/modified>") IRI modificationDateProperty,
			@Optional(defaultValue = "resource") String[] updateForRoles) throws ProjectInconsistentException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, DuplicatedResourceException, ProjectCreationException,
			ClassNotFoundException, WrongPropertiesException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, PUBindingException, RBACException {

		// Expands defaults in the specification of sail configurers
		coreRepoSailConfigurerSpecification.expandDefaults();
		supportRepoSailConfigurerSpecification.expandDefaults();

		// If no rendering engine has been configured, guess the best one based on the model type
		if (renderingEngineSpecification == null) {
			String renderingEngineFactoryID = Project.determineBestRenderingEngine(lexicalizationModel);
			renderingEngineSpecification = new PluginSpecification(renderingEngineFactoryID, null,
					new Properties());
		}

		uriGeneratorSpecification.expandDefaults();
		renderingEngineSpecification.expandDefaults();

		Project proj = ProjectManager.createProject(consumer, projectName, model, lexicalizationModel,
				baseURI, historyEnabled, validationEnabled, repositoryAccess, coreRepoID,
				coreRepoSailConfigurerSpecification, supportRepoID, supportRepoSailConfigurerSpecification,
				uriGeneratorSpecification, renderingEngineSpecification, creationDateProperty,
				modificationDateProperty, updateForRoles);

		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		objectNode.set("type", JsonNodeFactory.instance.textNode(proj.getType()));

		return objectNode;
	}
	
	/**
	 * 
	 * @param consumer
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @param userDependent if true, returns only the projects accessible by the logged user 
	 * 		(the user has a role assigned in it)
	 * @param onlyOpen if true, return only the open projects
	 * @return
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	//TODO @PreAuthorize
	public List<ProjectInfo> listProjects(@Optional ProjectConsumer consumer, 
			@Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel,
			@Optional (defaultValue = "false") boolean userDependent,
			@Optional (defaultValue = "false") boolean onlyOpen) throws ProjectAccessException {
		
		logger.debug("listProjects, asked by consumer: " + consumer);
		
		List<ProjectInfo> listProjInfo = new ArrayList<>(); 
		
		Collection<AbstractProject> projects = ProjectManager.listProjects(consumer);
		
		for (AbstractProject absProj : projects) {
			String name = absProj.getName();
			String baseURI = null;
			String defaultNamespace = null;
			String model = null;
			String lexicalizationModel = null;
			String type = null;
			boolean historyEnabled = false;
			boolean validationEnabled = false;
			boolean open = false;
			AccessResponse access = null;
			ProjectStatus status = new ProjectStatus(Status.ok);
			
			if (absProj instanceof Project) {
				Project proj = (Project) absProj;
					
				baseURI = proj.getBaseURI();
				defaultNamespace = proj.getDefaultNamespace();
				model = proj.getModel().stringValue();
				lexicalizationModel = proj.getLexicalizationModel().stringValue();
				historyEnabled = proj.isHistoryEnabled();
				validationEnabled = proj.isValidationEnabled();
				open = ProjectManager.isOpen(proj);
				access = ProjectManager.checkAccessibility(consumer, proj, requestedAccessLevel, requestedLockLevel);
				try {
					type = proj.getType();
				} catch (ProjectInconsistentException e) {
					status = new ProjectStatus(Status.error, e.getMessage());
				}
				
				if (onlyOpen && !open) { continue; }
				if (userDependent && !ProjectUserBindingsManager.hasUserAccessToProject(UsersManager.getLoggedUser(), proj)) {
					continue;
				}
			} else { //absProj instanceof CorruptedProject
				CorruptedProject proj = (CorruptedProject) absProj;
				status = new ProjectStatus(Status.corrupted, proj.getCauseOfCorruption().getMessage());
			}
			ProjectInfo projInfo = new ProjectInfo(name, open, baseURI, defaultNamespace, model, lexicalizationModel,
					type, historyEnabled, validationEnabled, access, status);
			listProjInfo.add(projInfo);
		}
				
		return listProjInfo;
	}
	
	/**
	 * Returns the access statuses for every project-consumer combination. Returns a response with a set of
	 * <code>project</code> elements containing <code>consumer</code> elements and a <code>lock</code>
	 * element. Each <code>project</code> element has a single attribute: its <code>name</code>. The
	 * <code>consumer</code> elements have the following attributes:
	 * <ul>
	 * <li><code>name</code>: consumer's name</li>
	 * <li><code>availableACLLevel</code>: ACL given from the project to the consumer</li>
	 * <li><code>acquiredACLLevel</code>: The access level with which the consumer accesses the project (only
	 * specified if the project is accessed by the consumer)</li>
	 * </ul>
	 * The <code>lock</code> element has the following attributes:
	 * <ul>
	 * <li><code>availableLockLevel</code>: lock level exposed by the project</li>
	 * <li><code>lockingConsumer</code></li>: name of the consumer that locks the project. Specified only if
	 * there is a consumer locking the current project.
	 * <li><code>acquiredLockLevel</code>: lock level which with a consumer is locking the project (optional
	 * as the previous</li>
	 * </ul>
	 * 
	 * 
	 * @param projectName
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws IOException
	 * @throws ForbiddenProjectAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public JsonNode getAccessStatusMap() throws InvalidProjectNameException, ProjectInexistentException,
			ProjectAccessException, ForbiddenProjectAccessException {
		
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode responseNode = jsonFactory.arrayNode(); 

		Collection<AbstractProject> projects = ProjectManager.listProjects();

		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project) {
				Project project = (Project) absProj;
				
				ObjectNode projectNode = jsonFactory.objectNode();
				projectNode.set("name", jsonFactory.textNode(project.getName()));
				
				ArrayNode consumerArrayNode = jsonFactory.arrayNode();

				Collection<AbstractProject> consumers = ProjectManager.listProjects();
				consumers.remove(project);// remove itself from its possible consumers

				ProjectACL projectAcl = project.getACL();
				
				// status for SYSTEM
				ProjectConsumer consumer = ProjectConsumer.SYSTEM;
				JsonNode consumerAclNode = createConsumerAclNode(project, consumer);
				consumerArrayNode.add(consumerAclNode);
				// ACL for other ProjectConsumer
				for (AbstractProject absCons : consumers) {
					if (absCons instanceof Project) {
						consumer = (Project) absCons;
						consumerAclNode = createConsumerAclNode(project, consumer);
						consumerArrayNode.add(consumerAclNode);
					}
				}
				
				projectNode.set("consumers", consumerArrayNode);
				
				// LOCK for the project
				ObjectNode lockNode = jsonFactory.objectNode();
				lockNode.set("availableLockLevel", jsonFactory.textNode(projectAcl.getLockLevel().name()));
				ProjectConsumer lockingConsumer = ProjectManager.getLockingConsumer(project.getName());
				String lockingConsumerName = null;
				String acquiredLockLevel = null;
				if (lockingConsumer != null) { // the project could be not locked by any consumer
					lockingConsumerName = lockingConsumer.getName();
					acquiredLockLevel = ProjectManager.getLockingLevel(project.getName(), lockingConsumer).name();
				}
				lockNode.set("lockingConsumer", jsonFactory.textNode(lockingConsumerName));
				lockNode.set("acquiredLockLevel", jsonFactory.textNode(acquiredLockLevel));
				projectNode.set("lock", lockNode);
				
				responseNode.add(projectNode);
			}
		}
		return responseNode;
	}
	
	private JsonNode createConsumerAclNode(Project project, ProjectConsumer consumer) 
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		
		ObjectNode consumerNode = jsonFactory.objectNode();
		consumerNode.set("name", jsonFactory.textNode(consumer.getName()));
		
		ProjectACL projectAcl = project.getACL();
		
		String availableAclLevel = null;
		AccessLevel aclForConsumer = projectAcl.getAccessLevelForConsumer(consumer);
		if (aclForConsumer != null) {
			availableAclLevel = aclForConsumer.name();
		}
		consumerNode.set("availableACLLevel", jsonFactory.textNode(availableAclLevel));
		
		String acquiredAclLevel = null;
		AccessLevel accessedLevel = ProjectManager.getAccessedLevel(project.getName(), consumer);
		if (accessedLevel != null) {
			acquiredAclLevel = accessedLevel.name();
		}
		consumerNode.set("acquiredACLLevel", jsonFactory.textNode(acquiredAclLevel));
		
		return consumerNode;
	}
	
	/**
	 * 
	 * @param projectName
	 * @param consumerName
	 * @param accessLevel if not provided revoke any access level assigned from the project to the consumer
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void updateAccessLevel(String projectName, String consumerName, @Optional AccessLevel accessLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProjectDescription(projectName);
		if (accessLevel != null) {
			project.getACL().grantAccess(ProjectManager.getProjectDescription(consumerName), accessLevel);
		} else {
			project.getACL().revokeAccess(ProjectManager.getProjectDescription(consumerName));
		}
	}
	
};