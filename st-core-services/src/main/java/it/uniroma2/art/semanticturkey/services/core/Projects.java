package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
//import it.uniroma2.art.semanticturkey.changetracking.ChangeTrackerNotDetectedException;
//import it.uniroma2.art.semanticturkey.changetracking.ChangeTrackerParameterMismatchException;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectDeletionException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedModelException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.CorruptedProject;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectInfo;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.project.ProjectStatus;
import it.uniroma2.art.semanticturkey.project.ProjectStatus.Status;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.project.RepositoryLocation;
import it.uniroma2.art.semanticturkey.project.STLocalRepositoryManager;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.resources.UpdateRoutines;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.projects.ProjectPropertyInfo;
import it.uniroma2.art.semanticturkey.services.core.projects.RepositorySummary;
import it.uniroma2.art.semanticturkey.services.core.projects.RepositorySummary.RemoteRepositorySummary;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;

@STService
public class Projects extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Projects.class);

	// TODO understand how to specify remote repository / different sail configurations
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'C')")
	public void createProject(ProjectConsumer consumer, String projectName, IRI model,
			IRI lexicalizationModel, String baseURI, boolean historyEnabled, boolean validationEnabled,
			RepositoryAccess repositoryAccess, String coreRepoID,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurer\", \"configuration\" : {\"@type\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfigurerConfiguration\"}}") PluginSpecification coreRepoSailConfigurerSpecification,
			@Optional String coreBackendType, String supportRepoID,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurer\", \"configuration\" : {\"@type\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfigurerConfiguration\"}}") PluginSpecification supportRepoSailConfigurerSpecification,
			@Optional String supportBackendType,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory\"}") PluginSpecification uriGeneratorSpecification,
			@Optional PluginSpecification renderingEngineSpecification,
			@Optional IRI creationDateProperty, @Optional IRI modificationDateProperty,
			@Optional(defaultValue = "resource") String[] updateForRoles) throws ProjectInconsistentException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, DuplicatedResourceException, ProjectCreationException,
			ClassNotFoundException, WrongPropertiesException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, ProjectBindingException, RBACException,
			UnsupportedModelException, UnsupportedLexicalizationModelException, InvalidConfigurationException,
			STPropertyAccessException {

		// If no rendering engine has been configured, guess the best one based on the model type
		if (renderingEngineSpecification == null) {
			String renderingEngineFactoryID = Project.determineBestRenderingEngine(lexicalizationModel);
			renderingEngineSpecification = new PluginSpecification(renderingEngineFactoryID, null,
					new Properties(), null);
		}

		uriGeneratorSpecification.expandDefaults();
		renderingEngineSpecification.expandDefaults();

		ProjectManager.createProject(consumer, projectName, model, lexicalizationModel, baseURI.trim(),
				historyEnabled, validationEnabled, repositoryAccess, coreRepoID,
				coreRepoSailConfigurerSpecification, coreBackendType, supportRepoID,
				supportRepoSailConfigurerSpecification, supportBackendType, uriGeneratorSpecification,
				renderingEngineSpecification, creationDateProperty, modificationDateProperty, updateForRoles);
	}

	/**
	 * 
	 * @param consumer
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @param userDependent
	 *            if true, returns only the projects accessible by the logged user (the user has a role
	 *            assigned in it)
	 * @param onlyOpen
	 *            if true, return only the open projects
	 * @return
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	// TODO @PreAuthorize
	public List<ProjectInfo> listProjects(@Optional ProjectConsumer consumer,
			@Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel,
			@Optional(defaultValue = "false") boolean userDependent,
			@Optional(defaultValue = "false") boolean onlyOpen) throws ProjectAccessException {

		logger.debug("listProjects, asked by consumer: " + consumer);

		List<ProjectInfo> listProjInfo = new ArrayList<>();

		Collection<AbstractProject> projects = ProjectManager.listProjects(consumer);

		for (AbstractProject absProj : projects) {
			String name = absProj.getName();
			String baseURI = null;
			String defaultNamespace = null;
			String model = null;
			String lexicalizationModel = null;
			boolean historyEnabled = false;
			boolean validationEnabled = false;
			boolean open = false;
			AccessResponse access = null;
			RepositoryLocation repoLocation = new RepositoryLocation(null);
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
				access = ProjectManager.checkAccessibility(consumer, proj, requestedAccessLevel,
						requestedLockLevel);
				repoLocation = proj.getDefaultRepositoryLocation();

				if (onlyOpen && !open) {
					continue;
				}
				if (userDependent && !ProjectUserBindingsManager
						.hasUserAccessToProject(UsersManager.getLoggedUser(), proj)) {
					continue;
				}
			} else { // absProj instanceof CorruptedProject
				CorruptedProject proj = (CorruptedProject) absProj;
				status = new ProjectStatus(Status.corrupted, proj.getCauseOfCorruption().getMessage());
			}
			ProjectInfo projInfo = new ProjectInfo(name, open, baseURI, defaultNamespace, model,
					lexicalizationModel, historyEnabled, validationEnabled, access, repoLocation, status);
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
					acquiredLockLevel = ProjectManager.getLockingLevel(project.getName(), lockingConsumer)
							.name();
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
	 * @param accessLevel
	 *            if not provided revoke any access level assigned from the project to the consumer
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
		Project project = ProjectManager.getProject(projectName, true);
		if (accessLevel != null) {
			project.getACL().grantAccess(ProjectManager.getProjectDescription(consumerName), accessLevel);
		} else {
			project.getACL().revokeAccess(ProjectManager.getProjectDescription(consumerName));
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'D')")
	public void deleteProject(ProjectConsumer consumer, String projectName)
			throws ProjectDeletionException, IOException {
		ProjectManager.deleteProject(projectName);
	}

	/**
	 * see
	 * {@link ProjectManager#accessProject(ProjectConsumer, String, it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel, it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel)}
	 * 
	 * @param consumer
	 * @param projectName
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @return
	 * @throws ForbiddenProjectAccessException
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws IOException
	 * @throws ProjectBindingException
	 * @throws RBACException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void accessProject(ProjectConsumer consumer, String projectName,
			ProjectACL.AccessLevel requestedAccessLevel, ProjectACL.LockLevel requestedLockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, ProjectBindingException, RBACException {

		ProjectManager.accessProject(consumer, projectName, requestedAccessLevel, requestedLockLevel);
	}

	/**
	 * see {@link ProjectManager#disconnectFromProject(ProjectConsumer, String)}
	 * 
	 * @param consumer
	 * @param projectName
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'D')")
	public void disconnectFromProject(ProjectConsumer consumer, String projectName) {

		ProjectManager.disconnectFromProject(consumer, projectName);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void repairProject(String projectName) throws IOException, InvalidProjectNameException,
			ProjectInexistentException, ProjectInconsistentException {
		UpdateRoutines.repairProject(projectName);
	}

	/*
	 * this one has being temporarily not imported from the old project service, as it requires to close and
	 * reopen a project. Not clear if we should allow a project to be deactivated/activated. Surely,
	 * considering the fact that now more clients may be accessing the project, it would be dangerous to close
	 * it and reopen it
	 * 
	 * public Response saveProjectAs(Project<?> project, String newProjectName) throws
	 * InvalidProjectNameException,
	 */

	/**
	 * saves project <code>projectName</code> to <code>newProject</code>
	 * 
	 * @param projectName
	 * @return
	 * @throws ProjectInexistentException
	 * @throws IOException
	 * @throws DuplicatedResourceException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'RC')")
	public void cloneProject(String projectName, String newProjectName) throws InvalidProjectNameException,
			DuplicatedResourceException, IOException, ProjectInexistentException, ProjectAccessException {

		logger.debug("requested to export current project");

		ProjectManager.cloneProjectToNewProject(projectName, newProjectName);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public void exportProject(HttpServletResponse oRes,
			@RequestParam(value = "projectName") String projectName)
			throws IOException, ProjectAccessException {
		File tempServerFile = File.createTempFile("export", ".zip");
		logger.debug("requested to export current project");
		ProjectManager.exportProject(projectName, tempServerFile);
		oRes.setHeader("Content-Disposition", "attachment; filename=export.zip");
		FileInputStream is = new FileInputStream(tempServerFile);
		IOUtils.copy(is, oRes.getOutputStream());
		oRes.setContentType("application/zip");
		oRes.flushBuffer();
		is.close();
	}

	/**
	 * 
	 * 
	 * @param projectName
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectUpdateException
	 * @throws ProjectInconsistentException
	 * @throws DuplicatedResourceException
	 * @throws ProjectCreationException
	 * @throws IOException
	 * @throws ProjectBindingException
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'C')")
	public void importProject(MultipartFile importPackage, String newProjectName)
			throws IOException, ProjectCreationException, DuplicatedResourceException,
			ProjectInconsistentException, ProjectUpdateException, InvalidProjectNameException,
			ProjectBindingException, ProjectInexistentException, ProjectAccessException {

		logger.debug("requested to import project from file: " + importPackage);

		File projectFile = File.createTempFile("prefix", "suffix");
		importPackage.transferTo(projectFile);
		ProjectManager.importProject(projectFile, newProjectName);
	}

	/**
	 * Updates the lock level of the project with the given <code>projectName</code>
	 * 
	 * @param projectName
	 * @param lockLevel
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void updateLockLevel(String projectName, LockLevel lockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProject(projectName, true);
		project.getACL().setLockableWithLevel(lockLevel);
	}

	/**
	 * this service returns a list name-value for all the property of a given project. Returns a response with
	 * elements called {@link #propertyTag} with attributes {@link #propNameAttr} for property name and
	 * 
	 * @param projectName
	 *            (optional)the project queried for properties
	 * @param propNameList
	 *            a ";" separated list of property names
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public Collection<ProjectPropertyInfo> getProjectPropertyMap(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {

		return ProjectManager.getProjectPropertyMap(projectName).entrySet().stream()
				.map(entry -> new ProjectPropertyInfo(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * this service returns a list name-value for all the property of a given project. Returns a response with
	 * elements called {@link #propertyTag} with attributes {@link #propNameAttr} for property name and
	 * 
	 * @param projectName
	 *            (optional)the project queried for properties
	 * @param propNameList
	 *            a ";" separated list of property names
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public String getProjectPropertyFileContent(String projectName) throws InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException, IOException {
		return ProjectManager.getProjectPropertyFileContent(projectName);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void saveProjectPropertyFileContent(String projectName, String content)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {
		ProjectManager.saveProjectPropertyFileContent(projectName, content);
	}

	/**
	 * This service sets the value of a property of the current project.
	 * 
	 * @param propName
	 * @param propValue
	 * @return
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws ReservedPropertyUpdateException
	 * @throws ProjectUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void setProjectProperty(String projectName, String propName, String propValue)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProjectDescription(projectName);
		project.setProperty(propName, propValue);
	}

	/**
	 * Returns the repositories associated with a (closed) project. Optionally, it is possible to skip local
	 * repositories.
	 * 
	 * @param projectName
	 * @param excludeLocal
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public Collection<RepositorySummary> getRepositories(String projectName,
			@Optional(defaultValue = "false") boolean excludeLocal)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {

		Collection<RepositorySummary> rv = new ArrayList<>();

		ProjectManager.handleProjectExclusively(projectName, project -> {
			STLocalRepositoryManager repoManager = new STLocalRepositoryManager(
					project.getProjectDirectory());
			repoManager.initialize();
			try {
				Collection<RepositoryInfo> repos = repoManager.getAllUserRepositoryInfos();

				for (RepositoryInfo rep : repos) {

					RepositoryConfig config = repoManager.getRepositoryConfig(rep.getId());
					RepositoryImplConfig repImplConfig = STLocalRepositoryManager
							.getUnfoldedRepositoryImplConfig(config);

					RemoteRepositorySummary remoteRepoSummary;

					if (repImplConfig instanceof HTTPRepositoryConfig) {
						HTTPRepositoryConfig httpRepConfig = ((HTTPRepositoryConfig) repImplConfig);

						java.util.Optional<STRepositoryInfo> stRepositoryInfo = repoManager
								.getSTRepositoryInfo(rep.getId());

						remoteRepoSummary = new RemoteRepositorySummary(
								Protocol.getServerLocation(httpRepConfig.getURL()),
								Protocol.getRepositoryID(httpRepConfig.getURL()),
								stRepositoryInfo.map(STRepositoryInfo::getUsername).orElse(null),
								stRepositoryInfo.map(STRepositoryInfo::getPassword).orElse(null));
					} else {
						if (excludeLocal) {
							continue; // as indicated in the parameters, skip local repositories
						}
						remoteRepoSummary = null;
					}

					RepositorySummary repSummary = new RepositorySummary(rep.getId(), rep.getDescription(),
							remoteRepoSummary);
					rv.add(repSummary);
				}
			} finally {
				repoManager.shutDown();
			}
		});

		return rv;
	}

	/**
	 * Modifies the access credentials of a repository associated with a given (closed) project. The new
	 * username and password are optional: if they are not given, they are considered <code>null</code>, thus
	 * indicating an unprotected repository.
	 * 
	 * @param projectName
	 * @param repositoryID
	 * @param newUsername
	 * @param newPassword
	 * @throws ProjectAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void modifyRepositoryAccessCredentials(String projectName, String repositoryID,
			@Optional String newUsername, @Optional String newPassword)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		ProjectManager.handleProjectExclusively(projectName, project -> {
			STLocalRepositoryManager repoManager = new STLocalRepositoryManager(
					project.getProjectDirectory());
			repoManager.initialize();
			try {
				repoManager.modifyAccessCredentials(repositoryID, newUsername, newPassword);
			} finally {
				repoManager.shutDown();
			}
		});
	}

	/**
	 * Modifies the access credentials of (possibly) many repositories at once. The repositories shall match
	 * the provided <code>serverURL</code> and <code>currentUsername</code> (only if
	 * <code>matchUsername</code> is <code>true</code>). When username matching is active, a <code>null</code>
	 * value for <code>currentUsername</code> indicates repositories with no associated username.
	 * 
	 * @param projectName
	 * @param serverURL
	 * @param matchUsername
	 * @param currentUsername
	 * @param newUsername
	 * @param newPassword
	 * @throws ProjectAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void batchModifyRepostoryAccessCredentials(String projectName, String serverURL,
			@Optional(defaultValue = "false") boolean matchUsername, @Optional String currentUsername,
			@Optional String newUsername, @Optional String newPassword)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		ProjectManager.handleProjectExclusively(projectName, project -> {
			STLocalRepositoryManager repoManager = new STLocalRepositoryManager(
					project.getProjectDirectory());
			repoManager.initialize();
			try {
				repoManager.batchModifyAccessCredentials(serverURL, matchUsername, currentUsername,
						newUsername, newPassword);
			} finally {
				repoManager.shutDown();
			}
		});

	}
}