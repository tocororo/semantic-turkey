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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SESAMEQNAME;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import it.uniroma2.art.owlart.rdf4jimpl.models.BaseRDFModelRDF4JImpl;
import it.uniroma2.art.owlart.rdf4jimpl.models.OWLModelRDF4JImpl;
import it.uniroma2.art.owlart.rdf4jimpl.models.SKOSModelRDF4JImpl;
import it.uniroma2.art.owlart.rdf4jimpl.models.SKOSXLModelRDF4JImpl;
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
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerFactory;
import it.uniroma2.art.semanticturkey.ontology.STOntologyManager;
import it.uniroma2.art.semanticturkey.ontology.impl.OntologyManagerCompatibilityImpl;
import it.uniroma2.art.semanticturkey.ontology.impl.OntologyManagerImpl;
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
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryTransactionManager;
import it.uniroma2.art.semanticturkey.vocabulary.SemAnnotVocab;

public abstract class Project<MODELTYPE extends RDFModel> extends AbstractProject {

	protected File infoSTPFile;
	protected File modelConfigFile;
	protected File renderingConfigFile;
	protected File uriGenConfigFile;

	protected STOntologyManager<MODELTYPE> ontManager;
	protected OntologyManager newOntManager;
	protected OntologyManagerImpl supportOntManager;

	Class<? extends ModelConfiguration> modelConfigClass;
	ModelConfiguration modelConfiguration;

	public static final String INFOFILENAME = "project.info";
	public static final String MODELCONFIG_FILENAME = "model.config";
	public static final String COREREPOCONFIG_FILENAME = "core-repo.ttl";
	public static final String SUPPORTREPOCONFIG_FILENAME = "support-repo.ttl";

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
	public static final String PROJECT_COREREPO_DIR_NAME = "core";
	public static final String PROJECT_SUPPORTREPO_DIR_NAME = "support";

	public static final String PLUGINS_PROP = "plugins";

	// Constants concerning project plugins
	public static final String MANDATORY_PLUGINS_PROP_PREFIX = "plugins.mandatory";

	public static final String URI_GENERATOR_PROP_PREFIX = MANDATORY_PLUGINS_PROP_PREFIX + ".urigen";
	public static final String URI_GENERATOR_FACTORY_ID_PROP = URI_GENERATOR_PROP_PREFIX + ".factoryID";
	public static final String URI_GENERATOR_FACTORY_ID_DEFAULT_PROP_VALUE = NativeTemplateBasedURIGeneratorFactory.class
			.getName();
	public static final String URI_GENERATOR_CONFIGURATION_TYPE_PROP = URI_GENERATOR_PROP_PREFIX
			+ ".configType";

	public static final String RENDERING_ENGINE_PROP_PREFIX = MANDATORY_PLUGINS_PROP_PREFIX + ".rendering";
	public static final String RENDERING_ENGINE_FACTORY_ID_PROP = RENDERING_ENGINE_PROP_PREFIX + ".factoryID";
	public static final String RENDERING_ENGINE_FACTORY_ID_DEFAULT_PROP_VALUE = RDFSRenderingEngineFactory.class
			.getName();
	public static final String RENDERING_ENGINE_CONFIGURATION_TYPE_PROP = RENDERING_ENGINE_PROP_PREFIX
			+ ".configType";

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
	
	private MODELTYPE primordialOntModel;
	private final ThreadLocal<MODELTYPE> modelHolder;
	private RDF4JRepositoryTransactionManager repositoryTransactionManager;
	protected RepositoryConfig coreRepoConfig;
	protected RepositoryConfig supportRepoConfig;

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
		uriGenConfigFile = new File(projectDir, URI_GENERATOR_CONFIG_FILENAME);
		renderingConfigFile = new File(projectDir, RENDERING_ENGINE_CONFIG_FILENAME);
		modelHolder = new ThreadLocal<>();

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

	void activate() throws ProjectIncompatibleException, ProjectInconsistentException, ModelCreationException,
			ProjectUpdateException, UnavailableResourceException, UnsupportedModelConfigurationException,
			UnloadableModelConfigurationException, ProjectAccessException {
		try {
			String ontMgrFactId = getProperty(ONTOLOGY_MANAGER_ID_PROP);

			if (ontMgrFactId != null) {
				OntologyManagerFactory<ModelConfiguration> ontMgrFact = PluginManager
						.getOntManagerImpl(ontMgrFactId);
				ontManager = ontMgrFact.createOntologyManager(this);
				if (ontManager == null)
					throw new ProjectIncompatibleException(
							"there is no OSGi bundle loaded in Semantic Turkey for the required OntologyManager: "
									+ getOntologyManagerImplID());

				newOntManager = new OntologyManagerCompatibilityImpl(ontManager);

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

			} else {
				try {
					File coreRepoConfigFile = new File(_projectDir, COREREPOCONFIG_FILENAME);
					Model coreRepoConfigModel = Rio.parse(new FileInputStream(coreRepoConfigFile),
							coreRepoConfigFile.toURI().toString(), RDFFormat.TURTLE);
					Resource coreRepoRes = Models
							.subject(coreRepoConfigModel.filter(null, RDF.TYPE,
									RepositoryConfigSchema.REPOSITORY))
							.orElseThrow(() -> new ProjectAccessException(
									"Could not find the repository resource in its configuration"));
					coreRepoConfig = RepositoryConfig.create(coreRepoConfigModel, coreRepoRes);

					File supportRepoConfigFile = new File(_projectDir, SUPPORTREPOCONFIG_FILENAME);
					Model supportRepoConfigModel = Rio.parse(new FileInputStream(supportRepoConfigFile),
							supportRepoConfigFile.toURI().toString(), RDFFormat.TURTLE);
					Resource supportRepoRes = Models
							.subject(supportRepoConfigModel.filter(null, RDF.TYPE,
									RepositoryConfigSchema.REPOSITORY))
							.orElseThrow(() -> new ProjectAccessException(
									"Could not find the repository resource in its configuration"));
					supportRepoConfig = RepositoryConfig.create(supportRepoConfigModel, supportRepoRes);

					newOntManager = new OntologyManagerImpl();
					supportOntManager = new OntologyManagerImpl();
				} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
					throw new ProjectAccessException(e);
				}
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

				logger.debug(
						"instantiating URIGenerator. PluginFactory.getID() = {} // PluginConfiguration = {}",
						uriGenFactory.getID(), uriGenConfig);

				this.uriGenerator = (URIGenerator) uriGenFactory.createInstance(uriGenConfig);
			} catch (IOException
					| it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException
					| ClassNotFoundException | UnsupportedPluginConfigurationException
					| UnloadablePluginConfigurationException e) {
				throw new ProjectAccessException(e);
			}

			// Activation of the rendering engine for this project
			String renderingEngineFactoryID = getProperty(RENDERING_ENGINE_FACTORY_ID_PROP);
			String renderingEngineConfigType = getProperty(RENDERING_ENGINE_CONFIGURATION_TYPE_PROP);

			if (renderingEngineFactoryID == null) {
				renderingEngineFactoryID = determineBestRenderingEngine(getModelType());
			}

			try {
				PluginFactory<?> renderingEngineFactory = PluginManager
						.getPluginFactory(renderingEngineFactoryID);
				PluginConfiguration renderingEngineConfig;

				if (renderingEngineConfigType != null) {
					renderingEngineConfig = renderingEngineFactory
							.createPluginConfiguration(renderingEngineConfigType);
					renderingEngineConfig
							.loadParameters(new File(_projectDir, RENDERING_ENGINE_CONFIG_FILENAME));
				} else {
					renderingEngineConfig = renderingEngineFactory.createDefaultPluginConfiguration();
				}

				logger.debug(
						"instantiating RenderingEngine. PluginFactory.getID() = {} // PluginConfiguration = {}",
						renderingEngineFactory.getID(), renderingEngineConfig);

				this.renderingEngine = (RenderingEngine) renderingEngineFactory
						.createInstance(renderingEngineConfig);
			} catch (IOException
					| it.uniroma2.art.semanticturkey.plugin.configuration.BadConfigurationException
					| ClassNotFoundException | UnsupportedPluginConfigurationException
					| UnloadablePluginConfigurationException e) {
				throw new ProjectAccessException(e);
			}

			logger.debug("activation of project: " + getName() + ": baseuri and OntologyManager ok");

			// activates the ontModel loads the triples (implementation depends on project type)
			loadTriples();
			logger.debug("activation of project: " + getName() + ": all triples loaded");

			Repository rdf4jRepo = null;
			try {
				rdf4jRepo = getRepository();
			} catch (IllegalStateException e) {
				// not an RDF4J repostory
			}

			if (ontManager != null) {
				primordialOntModel = ontManager.getOntModel();
			} else {
				String modelType = getProperty(PROJECT_MODEL_TYPE);

				if (OWLModel.class.getName().equals(modelType)) {
					primordialOntModel = (MODELTYPE) new OWLModelRDF4JImpl(new NonClosingBaseRDFModelRDF4JImpl(newOntManager.getRepository(), false, false));
				} else if (SKOSModel.class.getName().equals(modelType)) {
					primordialOntModel = (MODELTYPE) new SKOSModelRDF4JImpl(new NonClosingBaseRDFModelRDF4JImpl(newOntManager.getRepository(), false, false));
				} else if (SKOSXLModel.class.getName().equals(modelType)) {
					primordialOntModel = (MODELTYPE) new SKOSXLModelRDF4JImpl(new NonClosingBaseRDFModelRDF4JImpl(newOntManager.getRepository(), false, false));
				} else {
					throw new ProjectAccessException("Unsupport model type: " + modelType);
				}
			}

			if (rdf4jRepo != null) {
				repositoryTransactionManager = new RDF4JRepositoryTransactionManager(rdf4jRepo);
			}

			String defaultNamespace = getDefaultNamespace();
			if (defaultNamespace == null) {
				defaultNamespace = ModelUtilities.createDefaultNamespaceFromBaseURI(baseURI);
				logger.info("generating defaultNamespace from baseuri: " + defaultNamespace);
			}

			// retrieves the default namespace (in case it is persisted somehow by the triple store
			// if it is null (non persisted) or different from the one stored in the project (well, that would
			// be really weird), it sets it again as an initialization step
			String liveGotNS = getPrimordialOntModel().getDefaultNamespace();
			if (liveGotNS == null || !liveGotNS.equals(defaultNamespace)) {
				logger.debug(
						"activation of project: " + getName() + ": found defaultnamespace: " + liveGotNS);
				getPrimordialOntModel().setDefaultNamespace(defaultNamespace);
				logger.info("activation of project: " + getName() + ": defaultnamespace set to: "
						+ defaultNamespace);
				logger.info("activation of project: " + getName() + ": defaultnamespace set to@@@@ : "
						+ getPrimordialOntModel().getDefaultNamespace());
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

			newOntManager.declareApplicationOntology(SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.NAMESPACE), false, true);

			// nsPrefixMappingsPersistence must have been already created by constructor of Project subclasses
			newOntManager.initializeMappingsPersistence(nsPrefixMappingsPersistence);

			SemanticTurkey.initializeVocabularies(getPrimordialOntModel());
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
	
	public void deactivate() throws ModelUpdateException {
		try {
			getPrimordialOntModel().close();
		} finally {
			try {
				if (newOntManager != null) {
					newOntManager.getRepository().shutDown();
				}
			} finally {
				if (supportOntManager != null) {
					supportOntManager.getRepository().shutDown();
				}
			}
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
			throw new ProjectInconsistentException(
					"property: " + propertyName + " not defined for this project");
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

	public void setProperty(String propName, String propValue)
			throws ProjectUpdateException, ReservedPropertyUpdateException {
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

	public MODELTYPE getPrimordialOntModel() {
		return primordialOntModel;
	}

	public RDFModel unbindModelFromThread() throws ModelUpdateException {
		RDFModel oldModel = modelHolder.get();
		modelHolder.remove();
		if (oldModel == null) {
			logger.warn("Unbinding null model");
		} else {
			oldModel.close();
		}
		logger.debug("Unbound model {}", oldModel);

		return oldModel;
	}
	
	public boolean isModelBoundToThread() {
		return modelHolder.get() != null;
	}

	
	public void createModelAndBoundToThread() throws ModelCreationException {
		if (isModelBoundToThread()) {
			throw new IllegalStateException("Model already bound to thread");
		}
		MODELTYPE forkedModel = (MODELTYPE)getPrimordialOntModel().forkModel();
		modelHolder.set(forkedModel);
		logger.debug("Fork model {} producing new model {}", getPrimordialOntModel(), forkedModel);
	}

	@Deprecated
	public MODELTYPE getOntModel() {
		MODELTYPE model = modelHolder.get();
		
		if (model == null) {
			if (0==0)throw new RuntimeException("Could not obtain thread-bound model");
			logger.warn("Implicit access to primordial model");
//			throw new IllegalStateException("No model has been bound to the current thread");
			return getPrimordialOntModel();
		}
		
		return model;
	}

	@Deprecated
	// TODO this should really only be in OWLModel and SKOSModel Projects
	public OWLModel getOWLModel() {
		MODELTYPE model = getOntModel();
		
		if (model instanceof SKOSModel) {
			return ((SKOSModel)model).getOWLModel();
		}
		
		return (OWLModel)model;
	}

	public Repository getRepository() {
		return newOntManager.getRepository();
	}
	
	public RDF4JRepositoryTransactionManager getRepositoryTransactionManager() {
		if (repositoryTransactionManager != null) {
			return repositoryTransactionManager;
		} else {
			throw new IllegalStateException("Not an RDF4J project");
		}
	}


	public File getProjectStoreDir() {
		return new File(_projectDir, PROJECT_STORE_DIR_NAME);
	}
	
	public File getProjectCoreRepoDir() {
		return new File(_projectDir, PROJECT_COREREPO_DIR_NAME);
	}
	
	public File getProjectSupportRepoDir() {
		return new File(_projectDir, PROJECT_SUPPORTREPO_DIR_NAME);
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

	public OntologyManager getNewOntologyManager() {
		return newOntManager;
	}
}

class NonClosingBaseRDFModelRDF4JImpl extends BaseRDFModelRDF4JImpl {
	private NonClosingBaseRDFModelRDF4JImpl delegate;

	public NonClosingBaseRDFModelRDF4JImpl(Repository repo, boolean rdfsReasoning,
			boolean directTypeReasoning) throws SailException, RepositoryException, ModelCreationException {
		super(repo, rdfsReasoning, directTypeReasoning);
	}

	public NonClosingBaseRDFModelRDF4JImpl(Repository repo, boolean rdfsReasoning,
			boolean directTypeReasoning, NonClosingBaseRDFModelRDF4JImpl delegate)
					throws SailException, RepositoryException, ModelCreationException {
		super(repo, rdfsReasoning, directTypeReasoning);
		this.delegate = delegate;
	}

	@Override
	public void close() throws ModelUpdateException {
		repConn.close();
	}

	@Override
	public BaseRDFModelRDF4JImpl forkModel() throws ModelCreationException {
		if (this.getClass() != NonClosingBaseRDFModelRDF4JImpl.class) {
			throw new IllegalStateException("The model class '" + this.getClass().getName()
					+ "' seems not properly override forkModel()");
		}

		return new NonClosingBaseRDFModelRDF4JImpl(localrepository, rdfsReasoning, directTypeReasoning, this);
	}

	@Override
	public void setDefaultNamespace(String namespace) throws ModelUpdateException {
		if (delegate != null) {
			delegate.setDefaultNamespace(namespace);
		} else {
			super.setDefaultNamespace(namespace);
		}
	}

	@Override
	public String getDefaultNamespace() {
		if (delegate != null) {
			return delegate.getDefaultNamespace();
		} else {
			return super.getDefaultNamespace();
		}
	}

}
