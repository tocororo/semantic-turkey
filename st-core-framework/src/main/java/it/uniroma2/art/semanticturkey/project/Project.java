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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.SemanticTurkey;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectIncompatibleException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.RepositoryCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.impl.OntologyManagerImpl;
import it.uniroma2.art.semanticturkey.ontology.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.plugin.PluginFactory;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.plugin.extpts.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.plugin.extpts.SearchStrategy;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.RDFSRenderingEngineFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.SKOSRenderingEngineFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.SKOSXLRenderingEngineFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.repository.ReadOnlyRepositoryWrapper;
import it.uniroma2.art.semanticturkey.repository.config.ReadOnlyRepositoryWrapperConfig;
import it.uniroma2.art.semanticturkey.search.SearchStrategyUtils;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryTransactionManager;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;
import it.uniroma2.art.semanticturkey.vocabulary.SemAnnotVocab;

public abstract class Project extends AbstractProject {

	public static final IRI OWL_MODEL = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/2002/07/owl");
	public static final IRI SKOS_MODEL = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/2004/02/skos/core");

	public static final IRI RDFS_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/2000/01/rdf-schema");
	public static final IRI SKOS_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/2004/02/skos/core");
	public static final IRI SKOSXL_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/2008/05/skos-xl");

	protected File infoSTPFile;
	protected File renderingConfigFile;
	protected File uriGenConfigFile;

	protected OntologyManager newOntManager;
	protected OntologyManagerImpl supportOntManager;

	protected IRI model;
	protected IRI lexicalizationModel;

	protected Set<RDFResourceRole> updateForRoles;

	public static final String INFOFILENAME = "project.info";

	public static final String URI_GENERATOR_CONFIG_FILENAME = "urigen.config";
	public static final String RENDERING_ENGINE_CONFIG_FILENAME = "rendering.config";

	public static final String TIMESTAMP_PROP = "timeStamp";
	public static final String PROJECT_NAME_PROP = "name";
	public static final String BASEURI_PROP = "baseURI";
	public static final String DEF_NS_PROP = "defaultNamespace";
	public static final String MODEL_PROP = "model";
	public static final String LEXICALIZATION_MODEL_PROP = "lexicalizationModel";
	public static final String UPDATE_FOR_ROLES_PROP = "updateForRoles";
	public static final String PLUGINS_PROP = "plugins";

	public static final String HISTORY_ENABLED_PROP = "historyEnabled";
	public static final String VALIDATION_ENABLED_PROP = "validationEnabled";

	public static final String CREATION_DATE_PROP = "creationDate";
	public static final String MODIFICATION_DATE_PROP = "modificationDate";

	public static final String VERSIONS_PROP = "versions";

	public static final String DEFAULT_REPOSITORY_LOCATION_PROP = "defaultRepositoryLocation";

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
		reservedProperties.add(TIMESTAMP_PROP);
		reservedProperties.add(PROJECT_NAME_PROP);
		reservedProperties.add(BASEURI_PROP);
		reservedProperties.add(DEF_NS_PROP);
		reservedProperties.add(MODEL_PROP);
		reservedProperties.add(LEXICALIZATION_MODEL_PROP);
		reservedProperties.add(PLUGINS_PROP);
		reservedProperties.add(HISTORY_ENABLED_PROP);
		reservedProperties.add(VALIDATION_ENABLED_PROP);
	}

	private static final String SEPARATION_SYMBOL = ";";

	protected static Logger logger = LoggerFactory.getLogger(Project.class);

	private Properties stp_properties;
	public NSPrefixMappings nsPrefixMappingsPersistence;

	private ProjectACL acl;
	private URIGenerator uriGenerator;
	private RenderingEngine renderingEngine;

	private Map<Repository, RDF4JRepositoryTransactionManager> repository2TransactionManager;
	protected RepositoryConfig coreRepoConfig;
	protected RepositoryConfig supportRepoConfig;
	private STLocalRepositoryManager repositoryManager;
	private VersionManager versionManager;
	private RepositoryLocation defaultRepositoryLocation;

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
		uriGenConfigFile = new File(projectDir, URI_GENERATOR_CONFIG_FILENAME);
		renderingConfigFile = new File(projectDir, RENDERING_ENGINE_CONFIG_FILENAME);

		stp_properties = new Properties();
		try {
			FileInputStream propFileInStream = new FileInputStream(infoSTPFile);
			stp_properties.load(propFileInStream);
			propFileInStream.close();

			model = SimpleValueFactory.getInstance()
					.createIRI(Objects.requireNonNull(stp_properties.getProperty(MODEL_PROP),
							"Project property \"" + MODEL_PROP + "\" must not be null"));
			lexicalizationModel = SimpleValueFactory.getInstance()
					.createIRI(Objects.requireNonNull(stp_properties.getProperty(LEXICALIZATION_MODEL_PROP),
							"Project property \"" + LEXICALIZATION_MODEL_PROP + "\" must not be null"));
			if (!Arrays
					.asList(RDFS_LEXICALIZATION_MODEL, SKOS_LEXICALIZATION_MODEL, SKOSXL_LEXICALIZATION_MODEL)
					.contains(lexicalizationModel)) {
				throw new UnsupportedLexicalizationModelException(
						lexicalizationModel.stringValue() + " is not a valid lexicalization model");
			}

			String updateForRolesString = com.google.common.base.Objects
					.firstNonNull(stp_properties.getProperty(UPDATE_FOR_ROLES_PROP), "resource");

			updateForRoles = Arrays.stream(updateForRolesString.split(",")).map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(s -> s.equals("resource") ? RDFResourceRole.undetermined.name() : s)
					.map(RDFResourceRole::valueOf).collect(Collectors.toSet());
			acl = new ProjectACL(this);
			versionManager = new VersionManager(this);
			defaultRepositoryLocation = Optional.ofNullable(getProperty(DEFAULT_REPOSITORY_LOCATION_PROP))
					.map(RepositoryLocation::fromString).orElse(null);
		} catch (IOException | UnsupportedLexicalizationModelException e1) {
			logger.debug("an exception occurred inside the constructor of a corrupted project", e1);
			throw new ProjectCreationException(e1);
		}
	}

	void activate() throws ProjectIncompatibleException, ProjectInconsistentException, RDF4JException,
			ProjectUpdateException, ProjectAccessException {
		try {
			repositoryManager = new STLocalRepositoryManager(_projectDir);
			repositoryManager.initialize();
			Repository supportRepository = repositoryManager.getRepository("support");

			if (supportRepository != null) {
				supportOntManager = new OntologyManagerImpl(supportRepository, false);
			}

			Repository coreRepository = repositoryManager.getRepository("core");
			newOntManager = new OntologyManagerImpl(coreRepository, isValidationEnabled());

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
				PluginFactory<?, ?, ?, ?, ?> uriGenFactory = PluginManager.getPluginFactory(uriGenFactoryID);
				STProperties uriGenConfig;

				if (uriGenConfigType != null) {
					uriGenConfig = uriGenFactory.createPluginConfiguration(uriGenConfigType);
					uriGenConfig.loadProperties(new File(_projectDir, URI_GENERATOR_CONFIG_FILENAME));
				} else {
					uriGenConfig = uriGenFactory.createDefaultPluginConfiguration();
				}

				logger.debug(
						"instantiating URIGenerator. PluginFactory.getID() = {} // PluginConfiguration = {}",
						uriGenFactory.getID(), uriGenConfig);

				this.uriGenerator = (URIGenerator) uriGenFactory.createInstance(uriGenConfig);
			} catch (IOException | ClassNotFoundException | UnsupportedPluginConfigurationException
					| UnloadablePluginConfigurationException e) {
				throw new ProjectAccessException(e);
			}

			// Activation of the rendering engine for this project
			String renderingEngineFactoryID = getProperty(RENDERING_ENGINE_FACTORY_ID_PROP);
			String renderingEngineConfigType = getProperty(RENDERING_ENGINE_CONFIGURATION_TYPE_PROP);

			if (renderingEngineFactoryID == null) {
				renderingEngineFactoryID = determineBestRenderingEngine(lexicalizationModel);
			}

			try {
				PluginFactory<?, ?, ?, ?, ?> renderingEngineFactory = PluginManager
						.getPluginFactory(renderingEngineFactoryID);
				STProperties renderingEngineConfig;

				if (renderingEngineConfigType != null) {
					renderingEngineConfig = renderingEngineFactory
							.createPluginConfiguration(renderingEngineConfigType);
					renderingEngineConfig
							.loadProperties(new File(_projectDir, RENDERING_ENGINE_CONFIG_FILENAME));
				} else {
					renderingEngineConfig = renderingEngineFactory.createDefaultPluginConfiguration();
				}

				logger.debug(
						"instantiating RenderingEngine. PluginFactory.getID() = {} // PluginConfiguration = {}",
						renderingEngineFactory.getID(), renderingEngineConfig);

				this.renderingEngine = (RenderingEngine) renderingEngineFactory
						.createInstance(renderingEngineConfig);
			} catch (IOException | ClassNotFoundException | UnsupportedPluginConfigurationException
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
				// not an RDF4J repository
			}

			if (rdf4jRepo != null) {
				repository2TransactionManager = new MapMaker().weakKeys().makeMap();
				repository2TransactionManager.put(rdf4jRepo, new RDF4JRepositoryTransactionManager(rdf4jRepo,
						repositoryManager.getSTRepositoryInfo("core")));
			}

			String defaultNamespace = getDefaultNamespace();
			if (defaultNamespace == null) {
				defaultNamespace = ModelUtilities.createDefaultNamespaceFromBaseURI(baseURI);
				logger.debug("generating defaultNamespace from baseuri: " + defaultNamespace);
			}

			try (RepositoryConnection conn = coreRepository.getConnection()) {
				// retrieves the default namespace (in case it is persisted somehow by the triple store
				// if it is null (non persisted) or different from the one stored in the project (well, that
				// would
				// be really weird), it sets it again as an initialization step
				String liveGotNS = conn.getNamespace("");
				if (liveGotNS == null || !liveGotNS.equals(defaultNamespace)) {
					logger.debug(
							"activation of project: " + getName() + ": found defaultnamespace: " + liveGotNS);
					conn.setNamespace("", defaultNamespace);
					logger.debug("activation of project: " + getName() + ": defaultnamespace set to: "
							+ defaultNamespace);
					logger.debug("activation of project: " + getName() + ": defaultnamespace set to@@@@ : "
							+ conn.getNamespace(""));
					// it may seem strange that ST is reporting the msg:
					// "a mapping already exists for" <def namespace here of the current project>
					// this is due to a non-properly managed def namespace in owlart sesame implementation
					// it seems last sesame versions are persisting the namespace prefix mapping. In this
					// case,
					// there should be no need of writing explicitly a further variable (as it does at least
					// until
					// owlart-sesame2 version 1.1), containing the default namespace. What happens is that
					// the liveGotNS is null, though actually sesame has the right namespace associated to the
					// empty prefix "".
					// in future implementations of sesame2, the default namespace should be only inferable
					// from
					// the "" prefix, and no redundanc should be present
				}

				newOntManager.declareApplicationOntology(
						SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.NAMESPACE), false, true);

				// nsPrefixMappingsPersistence must have been already created by constructor of Project
				// subclasses
				newOntManager.initializeMappingsPersistence(nsPrefixMappingsPersistence);

				loadingCoreVocabularies();

				SemanticTurkey.initializeVocabularies(conn);
				logger.debug("defaultnamespace set to: " + defaultNamespace);
			}
		} catch (Exception e) {
			try {
				if (repositoryManager != null && repositoryManager.isInitialized()) {
					repositoryManager.shutDown();
				}
			} catch (RDF4JException e2) {
				throw new ProjectUpdateException(e2);
			}

			throw new ProjectUpdateException(e);
		}

		updateTimeStamp();
	}

	protected void loadingCoreVocabularies() throws RDF4JException, IOException, Exception {
		try (RepositoryConnection conn = newOntManager.getRepository().getConnection()) {
			conn.begin();

			Set<Resource> contexts = QueryResults.asSet(conn.getContextIDs());

			ValueFactory vf = conn.getValueFactory();
			IRI rdfBaseURI = vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns");
			IRI rdfsBaseURI = vf.createIRI("http://www.w3.org/2000/01/rdf-schema");
			IRI owlBaseURI = vf.createIRI("http://www.w3.org/2002/07/owl");
			IRI skosBaseURI = vf.createIRI("http://www.w3.org/2004/02/skos/core");
			IRI skosxlBaseURI = vf.createIRI("http://www.w3.org/2008/05/skos-xl");

			boolean isSKOSXL = Objects.equals(getLexicalizationModel(), SKOSXL_LEXICALIZATION_MODEL);

			MutableBoolean anyWritten = new MutableBoolean(false);

			ValidationUtilities.executeWithoutValidation(isValidationEnabled(), conn, (connection) -> {
				logger.debug("Loading core vocabularies");

				if (!contexts.contains(rdfBaseURI)) {

					logger.debug("Loading RDF vocabulary...");
					conn.add(OntologyManager.class.getResource("rdf.rdf"), rdfBaseURI.stringValue(),
							RDFFormat.RDFXML, rdfBaseURI);
					anyWritten.setValue(true);
				}

				if (!contexts.contains(rdfsBaseURI)) {
					logger.debug("Loading RDFS vocabulary...");
					conn.add(OntologyManager.class.getResource("rdf-schema.rdf"), rdfsBaseURI.stringValue(),
							RDFFormat.RDFXML, rdfsBaseURI);
					anyWritten.setValue(true);
				}

				if (!contexts.contains(owlBaseURI)) {
					logger.debug("Loading OWL vocabulary...");
					conn.add(OntologyManager.class.getResource("owl.rdf"), owlBaseURI.stringValue(),
							RDFFormat.RDFXML, owlBaseURI);
					anyWritten.setValue(true);
				}

				boolean isSKOS = isSKOSXL
						|| Objects.equals(getLexicalizationModel(), SKOS_LEXICALIZATION_MODEL)
						|| Objects.equals(getModel(), SKOS_MODEL);

				if (isSKOS && !contexts.contains(skosBaseURI)) {
					logger.debug("Loading SKOS vocabulary...");
					conn.add(OntologyManager.class.getResource("skos.rdf"), skosBaseURI.stringValue(),
							RDFFormat.RDFXML, skosBaseURI);
					anyWritten.setValue(true);
				}

				if (isSKOSXL && !contexts.contains(skosxlBaseURI)) {
					logger.debug("Loading SKOS-XL vocabulary...");
					conn.add(OntologyManager.class.getResource("skos-xl.rdf"), skosxlBaseURI.stringValue(),
							RDFFormat.RDFXML, skosxlBaseURI);
					anyWritten.setValue(true);
				}

			});

			logger.debug("About to commit the loaded triples");
			conn.commit();

			// If anything has been written, assumes a newly created project, so initialize search
			if (anyWritten.isTrue()) {
				conn.begin();
				SearchStrategy searchStrategy = SearchStrategyUtils
						.instantiateSearchStrategy(STRepositoryInfoUtils
								.getSearchStrategy(getRepositoryManager().getSTRepositoryInfo("core")));
				ValidationUtilities.executeWithoutValidation(isValidationEnabled(), conn, (conn2) -> {
					searchStrategy.initialize(conn);
				});
				conn.commit();
			}

			if (isSKOSXL && !contexts.contains(skosxlBaseURI)) {
				conn.setNamespace("skosxl", SKOSXL.NAMESPACE);
			}
			logger.debug("Core vocabularies loaded");
		}
	}

	public static String determineBestRenderingEngine(IRI lexicalizationModel) {
		if (lexicalizationModel.stringValue().equals("http://www.w3.org/2004/02/skos/core")) {
			return SKOSRenderingEngineFactory.class.getName();
		} else if (lexicalizationModel.stringValue().equals("http://www.w3.org/2008/05/skos-xl")) {
			return SKOSXLRenderingEngineFactory.class.getName();
		} else {
			return RDFSRenderingEngineFactory.class.getName();
		}
	}

	public void deactivate() {
		try {
			repositoryManager.shutDown();
		} finally {
			// try {
			// if (newOntManager != null) {
			// newOntManager.getRepository().shutDown();
			// }
			// } finally {
			// if (supportOntManager != null) {
			// supportOntManager.getRepository().shutDown();
			// }
			// }
		}
	}

	protected abstract void loadTriples() throws RDF4JException;

	private void updateProjectProperties() throws IOException {
		FileOutputStream os = new FileOutputStream(infoSTPFile);
		// properties.storeToXML(os, "local cache references for mirroring remote ontologies");
		stp_properties.store(os, "properties of local project");
		os.close();
	}

	public long getTimeStamp() {
		return Long.parseLong(stp_properties.getProperty(TIMESTAMP_PROP));
	}

	public IRI getModel() {
		return model;
	}

	public IRI getLexicalizationModel() {
		return lexicalizationModel;
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

	public String getName() {
		return stp_properties.getProperty(PROJECT_NAME_PROP);
	}

	public String getBaseURI() {
		return stp_properties.getProperty(BASEURI_PROP);
	}

	public String getDefaultNamespace() {
		return stp_properties.getProperty(DEF_NS_PROP);
	}

	public boolean isHistoryEnabled() {
		return Boolean.valueOf(getProperty(HISTORY_ENABLED_PROP));
	}

	public boolean isValidationEnabled() {
		return Boolean.valueOf(getProperty(VALIDATION_ENABLED_PROP));
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
			// getOntModel().setBaseURI(baseURI);
			newOntManager.setBaseURI(baseURI);
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
			try (RepositoryConnection conn = getRepository().getConnection()) {
				conn.setNamespace("", defaultNamespace);
			}
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

	/**
	 * Returns the core repository associated with this project. Clients should rarely invoke this method, and
	 * use instead the operations found in {@link STServiceContextUtils} (which are aware, for example, of
	 * version dumps).
	 * 
	 * @return
	 */
	public Repository getRepository() {
		return newOntManager.getRepository();
	}

	public RDF4JRepositoryTransactionManager getRepositoryTransactionManager(String repositoryId) {
		Repository repository = repositoryManager.getRepository(repositoryId);
		Optional<STRepositoryInfo> repoInfo = repositoryManager.getSTRepositoryInfo(repositoryId);

		return repository2TransactionManager.computeIfAbsent(repository,
				(r) -> new RDF4JRepositoryTransactionManager(r, repoInfo));
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

	private static final String AUXILIARY_METADATA_GRAPH_NAME_BASE = "http://semanticturkey.uniroma2.it/";
	private static final String AUXILIARY_METADATA_GRAPH_SUFFIX = "/meta";
	public static final String CORE_REPOSITORY = "core";
	public static final String SUPPORT_REPOSITORY = "support";

	public Resource getMetadataGraph(String extensionPathComponent) {
		return SimpleValueFactory.getInstance().createIRI(AUXILIARY_METADATA_GRAPH_NAME_BASE
				+ extensionPathComponent + AUXILIARY_METADATA_GRAPH_SUFFIX);
	}

	public OntologyManager getNewOntologyManager() {
		return newOntManager;
	}

	public STLocalRepositoryManager getRepositoryManager() {
		return repositoryManager;
	}

	/**
	 * Creates a new repository. The repository can optionally be wrapped in {@link ReadOnlyRepositoryWrapper}
	 * to prevent unintended modifications of the data.
	 * 
	 * @param repositoryAccess
	 *            if <code>null</code> the default repository location associated with the project is used
	 * @param repositoryId
	 *            if <code>null</code> when accessing a remote repository, the default value is
	 *            <code>projectName-localRepositoryId</code>
	 * @param repoConfigurerSpecification
	 * @param localRepostoryId
	 * @param readOnlyWrapper
	 * @param backendType
	 * @param customizeSearch
	 * @return
	 * @throws RepositoryCreationException
	 */
	public Repository createRepository(@Nullable RepositoryAccess repositoryAccess,
			@Nullable String repositoryId, PluginSpecification repoConfigurerSpecification,
			String localRepostoryId, boolean readOnlyWrapper, @Nullable String backendType,
			boolean customizeSearch) throws RepositoryCreationException {

		RepositoryImplConfig localRepositoryImplConfig;

		if (repositoryAccess == null) {
			repositoryAccess = getDefaultRepositoryLocation().toRepositoryAccess();
		}

		if (repositoryId != null) {
			if (repositoryAccess.isLocal()) {
				throw new IllegalArgumentException("Cannot specify the identifier of a local repository");
			}
		} else {
			if (repositoryAccess.isRemote()) {
				repositoryId = getName() + "-" + localRepostoryId;
			}
		}

		// TODO: not an atomic check
		if (repositoryManager.hasRepositoryConfig(localRepostoryId)) {
			throw new RepositoryCreationException("Local repository alread existing:" + localRepostoryId);
		}

		try {
			if (repositoryAccess.isRemote()) {
				if (repositoryId == null) {
					throw new IllegalArgumentException("The name of a remote repository must be non-null");
				}

				RemoteRepositoryAccess repositoryAccess2 = (RemoteRepositoryAccess) repositoryAccess;

				RepositoryManager remoteRepositoryManager = RemoteRepositoryManager.getInstance(
						repositoryAccess2.getServerURL().toString(), repositoryAccess2.getUsername(),
						repositoryAccess2.getPassword());

				if (repositoryAccess instanceof CreateRemote) { // Create remote
					RepositoryImplConfigurer repoConfigurer = (RepositoryImplConfigurer) repoConfigurerSpecification
							.instatiatePlugin();
					RepositoryImplConfig remoteRepositoryImplConfig = repoConfigurer
							.buildRepositoryImplConfig(null);
					if (backendType == null) {
						/* Creating a remote repository: try to detect the backend type */
						backendType = STLocalRepositoryManager.detectBackendType(remoteRepositoryImplConfig);
					}

					RepositoryConfig remoteRepositoryConfig = new RepositoryConfig(repositoryId, "",
							remoteRepositoryImplConfig);

					// TODO: this check is not atomic!
					if (remoteRepositoryManager.hasRepositoryConfig(repositoryId)) {
						throw new RepositoryCreationException(
								"Remote repository already exists: " + repositoryId);
					}

					remoteRepositoryManager.addRepositoryConfig(remoteRepositoryConfig);
				} else { // Access remote
					if (!remoteRepositoryManager.hasRepositoryConfig(repositoryId)) {
						throw new RepositoryCreationException(
								"Remote repository does not exist: " + repositoryId);
					}
				}

				HTTPRepositoryConfig localRepositoryImplConfig2 = new HTTPRepositoryConfig(Protocol
						.getRepositoryLocation(repositoryAccess2.getServerURL().toString(), repositoryId));
				localRepositoryImplConfig2.setUsername(repositoryAccess2.getUsername());
				localRepositoryImplConfig2.setPassword(repositoryAccess2.getPassword());
				localRepositoryImplConfig = localRepositoryImplConfig2;
			} else {
				RepositoryImplConfigurer repoConfigurer = (RepositoryImplConfigurer) repoConfigurerSpecification
						.instatiatePlugin();
				localRepositoryImplConfig = repoConfigurer.buildRepositoryImplConfig(null);
			}

			if (readOnlyWrapper) {
				localRepositoryImplConfig = new ReadOnlyRepositoryWrapperConfig(localRepositoryImplConfig);
			}

			RepositoryConfig localRepositoryConfig = new RepositoryConfig(localRepostoryId, "",
					localRepositoryImplConfig);
			repositoryManager.addRepositoryConfig(localRepositoryConfig, backendType, customizeSearch);
			return repositoryManager.getRepository(localRepostoryId);
		} catch (ClassCastException | ClassNotFoundException | UnsupportedPluginConfigurationException
				| UnloadablePluginConfigurationException | WrongPropertiesException e) {
			throw new RepositoryException(e);
		}
	}

	public Repository createRepository(RepositoryAccess repositoryAccess, String repositoryId,
			PluginSpecification repoConfigurerSpecification, String localRepostoryId)
			throws RepositoryCreationException {
		return createRepository(repositoryAccess, repositoryId, repoConfigurerSpecification, localRepostoryId,
				false, null, false);
	}

	public Repository createReadOnlyRepository(RepositoryAccess repositoryAccess, String repositoryId,
			PluginSpecification repoConfigurerSpecification, String localRepostoryId, String backendType,
			boolean customizeSearch) throws RepositoryCreationException {
		return createRepository(repositoryAccess, repositoryId, repoConfigurerSpecification, localRepostoryId,
				true, backendType, customizeSearch);
	}

	public VersionManager getVersionManager() {
		return versionManager;
	}

	public RepositoryLocation getDefaultRepositoryLocation() {
		return defaultRepositoryLocation;
	}

	/**
	 * Returns the set of {@link RDFResourceRole}s for which version metadata (i.e. creation/modification
	 * date) should be updated. The value {@link RDFResourceRole#undetermined} is used to represent any
	 * "resource", since the latter is not an explicit role.
	 * 
	 * @return
	 */
	public Set<RDFResourceRole> getUpdateForRoles() {
		return Collections.unmodifiableSet(updateForRoles);
	}

}