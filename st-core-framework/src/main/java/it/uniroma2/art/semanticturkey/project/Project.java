/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is Semantic Turkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http//ai-nlp.info.uniroma2.it/software/...
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.project;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.VocabularyInitializationException;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.SKOSModel;
import it.uniroma2.art.owlart.models.SKOSXLModel;
import it.uniroma2.art.owlart.models.UnloadableModelConfigurationException;
import it.uniroma2.art.owlart.models.UnsupportedModelConfigurationException;
import it.uniroma2.art.owlart.models.conf.BadConfigurationException;
import it.uniroma2.art.owlart.models.conf.ModelConfiguration;
import it.uniroma2.art.owlart.utilities.ModelUtilities;
import it.uniroma2.art.owlart.vocabulary.VocabUtilities;
import it.uniroma2.art.semanticturkey.SemanticTurkey;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectIncompatibleException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerFactory;
import it.uniroma2.art.semanticturkey.ontology.STOntologyManager;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.configuration.PluginConfiguration;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.RDFSRenderingEngineFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.SKOSRenderingEngineFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.SKOSXLRenderingEngineFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.vocabulary.SemAnnotVocab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Project<MODELTYPE extends RDFModel> extends AbstractProject {

	protected File infoSTPFile;
	protected File modelConfigFile;

	protected STOntologyManager<MODELTYPE> ontManager;
	Class<? extends ModelConfiguration> modelConfigClass;
	ModelConfiguration modelConfiguration;

	public static final String INFOFILENAME = "project.info";
	public static final String MODELCONFIG_FILENAME = "model.config";
	public static final String URI_GENERATOR_CONFIG_FILENAME = "urigen.config";
	public static final String RENDERING_ENGINE_CONFIG_FILENAME = "rendering.config";

	public static final String TIMESTAMP_PROP = "timeStamp";
	public static final String PROJECT_NAME_PROP = "name";
	public static final String MODELCONFIG_ID = "modelConfigID";
	public static final String ONTOLOGY_MANAGER_ID_PROP = "STOntologyManagerID";
	public static final String BASEURI_PROP = "baseURI";
	public static final String DEF_NS_PROP = "defaultNamespace";
	public static final String PROJECT_TYPE = "ProjectType";
	public static final String PROJECT_MODEL_TYPE = "ModelType";
	public static final String PROJECT_STORE_DIR_NAME = "store";
	public static final String PLUGINS_PROP = "plugins";
	
	// Constants concerning project plugins
	public static final String MANDATORY_PLUGINS_PROP_PREFIX = "plugins.mandatory";
	
	public static final String URI_GENERATOR_PROP_PREFIX = MANDATORY_PLUGINS_PROP_PREFIX + ".urigen";
	public static final String URI_GENERATOR_FACTORY_ID_PROP = URI_GENERATOR_PROP_PREFIX + ".factoryID";
	public static final String URI_GENERATOR_FACTORY_ID_DEFAULT_PROP_VALUE = NativeTemplateBasedURIGeneratorFactory.class.getName();
	public static final String URI_GENERATOR_CONFIGURATION_TYPE_PROP = URI_GENERATOR_PROP_PREFIX + ".configType";

	public static final String RENDERING_ENGINE_PROP_PREFIX = MANDATORY_PLUGINS_PROP_PREFIX + ".rendering";
	public static final String RENDERING_ENGINE_FACTORY_ID_PROP = RENDERING_ENGINE_PROP_PREFIX + ".factoryID";
	public static final String RENDERING_ENGINE_FACTORY_ID_DEFAULT_PROP_VALUE = RDFSRenderingEngineFactory.class.getName();
	public static final String RENDERING_ENGINE_CONFIGURATION_TYPE_PROP = RENDERING_ENGINE_PROP_PREFIX + ".configType";


	// this hashset, used by the setProperty(...) method, prevents ST system properties from being
	// accidentally overwritten by third party plugins/extensions
	public static final HashSet<String> reservedProperties = new HashSet<String>();
	static {

		reservedProperties.add(INFOFILENAME);
		reservedProperties.add(MODELCONFIG_FILENAME);
		reservedProperties.add(TIMESTAMP_PROP);
		reservedProperties.add(PROJECT_NAME_PROP);
		reservedProperties.add(MODELCONFIG_ID);
		reservedProperties.add(ONTOLOGY_MANAGER_ID_PROP);
		reservedProperties.add(BASEURI_PROP);
		reservedProperties.add(DEF_NS_PROP);
		reservedProperties.add(PROJECT_TYPE);
		reservedProperties.add(PROJECT_MODEL_TYPE);
		reservedProperties.add(PROJECT_STORE_DIR_NAME);
		reservedProperties.add(PLUGINS_PROP);
	}

	private static final String SEPARATION_SYMBOL = ";";

	protected static Logger logger = LoggerFactory.getLogger(Project.class);

	private Properties stp_properties;
	public NSPrefixMappings nsPrefixMappingsPersistence;

	private ProjectACL acl;
	private URIGenerator uriGenerator;
	private RenderingEngine renderingEngine;

	/**
	 * this constructor always assumes that the project folder actually exists. Accessing an already existing
	 * folder or creating a new one is in charge of the ProjectManager
	 * <p>
	 * the created project gives access to all of its properties, though it needs to be {@link #activate()}d
	 * for its RDF content to be accessed
	 * </p>
	 * <p>
	 * implementation of this constructor by subclasses <b><i>must</i></b> take care of properly initializing
	 * the <code>nsPrefixMappingsPersistence</code> field.
	 * 
	 * @param projectName
	 * @param projectDir
	 * @throws ProjectCreationException
	 */
	Project(String projectName, File projectDir) throws ProjectCreationException {
		super(projectName, projectDir);
		logger.debug("initializing project: " + projectName);
		infoSTPFile = new File(projectDir, INFOFILENAME);
		modelConfigFile = new File(projectDir, MODELCONFIG_FILENAME);

		stp_properties = new Properties();
		try {
			FileInputStream propFileInStream = new FileInputStream(infoSTPFile);
			stp_properties.load(propFileInStream);
			propFileInStream.close();
			acl = new ProjectACL(this);
		} catch (IOException e1) {
			throw new ProjectCreationException(e1);
		}
	}

	void activate() throws ProjectIncompatibleException, ProjectInconsistentException,
			ModelCreationException, ProjectUpdateException, UnavailableResourceException,
			UnsupportedModelConfigurationException, UnloadableModelConfigurationException,
			ProjectAccessException {
		try {
			OntologyManagerFactory<ModelConfiguration> ontMgrFact = PluginManager
					.getOntManagerImpl(getOntologyManagerImplID());
			ontManager = ontMgrFact.createOntologyManager(this);
			if (ontManager == null)
				throw new ProjectIncompatibleException(
						"there is no OSGi bundle loaded in Semantic Turkey for the required OntologyManager: "
								+ getOntologyManagerImplID());

			try {
				modelConfiguration = ontMgrFact.createModelConfigurationObject(getModelConfigurationID());
				modelConfiguration.loadParameters(modelConfigFile);
			} catch (ClassNotFoundException e) {
				throw new ProjectAccessException(e);
			} catch (BadConfigurationException e) {
				throw new ProjectAccessException(e);
			} catch (IOException e) {
				throw new ProjectAccessException(e);
			}

			String baseURI = getBaseURI();
			if (baseURI == null)
				throw new ProjectInconsistentException("baseURI is not specified");

			// Activation of the URI Generator for this project
			String uriGenFactoryID = getProperty(URI_GENERATOR_FACTORY_ID_PROP);
			String uriGenConfigType = getProperty(URI_GENERATOR_CONFIGURATION_TYPE_PROP);

			if (uriGenFactoryID == null) {
				uriGenFactoryID = URI_GENERATOR_FACTORY_ID_DEFAULT_PROP_VALUE;
			}
			
			try {
				PluginFactory<?> uriGenFactory = PluginManager.getPluginFactory(uriGenFactoryID);
				PluginConfiguration uriGenConfig;
				
				if (uriGenConfigType != null) {
					uriGenConfig = uriGenFactory.createPluginConfiguration(uriGenConfigType);
					uriGenConfig.loadParameters(new File(_projectDir, URI_GENERATOR_CONFIG_FILENAME));
				} else {
					uriGenConfig = uriGenFactory.createDefaultPluginConfiguration();
				}
				
				logger.debug("instantiating URIGenerator. PluginFactory.getID() = {} // PluginConfiguration = {}", uriGenFactory.getID(), uriGenConfig); 

				this.uriGenerator = (URIGenerator)uriGenFactory.createInstance(uriGenConfig);
			} catch (IOException | it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException | ClassNotFoundException | UnsupportedPluginConfigurationException | UnloadablePluginConfigurationException e) {
				throw new ProjectAccessException(e);
			}
			
			// Activation of the rendering engine for this project
			String renderingEngineFactoryID = getProperty(RENDERING_ENGINE_FACTORY_ID_PROP);
			String renderingEngineConfigType = getProperty(RENDERING_ENGINE_CONFIGURATION_TYPE_PROP);

			if (renderingEngineFactoryID == null) {
				renderingEngineFactoryID = determineBestRenderingEngine(getModelType());
			}
			
			try {
				PluginFactory<?> renderingEngineFactory = PluginManager.getPluginFactory(renderingEngineFactoryID);
				PluginConfiguration renderingEngineConfig;
				
				if (renderingEngineConfigType != null) {
					renderingEngineConfig = renderingEngineFactory.createPluginConfiguration(renderingEngineConfigType);
					renderingEngineConfig.loadParameters(new File(_projectDir, RENDERING_ENGINE_CONFIG_FILENAME));
				} else {
					renderingEngineConfig = renderingEngineFactory.createDefaultPluginConfiguration();
				}
				
				logger.debug("instantiating RenderingEngine. PluginFactory.getID() = {} // PluginConfiguration = {}", renderingEngineFactory.getID(), renderingEngineConfig); 

				this.renderingEngine = (RenderingEngine)renderingEngineFactory.createInstance(renderingEngineConfig);
			} catch (IOException | it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException | ClassNotFoundException | UnsupportedPluginConfigurationException | UnloadablePluginConfigurationException e) {
				throw new ProjectAccessException(e);
			}

			
			logger.debug("activation of project: " + getName() + ": baseuri and OntologyManager ok");

			// activates the ontModel loads the triples (implementation depends on project type)
			loadTriples();
			logger.debug("activation of project: " + getName() + ": all triples loaded");
			String defaultNamespace = getDefaultNamespace();
			if (defaultNamespace == null) {
				defaultNamespace = ModelUtilities.createDefaultNamespaceFromBaseURI(baseURI);
				logger.info("generating defaultNamespace from baseuri: " + defaultNamespace);
			}

			// retrieves the default namespace (in case it is persisted somehow by the triple store
			// if it is null (non persisted) or different from the one stored in the project (well, that would
			// be really weird), it sets it again as an initialization step
			String liveGotNS = getOntModel().getDefaultNamespace();
			if (liveGotNS == null || !liveGotNS.equals(defaultNamespace)) {
				logger.debug("activation of project: " + getName() + ": found defaultnamespace: " + liveGotNS);
				getOntModel().setDefaultNamespace(defaultNamespace);
				logger.info("activation of project: " + getName() + ": defaultnamespace set to: "
						+ defaultNamespace);
				// it may seem strange that ST is reporting the msg:
				// "a mapping already exists for" <def namespace here of the current project>
				// this is due to a non-properly managed def namespace in owlart sesame implementation
				// it seems last sesame versions are persisting the namespace prefix mapping. In this case,
				// there should be no need of writing explicitly a further variable (as it does at least until
				// owlart-sesame2 version 1.1), containing the default namespace. What happens is that
				// the liveGotNS is null, though actually sesame has the right namespace associated to the
				// empty prefix "".
				// in future implementations of sesame2, the default namespace should be only inferable from
				// the "" prefix, and no redundanc should be present
			}

			ontManager.declareApplicationOntology(getOntModel().createURIResource(SemAnnotVocab.NAMESPACE),
					false, true);

			// nsPrefixMappingsPersistence must have been already created by constructor of Project subclasses
			ontManager.initializeMappingsPersistence(nsPrefixMappingsPersistence);

			SemanticTurkey.initializeVocabularies(getOntModel());
			logger.info("defaultnamespace set to: " + defaultNamespace);
		} catch (ModelAccessException e) {
			throw new ProjectUpdateException(e);
		} catch (ModelUpdateException e) {
			throw new ProjectUpdateException(e);
		} catch (VocabularyInitializationException e) {
			throw new ProjectUpdateException(e);
		}

		updateTimeStamp();
	}

	public static String determineBestRenderingEngine(Class<? extends RDFModel> modelType) {
		if (modelType == SKOSModel.class) {
			return SKOSRenderingEngineFactory.class.getName();
		} else if (modelType == SKOSXLModel.class) {
			return SKOSXLRenderingEngineFactory.class.getName();
		} else {
			return RDFSRenderingEngineFactory.class.getName();
		}
	}
	
	/**
	 * this initializes the {@link #model} field with a newly created {@link OWLModel} for this project
	 * 
	 * @throws ModelCreationException
	 */
	protected abstract void loadTriples() throws ModelCreationException;

	private void updateProjectProperties() throws IOException {
		FileOutputStream os = new FileOutputStream(infoSTPFile);
		// properties.storeToXML(os, "local cache references for mirroring remote ontologies");
		stp_properties.store(os, "properties of local project");
		os.close();
	}

	public long getTimeStamp() {
		return Long.parseLong(stp_properties.getProperty(TIMESTAMP_PROP));
	}

	public String getOntologyManagerImplID() throws ProjectInconsistentException {
		return getRequiredProperty(ONTOLOGY_MANAGER_ID_PROP);
	}
	
	/*
	 * @SuppressWarnings("unchecked") public Class<? extends STOntologyManager> getOntologyManagerImplClass()
	 * throws ProjectInconsistentException { return getClassTypedProperty(ONTOLOGY_MANAGER_ID_PROP,
	 * STOntologyManager.class); }
	 */

	/*
	 * public Class<? extends ModelConfiguration> getModelConfigurationClass() throws
	 * ProjectInconsistentException { return getClassTypedProperty(MODELCONFIG_ID, ModelConfiguration.class);
	 * }
	 */

	// casting is checked internally
	@SuppressWarnings("unchecked")
	public Class<MODELTYPE> getModelType() throws ProjectInconsistentException {
		return (Class<MODELTYPE>) getClassTypedProperty(Project.PROJECT_MODEL_TYPE, RDFModel.class);
	}

	@SuppressWarnings("unchecked")
	private <T> Class<? extends T> getClassTypedProperty(String propertyName, Class<T> definingClass)
			throws ProjectInconsistentException {
		logger.debug("getting " + propertyName + " for this project");
		String propValue = getProperty(propertyName);
		logger.debug(propertyName + " declared for this project: " + propValue);
		Class<?> classTypedPropertyValue;
		if (propValue == null) {
			throw new ProjectInconsistentException("property: " + propertyName
					+ " not defined for this project");
		}
		try {
			classTypedPropertyValue = Class.forName(propValue);
			// this should check that the returned model is a subclass of RDFModel
			logger.debug(propertyName + " for this project: " + classTypedPropertyValue);
			if (definingClass.isAssignableFrom(classTypedPropertyValue))
				return (Class<? extends T>) classTypedPropertyValue;
			else
				throw new ProjectInconsistentException(propertyName + " \"" + classTypedPropertyValue
						+ "\" assigned to this project is a legal java class, but is not a know "
						+ definingClass.getName() + " subclass");

		} catch (ClassNotFoundException e) {
			throw new ProjectInconsistentException("class " + e.getMessage() + ", specified as: "
					+ propertyName + " for this project, does not exists");
		}
	}

	public String getModelConfigurationID() throws ProjectInconsistentException {
		return getRequiredProperty(MODELCONFIG_ID);
	}

	public STOntologyManager<MODELTYPE> getOntologyManager() {
		return ontManager;
	}

	public String getName() {
		return stp_properties.getProperty(PROJECT_NAME_PROP);
	}

	public String getBaseURI() {
		return stp_properties.getProperty(BASEURI_PROP);
	}

	public String getDefaultNamespace() {
		return stp_properties.getProperty(DEF_NS_PROP);
	}

	public String getType() throws ProjectInconsistentException {
		return getRequiredProperty(PROJECT_TYPE);
	}

	String getRequiredProperty(String propertyName) throws ProjectInconsistentException {
		String propValue = stp_properties.getProperty(propertyName);
		if (propValue != null)
			return propValue;
		else
			throw new ProjectInconsistentException("missing required " + propertyName
					+ " value from description of project: " + this.getName());
	}

	/**
	 * returns the value associated to a given property for this project
	 * 
	 * @param propName
	 *            the name of the queried property
	 * @return the value associated to this property
	 */
	public String getProperty(String propName) {
		logger.debug("getting value of property: " + propName);
		return stp_properties.getProperty(propName);
	}

	public void setProperty(String propName, String propValue) throws ProjectUpdateException,
			ReservedPropertyUpdateException {
		logger.debug("setting property: " + propName + " to value: " + propValue);
		if (reservedProperties.contains(propName))
			throw new ReservedPropertyUpdateException(propName);

		String oldValue = stp_properties.getProperty(propName);
		try {
			stp_properties.setProperty(propName, propValue);
			updateProjectProperties();
		} catch (IOException e) {
			stp_properties.setProperty(propName, oldValue);
			throw new ProjectUpdateException(e);
		}

	}

	public void setBaseURI(String baseURI) throws ProjectUpdateException {
		try {
			getOntModel().setBaseURI(baseURI);
			stp_properties.setProperty(BASEURI_PROP, baseURI);
			updateProjectProperties();
		} catch (Exception e) {
			throw new ProjectUpdateException(e);
		}
	}

	public void setName(String name) throws ProjectUpdateException {
		try {
			stp_properties.setProperty(PROJECT_NAME_PROP, name);
			updateProjectProperties();
		} catch (Exception e) {
			throw new ProjectUpdateException(e);
		}
	}

	public void setDefaultNamespace(String defaultNamespace) throws ProjectUpdateException {
		try {
			getOntModel().setDefaultNamespace(defaultNamespace);
			stp_properties.setProperty(DEF_NS_PROP, defaultNamespace);
			updateProjectProperties();
		} catch (Exception e) {
			throw new ProjectUpdateException(e);
		}
	}

	public void updateTimeStamp() throws ProjectUpdateException {
		Date currentDate = new Date();
		stp_properties.setProperty(TIMESTAMP_PROP, Long.toString(currentDate.getTime()));
		try {
			updateProjectProperties();
		} catch (IOException e) {
			throw new ProjectUpdateException(e);
		}
	}

	/**
	 * Registers the plugin with name <code>pluginName</code> to this project.<br/>
	 * The plugin is registered by adding its name to the column-separated property "plugins" stored in the
	 * project property file.
	 * 
	 * @param pluginName
	 * @throws DuplicatedResourceException
	 * @throws ProjectUpdateException
	 */
	public void registerPlugin(String pluginName) throws DuplicatedResourceException, ProjectUpdateException {

		// loads the string with all previously registered plugin names, and if there is no plugin,
		// istantiates the string with the new one, otherwise reparses this string to get all previous
		// plugins (to check that the new plugin to be registered is not already present among any of the
		// previous ones), then adds the new one and then stores again the string
		String pluginsString = stp_properties.getProperty(PLUGINS_PROP);
		if (pluginsString == null) {
			pluginsString = pluginName;
		} else {
			String[] plugins = pluginsString.split(SEPARATION_SYMBOL);
			if (plugins.length == 0)
				pluginsString = pluginName;
			// checks that the plugin has not already been registered for this project
			else {
				for (int i = 0; i < plugins.length; i++) {
					if (plugins[i].equals(pluginName))
						throw new DuplicatedResourceException(
								"a plugin with this name is already associated to this project; this may be due to a naming conflict between two plugins or an incorrect deregistration of the same one");
				}
				pluginsString += SEPARATION_SYMBOL + pluginName;
			}
		}
		stp_properties.setProperty(PLUGINS_PROP, pluginsString);
		try {
			updateProjectProperties();
		} catch (IOException e) {
			throw new ProjectUpdateException(e);
		}
	}

	private String addPluginToPropertyValue(String pluginName, String propValue) {
		if (propValue.equals(""))
			return pluginName;
		else
			return propValue + SEPARATION_SYMBOL + pluginName;
	}

	/**
	 * removes the plugin with name <code>pluginName</code> from this project. If the project does not contain
	 * any registered plugin, or if the plugin is not registered among the reigistered ones, than a
	 * {@link ProjectUpdateException} is thrown, with an appropriate message
	 * 
	 * @param pluginName
	 * @throws ProjectUpdateException
	 */
	public void deregisterPlugin(String pluginName) throws ProjectUpdateException {

		boolean modified = false;
		String pluginsString = stp_properties.getProperty(PLUGINS_PROP);

		if (pluginsString == null)
			throw new ProjectUpdateException("unable to deregister plugin: " + pluginName
					+ " actually it seems there is no plugin associated to this project");

		String[] plugins = pluginsString.split(SEPARATION_SYMBOL);

		if (plugins.length == 0)
			throw new ProjectUpdateException("unable to deregister plugin: " + pluginName
					+ " actually it seems there is no plugin associated to this project");

		pluginsString = "";
		for (int i = 0; i < plugins.length; i++) {
			if (!plugins[i].equals(pluginName))
				pluginsString = addPluginToPropertyValue(plugins[i], pluginsString);
			else
				modified = true;
		}

		if (!modified)
			throw new ProjectUpdateException("unable to deregister plugin: " + pluginName
					+ " because it does not appear to be associated to this project");
		stp_properties.setProperty(PLUGINS_PROP, pluginsString);
		try {
			updateProjectProperties();
		} catch (IOException e) {
			throw new ProjectUpdateException(e);
		}
	}

	/**
	 * returns the list of registered plugins
	 * 
	 * @return a List containing the names of the registered plugins
	 */
	public List<String> getRegisteredPlugins() {
		String pluginsString = stp_properties.getProperty(PLUGINS_PROP);
		if (pluginsString == null) {
			return new ArrayList<String>();
		} else {
			String[] plugins = pluginsString.split(SEPARATION_SYMBOL);
			return Arrays.asList(plugins);
		}
	}

	public MODELTYPE getOntModel() {
		return getOntologyManager().getOntModel();
	}

	// TODO this should really only be in OWLModel and SKOSModel Projects
	public OWLModel getOWLModel() {
		return getOntologyManager().getOWLModel();
	}

	public File getProjectStoreDir() {
		return new File(_projectDir, PROJECT_STORE_DIR_NAME);
	}

	public String toString() {
		return "proj:" + getName() + "|defNS:" + getDefaultNamespace() + "|TS:" + getTimeStamp();
	}

	// ACCESS CONTROL

	public ProjectACL getACL() {
		return acl;
	}
	
	public URIGenerator getURIGenerator() {
		return this.uriGenerator;
	}
	
	public RenderingEngine getRenderingEngine() {
		return this.renderingEngine;
	}
	
	// Auxiliary graphs management

	private static final String AUXILIARY_METADATA_GRAPH_NAME_BASE = "http://semanticturkey/";
	private static final String AUXILIARY_METADATA_GRAPH_SUFFIX = "/meta";

	public ARTResource getMetadataGraph(String extensionPathComponent) {
		return VocabUtilities.nodeFactory.createURIResource(AUXILIARY_METADATA_GRAPH_NAME_BASE
				+ extensionPathComponent + AUXILIARY_METADATA_GRAPH_SUFFIX);
	}

}
