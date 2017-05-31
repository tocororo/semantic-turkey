package it.uniroma2.art.semanticturkey.services.core;

import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.PUBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
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
			@Optional PluginSpecification renderingEngineSpecification) throws ProjectInconsistentException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, DuplicatedResourceException, ProjectCreationException,
			ClassNotFoundException, BadConfigurationException, UnsupportedPluginConfigurationException,
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

		Project proj = ProjectManager.createProject(consumer, projectName, model,
				lexicalizationModel, baseURI, historyEnabled, validationEnabled, repositoryAccess, coreRepoID,
				coreRepoSailConfigurerSpecification, supportRepoID, supportRepoSailConfigurerSpecification,
				uriGeneratorSpecification, renderingEngineSpecification);

		STUser loggedUser = UsersManager.getLoggedUser();
		// TODO is correct to assign administrator role to the user that creates project?
		// if not how do I handle the administrator role since the role is related to a project?
		ProjectUserBindingsManager.addRoleToPUBinding(loggedUser, proj,
				RBACManager.DefaultRole.ADMINISTRATOR);

		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		objectNode.set("type", JsonNodeFactory.instance.textNode(proj.getType()));

		return objectNode;
	}
};