package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectDeletionException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.generation.annotation.RequestMethod;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.SaveToStoreProject;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.resources.UpdateRoutines;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * 
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
 * 
 */
@GenerateSTServiceController
@Validated
@Component
@Controller // just for exportProject service
public class Projects extends STServiceAdapterOLD {

	protected static Logger logger = LoggerFactory.getLogger(Projects.class);

	public static class XMLNames {
		// response tags and attributes
		public final static String baseuriTag = "baseuri";
		public final static String projectTag = "project";
		public final static String propertyTag = "property";
		public final static String propNameAttr = "name";
		public final static String openAttr = "open";
		public final static String propValueAttr = "value";
		public final static String ontMgrAttr = "ontmgr";
		public final static String typeAttr = "type";
		public final static String ontoTypeAttr = "ontoType";
		public final static String modelConfigAttr = "modelConfigType";
		public final static String statusAttr = "status";
		public final static String statusMsgAttr = "stMsg";
		public final static String accessibleAttr = "accessible";
	}

	@GenerateSTServiceController
	public void deleteProject(ProjectConsumer consumer, String projectName)
			throws ProjectDeletionException, IOException {
		ProjectManager.deleteProject(projectName);
		// delete the folder about project-user bindings
		ProjectUserBindingsManager.deletePUBindingsOfProject(projectName);
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
	 * @throws PUBindingException
	 * @throws RBACException 
	 */
	@GenerateSTServiceController
	public void accessProject(ProjectConsumer consumer, String projectName,
			ProjectACL.AccessLevel requestedAccessLevel, ProjectACL.LockLevel requestedLockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, PUBindingException, RBACException {

		ProjectManager.accessProject(consumer, projectName, requestedAccessLevel, requestedLockLevel);
	}

	/**
	 * see {@link ProjectManager#disconnectFromProject(ProjectConsumer, String)}
	 * 
	 * @param consumer
	 * @param projectName
	 * @throws ModelUpdateException
	 */
	@GenerateSTServiceController
	public void disconnectFromProject(ProjectConsumer consumer, String projectName)
			throws ModelUpdateException {

		ProjectManager.disconnectFromProject(consumer, projectName);
	}

	@SuppressWarnings("unchecked")
	@GenerateSTServiceController
	// @AutoRendering
	public Response listProjects(@Optional ProjectConsumer consumer,
			@Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel)
			throws ProjectAccessException {

		logger.debug("listProjects, asked by consumer: " + consumer);
		Collection<AbstractProject> projects;

		projects = ProjectManager.listProjects(consumer);
		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		Element dataElem = resp.getDataElement();

		for (AbstractProject absProj : projects) {
			Element projElem = XMLHelp.newElement(dataElem, XMLNames.projectTag, absProj.getName());
			if (absProj instanceof Project<?>) {
				Project<? extends RDFModel> proj = (Project<? extends RDFModel>) absProj;
				try {
					projElem.setAttribute(XMLNames.ontoTypeAttr,
							((Project<?>) proj).getModelType().getName());
					String ontMgr = "";
					projElem.setAttribute(XMLNames.ontMgrAttr, ontMgr);

					String mConfID = "";

					projElem.setAttribute(XMLNames.modelConfigAttr, mConfID);

					projElem.setAttribute(XMLNames.typeAttr, ((Project<?>) proj).getType());

					projElem.setAttribute(XMLNames.statusAttr, "ok");

					projElem.setAttribute(XMLNames.openAttr,
							Boolean.toString(ProjectManager.isOpen((Project<?>) absProj)));

					if (consumer != null) {
						ProjectManager.AccessResponse access = ProjectManager.checkAccessibility(consumer,
								proj, requestedAccessLevel, requestedLockLevel);

						projElem.setAttribute(XMLNames.accessibleAttr,
								Boolean.toString(access.isAffirmative()));

						if (!access.isAffirmative())
							projElem.setAttribute("accessibilityFault", access.getMsg());

					}

				} catch (DOMException e) {
					projElem.setAttribute(XMLNames.statusAttr, "error");
					projElem.setAttribute(XMLNames.statusMsgAttr,
							"problem when building XML response for this project");
				} catch (ProjectInconsistentException e) {
					projElem.setAttribute(XMLNames.statusAttr, "error");
					projElem.setAttribute(XMLNames.statusMsgAttr, e.getMessage());
				}

			} else
				// proj instanceof CorruptedProject
				projElem.setAttribute(XMLNames.statusAttr, "corrupted");
		}
		return resp;
	}

	@GenerateSTServiceController
	public void repairProject(String projectName) throws IOException, InvalidProjectNameException,
			ProjectInexistentException, ProjectInconsistentException {
		UpdateRoutines.repairProject(projectName);
	}

	/**
	 * saves state of currently loaded project <code>projectName</code>
	 * 
	 * @param project
	 * @return
	 * @throws IllegalAccessException
	 * @throws ProjectUpdateException
	 */
	@GenerateSTServiceController
	public void saveProject(String project) throws IllegalAccessException, ProjectUpdateException {
		logger.info("requested to save project: " + project);

		if (!ProjectManager.isOpen(project))
			throw new IllegalAccessException("the project has to be open first in order to be saved!");

		Project<?> projectInstance = ProjectManager.getProject(project);

		if (!(projectInstance instanceof SaveToStoreProject<?>))
			throw new IllegalAccessException("non-sense request: this is not a saveable project!");

		((SaveToStoreProject<?>) projectInstance).save();
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
	 * @throws UnavailableResourceException
	 * @throws IOException
	 * @throws DuplicatedResourceException
	 * @throws InvalidProjectNameException
	 */
	@GenerateSTServiceController
	public void cloneProject(String projectName, String newProjectName)
			throws InvalidProjectNameException, DuplicatedResourceException, IOException,
			UnavailableResourceException, ProjectInexistentException {

		logger.info("requested to export current project");

		ProjectManager.cloneProjectToNewProject(projectName, newProjectName);

	}

	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Projects/exportProject", method = org.springframework.web.bind.annotation.RequestMethod.GET)
	public void exportProject(HttpServletResponse oRes,
			@RequestParam(value = "projectName") String projectName) throws IOException, ModelAccessException,
			UnsupportedRDFFormatException, UnavailableResourceException {
		File tempServerFile = File.createTempFile("export", ".zip");
		logger.info("requested to export current project");
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
	 * @throws ModelUpdateException
	 * @throws ProjectUpdateException
	 * @throws ProjectInconsistentException
	 * @throws DuplicatedResourceException
	 * @throws ProjectCreationException
	 * @throws UnsupportedRDFFormatException
	 * @throws ModelAccessException
	 * @throws IOException
	 * @throws PUBindingException 
	 */
	@GenerateSTServiceController(method = RequestMethod.POST)
	public void importProject(MultipartFile importPackage, String newProjectName)
			throws IOException, ModelAccessException, UnsupportedRDFFormatException, ProjectCreationException,
			DuplicatedResourceException, ProjectInconsistentException, ProjectUpdateException,
			ModelUpdateException, InvalidProjectNameException, PUBindingException {

		logger.info("requested to import project from file: " + importPackage);

		File projectFile = File.createTempFile("prefix", "suffix");
		importPackage.transferTo(projectFile);
		ProjectManager.importProject(projectFile, newProjectName);
		
		STUser loggedUser = UsersManager.getLoggedUser();
		//TODO is correct to assign administrator role to the user that creates project?
		//if not how do I handle the administrator role since the role is related to a project?
		ProjectUserBindingsManager.addRoleToPUBinding(
				loggedUser.getEmail(), newProjectName, RBACManager.DefaultRole.ADMINISTRATOR);
	}

	/**
	 * this service returns values associated to properties of a given project returns a response with
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
	@GenerateSTServiceController
	public Response getProjectProperty(String projectName, String[] propertyNames)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {

		// String[] propNames = propNameList.split(";");
		String[] propValues = new String[propertyNames.length];

		for (int i = 0; i < propertyNames.length; i++)
			propValues[i] = ProjectManager.getProjectProperty(projectName, propertyNames[i]);

		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		Element dataElem = resp.getDataElement();

		for (int i = 0; i < propValues.length; i++) {
			Element projElem = XMLHelp.newElement(dataElem, XMLNames.propertyTag);
			projElem.setAttribute(XMLNames.propNameAttr, propertyNames[i]);
			projElem.setAttribute(XMLNames.propValueAttr, propValues[i]);
		}

		return resp;
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
	@GenerateSTServiceController
	public Response getProjectPropertyMap(String projectName) throws InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException, IOException {

		Map<String, String> map = ProjectManager.getProjectPropertyMap(projectName);

		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		Element dataElem = resp.getDataElement();

		Set<String> keys = map.keySet();
		for (String prop : keys) {
			Element projElem = XMLHelp.newElement(dataElem, XMLNames.propertyTag);
			projElem.setAttribute(XMLNames.propNameAttr, prop);
			projElem.setAttribute(XMLNames.propValueAttr, map.get(prop));
		}
		return resp;
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
	@GenerateSTServiceController
	public Response getProjectPropertyFileContent(String projectName) throws InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException, IOException {
		String rawFile = ProjectManager.getProjectPropertyFileContent(projectName);
		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		Element dataElem = resp.getDataElement();
		Element contentElem = XMLHelp.newElement(dataElem, "content");
		contentElem.setTextContent(rawFile);
		return resp;
	}

	@GenerateSTServiceController(method = RequestMethod.POST)
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
	@GenerateSTServiceController
	public void setProjectProperty(String projectName, String propName, String propValue)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project<?> project = ProjectManager.getProjectDescription(projectName);
		project.setProperty(propName, propValue);
	}

	// TODO
	@SuppressWarnings("unchecked")
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
	@GenerateSTServiceController
	public Response getAccessStatusMap() throws InvalidProjectNameException, ProjectInexistentException,
			ProjectAccessException, ForbiddenProjectAccessException {

		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		Element dataElem = resp.getDataElement();

		Collection<AbstractProject> projects = ProjectManager.listProjects();

		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project<?>) {
				Project<? extends RDFModel> project = (Project<? extends RDFModel>) absProj;
				Element projectElement = XMLHelp.newElement(dataElem, "project");
				projectElement.setAttribute("name", project.getName());

				Collection<AbstractProject> consumers = ProjectManager.listProjects();
				consumers.remove(project);// remove itself from its possible consumers

				ProjectACL projectAcl = ProjectManager.getProjectDescription(project.getName()).getACL();

				// status for SYSTEM
				ProjectConsumer consumer = ProjectConsumer.SYSTEM;
				Element consumerElement = XMLHelp.newElement(projectElement, "consumer");
				consumerElement.setAttribute("name", consumer.getName());
				AccessLevel aclForConsumer = projectAcl.getAccessLevelForConsumer(consumer);
				String acl = "R";
				if (aclForConsumer != null)
					acl = aclForConsumer.name();
				consumerElement.setAttribute("availableACLLevel", acl);
				AccessLevel accessedLevel = ProjectManager.getAccessedLevel(project.getName(), consumer);
				if (accessedLevel != null) {
					consumerElement.setAttribute("acquiredACLLevel", accessedLevel.name());
				}
				// ACL for other ProjectConsumer
				for (AbstractProject absCons : consumers) {
					if (absCons instanceof Project<?>) {
						Project<? extends RDFModel> cons = (Project<? extends RDFModel>) absCons;
						consumerElement = XMLHelp.newElement(projectElement, "consumer");
						consumerElement.setAttribute("name", cons.getName());
						aclForConsumer = projectAcl.getAccessLevelForConsumer(cons);
						acl = "R";
						if (aclForConsumer != null)
							acl = aclForConsumer.name();
						consumerElement.setAttribute("availableACLLevel", acl);
						accessedLevel = ProjectManager.getAccessedLevel(project.getName(), cons);
						if (accessedLevel != null) {
							consumerElement.setAttribute("acquiredACLLevel", accessedLevel.name());
						}
					}
				}
				// LOCK for the project
				Element lockElement = XMLHelp.newElement(projectElement, "lock");
				lockElement.setAttribute("availableLockLevel", projectAcl.getLockLevel().name());
				ProjectConsumer lockingConsumer = ProjectManager.getLockingConsumer(project.getName());
				if (lockingConsumer != null) { // the project could be not locked by any consumer
					lockElement.setAttribute("lockingConsumer", lockingConsumer.getName());
					lockElement.setAttribute("acquiredLockLevel",
							ProjectManager.getLockingLevel(project.getName(), lockingConsumer).name());
				}
			}
		}
		return resp;
	}

	/**
	 * Updates the access level granted by the project with the given <code>projectName</code> to the given
	 * consumer
	 * 
	 * @param projectName
	 * @param consumer
	 * @param accessLevel
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@GenerateSTServiceController
	public void updateAccessLevel(String projectName, String consumerName, AccessLevel accessLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project<RDFModel> project = ProjectManager.getProjectDescription(projectName);
		project.getACL().grantAccess(ProjectManager.getProjectDescription(consumerName), accessLevel);
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
	@GenerateSTServiceController
	public void updateLockLevel(String projectName, LockLevel lockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project<RDFModel> project = ProjectManager.getProjectDescription(projectName);
		project.getACL().setLockableWithLevel(lockLevel);
	}

}
