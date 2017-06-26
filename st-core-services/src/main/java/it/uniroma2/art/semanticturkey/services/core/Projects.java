package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
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
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.SaveToStoreProject;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.resources.UpdateRoutines;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
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
		public final static String modelAttr = "model";
		public final static String lexicalizationModelAttr = "lexicalizationModel";

		public final static String statusAttr = "status";
		public final static String statusMsgAttr = "stMsg";
		public final static String accessibleAttr = "accessible";
		public final static String validationEnabled = "validationEnabled";
		public final static String historyEnabled = "historyEnabled";

	}

	@GenerateSTServiceController
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
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'D')")
	public void disconnectFromProject(ProjectConsumer consumer, String projectName) {

		ProjectManager.disconnectFromProject(consumer, projectName);
	}

//	@SuppressWarnings("unchecked")
//	@GenerateSTServiceController
//	// @AutoRendering
////	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
//	public Response listProjects(@Optional ProjectConsumer consumer,
//			@Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
//			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel)
//			throws ProjectAccessException {
//
//		logger.debug("listProjects, asked by consumer: " + consumer);
//		Collection<AbstractProject> projects;
//
//		projects = ProjectManager.listProjects(consumer);
//		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
//		Element dataElem = resp.getDataElement();
//
//		for (AbstractProject absProj : projects) {
//			Element projElem = XMLHelp.newElement(dataElem, XMLNames.projectTag, absProj.getName());
//			if (absProj instanceof Project) {
//				Project proj = (Project) absProj;
//				try {
//					projElem.setAttribute(XMLNames.modelAttr,
//							((Project) proj).getModel().stringValue());
//					projElem.setAttribute(XMLNames.lexicalizationModelAttr,
//							((Project) proj).getLexicalizationModel().stringValue());
//					
//					// TODO: temporary fix to make UI work until it is updated
//					projElem.setAttribute("ontoType", ((Project) proj).computeOntoType());
//
//					
//					String ontMgr = "";
//					projElem.setAttribute(XMLNames.ontMgrAttr, ontMgr);
//
//					projElem.setAttribute(XMLNames.typeAttr, ((Project) proj).getType());
//
//					projElem.setAttribute(XMLNames.historyEnabled, Boolean.toString(((Project) proj).isHistoryEnabled()));
//					projElem.setAttribute(XMLNames.validationEnabled, Boolean.toString(((Project) proj).isValidationEnabled()));
//
//					projElem.setAttribute(XMLNames.statusAttr, "ok");
//
//					projElem.setAttribute(XMLNames.openAttr,
//							Boolean.toString(ProjectManager.isOpen((Project) absProj)));
//
//					if (consumer != null) {
//						ProjectManager.AccessResponse access = ProjectManager.checkAccessibility(consumer,
//								proj, requestedAccessLevel, requestedLockLevel);
//
//						projElem.setAttribute(XMLNames.accessibleAttr,
//								Boolean.toString(access.isAffirmative()));
//
//						if (!access.isAffirmative())
//							projElem.setAttribute("accessibilityFault", access.getMsg());
//
//					}
//
//				} catch (DOMException e) {
//					projElem.setAttribute(XMLNames.statusAttr, "error");
//					projElem.setAttribute(XMLNames.statusMsgAttr,
//							"problem when building XML response for this project");
//				} catch (ProjectInconsistentException e) {
//					projElem.setAttribute(XMLNames.statusAttr, "error");
//					projElem.setAttribute(XMLNames.statusMsgAttr, e.getMessage());
//				}
//
//			} else
//				// proj instanceof CorruptedProject
//				projElem.setAttribute(XMLNames.statusAttr, "corrupted");
//		}
//		return resp;
//	}

	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
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
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void saveProject(String project) throws IllegalAccessException, ProjectUpdateException {
		logger.info("requested to save project: " + project);

		if (!ProjectManager.isOpen(project))
			throw new IllegalAccessException("the project has to be open first in order to be saved!");

		Project projectInstance = ProjectManager.getProject(project);

		if (!(projectInstance instanceof SaveToStoreProject))
			throw new IllegalAccessException("non-sense request: this is not a saveable project!");

		((SaveToStoreProject) projectInstance).save();
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
	 * @throws ProjectAccessException 
	 */
	@GenerateSTServiceController
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'RC')")
	public void cloneProject(String projectName, String newProjectName)
			throws InvalidProjectNameException, DuplicatedResourceException, IOException,
			ProjectInexistentException, ProjectAccessException {

		logger.info("requested to export current project");

		ProjectManager.cloneProjectToNewProject(projectName, newProjectName);

	}

	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/Projects/exportProject", method = org.springframework.web.bind.annotation.RequestMethod.GET)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public void exportProject(HttpServletResponse oRes,
			@RequestParam(value = "projectName") String projectName) throws IOException, ProjectAccessException {
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
	 * @throws ProjectAccessException 
	 * @throws ProjectInexistentException 
	 */
	@GenerateSTServiceController(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'C')")
	public void importProject(MultipartFile importPackage, String newProjectName)
			throws IOException, ProjectCreationException,
			DuplicatedResourceException, ProjectInconsistentException, ProjectUpdateException, 
			InvalidProjectNameException, PUBindingException, ProjectInexistentException, 
			ProjectAccessException {

		logger.info("requested to import project from file: " + importPackage);

		File projectFile = File.createTempFile("prefix", "suffix");
		importPackage.transferTo(projectFile);
		ProjectManager.importProject(projectFile, newProjectName);
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
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
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
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
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
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
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
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void setProjectProperty(String projectName, String propName, String propValue)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProjectDescription(projectName);
		project.setProperty(propName, propValue);
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
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void updateLockLevel(String projectName, LockLevel lockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProjectDescription(projectName);
		project.getACL().setLockableWithLevel(lockLevel);
	}

}
