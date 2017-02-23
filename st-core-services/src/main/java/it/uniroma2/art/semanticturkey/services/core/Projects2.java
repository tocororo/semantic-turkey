package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.PUBindingCreationException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;

@STService
public class Projects2 extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Projects2.class);

	// TODO understand how to specify remote repository / different sail configurations
	@STServiceOperation
	public JsonNode createProject(ProjectConsumer consumer, String projectName, Class<? extends RDFModel> modelType,
			String baseURI, boolean historyEnabled, boolean validationEnabled,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory\"}") PluginSpecification uriGeneratorSpecification,
			@Optional PluginSpecification renderingEngineSpecification) throws ProjectInconsistentException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, DuplicatedResourceException, ProjectCreationException,
			ClassNotFoundException, BadConfigurationException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, PUBindingCreationException {

		// If no rendering engine has been configured, guess the best one based on the model type
		if (renderingEngineSpecification == null) {
			String renderingEngineFactoryID = Project.determineBestRenderingEngine(modelType);
			renderingEngineSpecification = new PluginSpecification(renderingEngineFactoryID, null, new Properties());
		}

		uriGeneratorSpecification.expandDefaults();
		renderingEngineSpecification.expandDefaults();

		Project<? extends RDFModel> proj = ProjectManager.createProject2(consumer, projectName, modelType, baseURI,
				historyEnabled, validationEnabled, uriGeneratorSpecification, renderingEngineSpecification);

		// create the folders for the bindings between project and users
		// this is required (is not enough in accessProject, cause accessProject is not invoked after
		// createProject)
		ProjectUserBindingsManager.createPUBindingsOfProject(projectName);

		ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
		objectNode.set("type", JsonNodeFactory.instance.textNode(proj.getType()));

		return objectNode;
	}
};