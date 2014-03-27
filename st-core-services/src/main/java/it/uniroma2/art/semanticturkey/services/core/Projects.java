package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectDeletionException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.main.ProjectsOld.Req;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.util.Collection;
import java.util.Properties;

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
	public void createProject(ProjectConsumer consumer, String projectName,
			Class<? extends RDFModel> modelType, String baseURI, String ontManagerFactoryID,
			String modelConfigurationClass, Properties modelConfiguration)
			throws DuplicatedResourceException, InvalidProjectNameException, ProjectCreationException,
			ProjectInconsistentException, ProjectUpdateException {

		ProjectManager.createProject(consumer, projectName, modelType, baseURI, ontManagerFactoryID,
				modelConfigurationClass, modelConfiguration);
	}

	@GenerateSTServiceController
	public void deleteProject(ProjectConsumer consumer, String projectName)
			throws ProjectDeletionException {
		ProjectManager.deleteProject(projectName);
	}

	/**
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
			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel) throws ProjectAccessException {

		System.out.println("consumer = " + consumer);
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

}
