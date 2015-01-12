package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.model.NodeFilters;
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
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.SaveToStoreProject;
import it.uniroma2.art.semanticturkey.resources.UpdateRoutines;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
//import it.uniroma2.art.semanticturkey.servlet.main.ProjectsOld.Req;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

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
public class Projects extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Projects.class);

	// requests
		public static class Req {
			public final static String createProjectRequest = "createProject";
			public final static String listProjectsRequest = "listProjects";
			public final static String getProjectPropertyRequest = "getProjectProperty";
			public final static String getProjectPropertyFileContentRequest = "getProjectPropertyFileContent";
			public final static String getProjectPropertyMapRequest = "getProjectPropertyMap";
			
			/*public final static String isCurrentProjectActiveRequest = "isCurrentProjectActive";
			public final static String openProjectRequest = "openProject";
			public final static String createNewProjectRequest = "newProject";
			public final static String createNewProjectFromFileRequest = "newProjectFromFile";
			public final static String closeProjectRequest = "closeProject";
			public final static String deleteProjectRequest = "deleteProject";
			public final static String exportProjectRequest = "exportProject";
			public final static String importProjectRequest = "importProject";
			public final static String cloneProjectRequest = "cloneProject";
			public final static String saveProjectRequest = "saveProject";
			public final static String saveProjectAsRequest = "saveProjectAs";
			public final static String listProjectsRequest = "listProjects";
			public final static String getProjectPropertyRequest = "getProjectProperty";
			public final static String setProjectPropertyRequest = "setProjectProperty";
			public final static String getCurrentProjectRequest = "getCurrentProject";
			public final static String repairProjectRequest = "repairProject";*/
	}
	
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
	public Response createProject(ProjectConsumer consumer, String projectName,
			Class<? extends RDFModel> modelType, String baseURI, String ontManagerFactoryID,
			String modelConfigurationClass, Properties modelConfiguration)
			throws DuplicatedResourceException, InvalidProjectNameException, ProjectCreationException,
			ProjectInconsistentException, ProjectUpdateException {

		Project<? extends RDFModel> proj = ProjectManager.createProject(consumer, projectName, modelType, baseURI, ontManagerFactoryID,
				modelConfigurationClass, modelConfiguration);
		
		String request = Req.createProjectRequest;
		XMLResponseREPLY response = ServletUtilities.getService().createReplyResponse(request,
				RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		XMLHelp.newElement(dataElement, "type", proj.getType());
		return response;
	}

	/*@GenerateSTServiceController
	public void createProject(ProjectConsumer consumer, String projectName,
			Class<? extends RDFModel> modelType, String baseURI, String ontManagerFactoryID,
			String modelConfigurationClass, Properties modelConfiguration, File rdfFile)
			throws DuplicatedResourceException, InvalidProjectNameException, ProjectCreationException,
			ProjectInconsistentException, ProjectUpdateException {

		Project<?> project = ProjectManager.createProject(consumer, projectName, modelType, baseURI,
				ontManagerFactoryID, modelConfigurationClass, modelConfiguration);
		try {
			project.getOntModel().addRDF(rdfFile, baseURI, RDFFormat.guessRDFFormatFromFile(rdfFile),
					NodeFilters.MAINGRAPH);
		} catch (Exception e) {
			try {
				ProjectManager.deleteProject(projectName);
			} catch (ProjectDeletionException e1) {
				throw new ProjectCreationException(
						"a problem raised when creating the project, however, we were not able to delete the folder which has been created for it; please delete it manually.\n"
								+ "Also, the system was unable to delete the project. Pls remove it manually");
			}
			throw new ProjectCreationException(
					"a problem raised when creating the project, however, we were not able to delete the folder which has been created for it; please delete it manually");

		}
	}*/

	@GenerateSTServiceController
	public void deleteProject(ProjectConsumer consumer, String projectName) throws ProjectDeletionException {
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
	 */
	@GenerateSTServiceController
	public void accessProject(ProjectConsumer consumer, String projectName,
			ProjectACL.AccessLevel requestedAccessLevel, ProjectACL.LockLevel requestedLockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException {

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
		String request = Req.listProjectsRequest;
		Collection<AbstractProject> projects;

		projects = ProjectManager.listProjects(consumer);
		XMLResponseREPLY resp = servletUtilities.createReplyResponse(request, RepliesStatus.ok);
		Element dataElem = resp.getDataElement();

		for (AbstractProject absProj : projects) {
			Element projElem = XMLHelp.newElement(dataElem, XMLNames.projectTag, absProj.getName());
			if (absProj instanceof Project<?>) {
				Project<? extends RDFModel> proj = (Project<? extends RDFModel>) absProj;
				try {
					projElem.setAttribute(XMLNames.ontoTypeAttr, ((Project<?>) proj).getModelType().getName());
					String ontMgr = ((Project<?>) proj).getOntologyManagerImplID();
					projElem.setAttribute(XMLNames.ontMgrAttr, ontMgr);
					String mConfID = ((Project<?>) proj).getModelConfigurationID();
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
	public void cloneProject(String projectName, String newProjectName) throws InvalidProjectNameException,
			DuplicatedResourceException, IOException, UnavailableResourceException,
			ProjectInexistentException {

		logger.info("requested to export current project");

		ProjectManager.cloneProjectToNewProject(projectName, newProjectName);

	}

	/**
	 * exports a project to a given project file in Semantic Turkey project archive format
	 * 
	 * @param projectName
	 * @return
	 * @throws UnavailableResourceException
	 * @throws UnsupportedRDFFormatException
	 * @throws ModelAccessException
	 * @throws IOException
	 */
	public void exportProject(String projectName, String exportPackage) throws IOException,
			ModelAccessException, UnsupportedRDFFormatException, UnavailableResourceException {

		logger.info("requested to export current project");
		File projectFile = new File(exportPackage);
		ProjectManager.exportProject(projectName, projectFile);
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
	 */
	public void importProject(String importPackage, String newProjectName) throws IOException,
			ModelAccessException, UnsupportedRDFFormatException, ProjectCreationException,
			DuplicatedResourceException, ProjectInconsistentException, ProjectUpdateException,
			ModelUpdateException, InvalidProjectNameException {

		logger.info("requested to import project from file: " + importPackage);

		File projectFile = new File(importPackage);
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
	public Response getProjectProperty(String projectName, String[] propertyNames)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {

		//String[] propNames = propNameList.split(";");
		String[] propValues = new String[propertyNames.length];

		for (int i = 0; i < propertyNames.length; i++)
			propValues[i] = ProjectManager.getProjectProperty(projectName, propertyNames[i]);

		XMLResponseREPLY resp = servletUtilities.createReplyResponse(Req.getProjectPropertyRequest,
				RepliesStatus.ok);
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
	public Response getProjectPropertyMap(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {

		Map<String, String> map = ProjectManager.getProjectPropertyMap(projectName);

		XMLResponseREPLY resp = servletUtilities.createReplyResponse(Req.getProjectPropertyMapRequest,
				RepliesStatus.ok);
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
	public Response getProjectPropertyFileContent(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {
		String rawFile = ProjectManager.getProjectPropertyFileContent(projectName);
		XMLResponseREPLY resp = servletUtilities.createReplyResponse(Req.getProjectPropertyFileContentRequest,
				RepliesStatus.ok);
		Element dataElem = resp.getDataElement();
		Element contentElem = XMLHelp.newElement(dataElem, "content");
		contentElem.setTextContent(rawFile);
		return resp;
	}
	
	@GenerateSTServiceController (method = RequestMethod.POST)
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

}
