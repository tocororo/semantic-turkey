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

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositorySchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.sail.config.SailConfigSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.AlreadyExistingRepositoryException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.RepositoryNotExistingException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedModelException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.rendering.RenderingEngine;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.ontology.NSPrefixMappings;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.impl.OntologyManagerImpl;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.RepositorySummary.RemoteRepositorySummary;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.repository.ReadOnlyRepositoryWrapper;
import it.uniroma2.art.semanticturkey.repository.config.ReadOnlyRepositoryWrapperConfig;
import it.uniroma2.art.semanticturkey.search.SearchStrategyUtils;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryTransactionManager;
import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

public abstract class Project extends AbstractProject {

	public static final String RDFS_MODEL_STRING = "http://www.w3.org/2000/01/rdf-schema";
	public static final IRI RDFS_MODEL = SimpleValueFactory.getInstance().createIRI(RDFS_MODEL_STRING);

	public static final String OWL_MODEL_STRING = "http://www.w3.org/2002/07/owl";
	public static final IRI OWL_MODEL = SimpleValueFactory.getInstance().createIRI(OWL_MODEL_STRING);

	public static final String SKOS_MODEL_STRING = "http://www.w3.org/2004/02/skos/core";
	public static final IRI SKOS_MODEL = SimpleValueFactory.getInstance().createIRI(SKOS_MODEL_STRING);

	public static final String ONTOLEXLEMON_MODEL_STRING = "http://www.w3.org/ns/lemon/ontolex";
	public static final IRI ONTOLEXLEMON_MODEL = SimpleValueFactory.getInstance()
			.createIRI(ONTOLEXLEMON_MODEL_STRING);

	public static final String EDOAL_MODEL_STRING = "http://ns.inria.org/edoal/1.0/";
	public static final IRI EDOAL_MODEL = SimpleValueFactory.getInstance().createIRI(EDOAL_MODEL_STRING);

	public static final String RDFS_LEXICALIZATION_MODEL_STRING = "http://www.w3.org/2000/01/rdf-schema";
	public static final IRI RDFS_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI(RDFS_LEXICALIZATION_MODEL_STRING);

	public static final String SKOS_LEXICALIZATION_MODEL_STRING = "http://www.w3.org/2004/02/skos/core";
	public static final IRI SKOS_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI(SKOS_LEXICALIZATION_MODEL_STRING);

	public static final String SKOSXL_LEXICALIZATION_MODEL_STRING = "http://www.w3.org/2008/05/skos-xl";
	public static final IRI SKOSXL_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI(SKOSXL_LEXICALIZATION_MODEL_STRING);

	public static final String ONTOLEXLEMON_LEXICALIZATION_MODEL_STRING = "http://www.w3.org/ns/lemon/ontolex";
	public static final IRI ONTOLEXLEMON_LEXICALIZATION_MODEL = SimpleValueFactory.getInstance()
			.createIRI(ONTOLEXLEMON_LEXICALIZATION_MODEL_STRING);

	protected File infoSTPFile;
	protected File renderingConfigFile;
	protected File uriGenConfigFile;

	protected OntologyManager ontManager;
	protected OntologyManagerImpl supportOntManager;

	protected IRI model;
	protected IRI lexicalizationModel;

	protected Map<String, String> labels; //lang->label (Map grants the uniqueness of label per lang)

	protected String description;

	protected String createdAt;

	public static final String INFOFILENAME = "project.info";

	public static final String URI_GENERATOR_CONFIG_FILENAME = "urigen.cfg";
	public static final String RENDERING_ENGINE_CONFIG_FILENAME = "rendering.cfg";

	public static final String TIMESTAMP_PROP = "timeStamp";
	public static final String CREATED_AT_PROP = "created_at";
	public static final String PROJECT_NAME_PROP = "name";
	public static final String LABELS_PROP = "labels";
	public static final String BASEURI_PROP = "baseURI";
	public static final String DEF_NS_PROP = "defaultNamespace";
	public static final String MODEL_PROP = "model";
	public static final String LEXICALIZATION_MODEL_PROP = "lexicalizationModel";

	public static final String OPEN_AT_STARTUP_PROP = "openAtStartup";

	public static final String HISTORY_ENABLED_PROP = "historyEnabled";
	public static final String VALIDATION_ENABLED_PROP = "validationEnabled";
	public static final String BLACKLISTING_ENABLED_PROP = "blacklistingEnabled";

	public static final String DESCRIPTION_PROP = "description";

	public static final String VERSIONS_PROP = "versions";

	public static final String DEFAULT_REPOSITORY_LOCATION_PROP = "defaultRepositoryLocation";

	// Constants concerning project plugins
	public static final String MANDATORY_PLUGINS_PROP_PREFIX = "plugins.mandatory";

	public static final String URI_GENERATOR_PROP_PREFIX = MANDATORY_PLUGINS_PROP_PREFIX + ".urigen";
	public static final String URI_GENERATOR_FACTORY_ID_PROP = URI_GENERATOR_PROP_PREFIX + ".factoryID";
	public static final String URI_GENERATOR_FACTORY_ID_DEFAULT_PROP_VALUE = "it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGenerator";
	public static final String URI_GENERATOR_CONFIGURATION_TYPE_PROP = URI_GENERATOR_PROP_PREFIX
			+ ".configType";

	public static final String RENDERING_ENGINE_PROP_PREFIX = MANDATORY_PLUGINS_PROP_PREFIX + ".rendering";
	public static final String RENDERING_ENGINE_FACTORY_ID_PROP = RENDERING_ENGINE_PROP_PREFIX + ".factoryID";
	public static final String RENDERING_ENGINE_FACTORY_ID_DEFAULT_PROP_VALUE = "it.uniroma2.art.semanticturkey.extension.impl.rendering.rdfs.RDFSRenderingEngine";
	public static final String RENDERING_ENGINE_CONFIGURATION_TYPE_PROP = RENDERING_ENGINE_PROP_PREFIX
			+ ".configType";

	public static final String SHACL_ENABLED_PROP = "shaclEnabled";
	public static final String TRIVIAL_INFERENCER_ENABLED_PROP = "trivialInferencerEnabled";

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
		reservedProperties.add(HISTORY_ENABLED_PROP);
		reservedProperties.add(VALIDATION_ENABLED_PROP);
		reservedProperties.add(BLACKLISTING_ENABLED_PROP);
		reservedProperties.add(SHACL_ENABLED_PROP);
		reservedProperties.add(CREATED_AT_PROP);
		reservedProperties.add(OPEN_AT_STARTUP_PROP);
	}

	private static final String SEPARATION_SYMBOL = ";";

	protected static Logger logger = LoggerFactory.getLogger(Project.class);

	private Properties stp_properties;
	public static final String stpComment = "properties of local project";
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
	private ExtensionPointManager exptManager;

	/*
	 * Stuff about DCT metadata, deprecated from v8.0 in favor of ResourceMetadata, but still used in the
	 * update routine 7.0 -> 8.0
	 */
	public static final String UPDATE_FOR_ROLES_PROP_DEPRECATED = "updateForRoles";
	public static final String CREATION_DATE_PROP_DEPRECATED = "creationDate";
	public static final String MODIFICATION_DATE_PROP_DEPRECATED = "modificationDate";
	protected Set<RDFResourceRole> updateForRoles;

	/**
	 * this constructor always assumes that the project folder actually exists. Accessing an already existing
	 * folder or creating a new one is in charge of the ProjectManager
	 * <p>
	 * the created project gives access to all of its properties, though it needs to be
	 * {@link #activate(ExtensionPointManager))}d for its RDF content to be accessed
	 * </p>
	 * <p>
	 * implementation of this constructor by subclasses <b><i>must</i></b> take care of properly initializing
	 * the <code>nsPrefixMappingsPersistence</code> field.
	 * 
	 * @param projectName
	 * @param projectDir
	 * @throws ProjectCreationException
	 * @throws UnsupportedModelException
	 */
	public Project(String projectName, File projectDir) throws ProjectCreationException {
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

			checkModels(model, lexicalizationModel);

			//init labels
			String labelsValue = stp_properties.getProperty(LABELS_PROP);
			if (labelsValue != null) {
				ObjectMapper mapper = new ObjectMapper();
				labels = mapper.readValue(labelsValue, new TypeReference<Object>() {});
			} else {
				labels = new HashMap<>();
			}

			// for the update routine 7 -> 8, we are interested in the sole projects with "resource" as
			// updateForRoles
			updateForRoles = new HashSet<>();
			String updateForRolesString = stp_properties.getProperty(UPDATE_FOR_ROLES_PROP_DEPRECATED);
			if (updateForRolesString != null && updateForRolesString.equals("resource")) {
				updateForRoles.add(RDFResourceRole.undetermined);
			}

			acl = new ProjectACL(this);
			versionManager = new VersionManager(this);
			defaultRepositoryLocation = Optional.ofNullable(getProperty(DEFAULT_REPOSITORY_LOCATION_PROP))
					.map(RepositoryLocation::fromString).orElse(null);

			description = Optional.ofNullable(getProperty(DESCRIPTION_PROP)).orElse(null);

			createdAt = Optional.ofNullable(getProperty(CREATED_AT_PROP)).orElse(null);

		} catch (IOException | UnsupportedLexicalizationModelException | UnsupportedModelException
				| ProjectInconsistentException e1) {
			logger.debug("an exception occurred inside the constructor of a corrupted project", e1);
			throw new ProjectCreationException(e1);
		}
	}

	public static void checkModels(IRI model, IRI lexicalizationModel) throws UnsupportedModelException,
			UnsupportedLexicalizationModelException, ProjectInconsistentException {
		checkModel(model);
		checkLexicalizationModel(lexicalizationModel);

		if (model.equals(ONTOLEXLEMON_MODEL)
				&& !lexicalizationModel.equals(ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
			throw new ProjectInconsistentException(
					"When OntoLex Lemon is used as model, it is shall also be used as lexicalization model");
		}
	}

	public static void checkModel(IRI model) throws UnsupportedModelException {
		if (!Arrays.asList(RDFS_MODEL, SKOS_MODEL, OWL_MODEL, ONTOLEXLEMON_MODEL, EDOAL_MODEL)
				.contains(model)) {
			throw new UnsupportedModelException(model.stringValue() + " is not a valid model");
		}
	}

	public static void checkLexicalizationModel(IRI lexicalizationModel)
			throws UnsupportedLexicalizationModelException {
		if (!Arrays.asList(RDFS_LEXICALIZATION_MODEL, SKOS_LEXICALIZATION_MODEL, SKOSXL_LEXICALIZATION_MODEL,
				ONTOLEXLEMON_LEXICALIZATION_MODEL).contains(lexicalizationModel)) {
			throw new UnsupportedLexicalizationModelException(
					lexicalizationModel.stringValue() + " is not a valid lexicalization model");
		}
	}

	void activate(ExtensionPointManager exptManager) throws RDF4JException, ProjectUpdateException {
		this.exptManager = exptManager;
		try {
			repositoryManager = new STLocalRepositoryManager(_projectDir);
			repositoryManager.init();
			Repository supportRepository = repositoryManager.getRepository("support");

			if (supportRepository != null) {
				supportOntManager = new OntologyManagerImpl(supportRepository, false);
			}

			Repository coreRepository = repositoryManager.getRepository("core");
			ontManager = new OntologyManagerImpl(coreRepository, isValidationEnabled());

			String baseURI = getBaseURI();
			if (baseURI == null)
				throw new ProjectInconsistentException("baseURI is not specified");

			// Activation of the URI Generator for this project
			String uriGenFactoryID = getProperty(URI_GENERATOR_FACTORY_ID_PROP);

			if (uriGenFactoryID == null) {
				uriGenFactoryID = URI_GENERATOR_FACTORY_ID_DEFAULT_PROP_VALUE;
			}

			try {
				PluginSpecification uriGenSpec;

				File uriGenConfigFile = new File(_projectDir, URI_GENERATOR_CONFIG_FILENAME);
				if (uriGenConfigFile.exists()) {
					uriGenSpec = new PluginSpecification(uriGenFactoryID, null, null,
							(ObjectNode) STPropertiesManager.createObjectMapper().readTree(uriGenConfigFile));
				} else {
					uriGenSpec = new PluginSpecification(uriGenFactoryID, null, null, null);
				}

				logger.debug("instantiating URIGenerator. Specification = {}", uriGenSpec);

				this.uriGenerator = exptManager.instantiateExtension(URIGenerator.class, uriGenSpec);
			} catch (IOException e) {
				throw new ProjectAccessException(e);
			}

			// Activation of the rendering engine for this project
			String renderingEngineFactoryID = getProperty(RENDERING_ENGINE_FACTORY_ID_PROP);

			if (renderingEngineFactoryID == null) {
				PluginSpecification renderingEngineSpec = BaseRenderingEngine
						.getRenderingEngineSpecificationForLexicalModel(lexicalizationModel)
						.orElseThrow(() -> new IllegalArgumentException(
								"Unsupported lexicalization model: " + lexicalizationModel));
				renderingEngineFactoryID = renderingEngineSpec.getFactoryId();
			}

			try {
				PluginSpecification renderingEngineSpec;

				File renderingEngineConfigFile = new File(_projectDir, RENDERING_ENGINE_CONFIG_FILENAME);
				if (renderingEngineConfigFile.exists()) {
					renderingEngineSpec = new PluginSpecification(renderingEngineFactoryID, null, null,
							(ObjectNode) STPropertiesManager.createObjectMapper().readTree(renderingEngineConfigFile));
				} else {
					renderingEngineSpec = new PluginSpecification(renderingEngineFactoryID, null, null, null);
				}

				logger.debug("instantiating RenderingEngine. Specification = {}", renderingEngineSpec);

				this.renderingEngine = exptManager.instantiateExtension(RenderingEngine.class, renderingEngineSpec);
			} catch (IOException e) {
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

				// nsPrefixMappingsPersistence must have been already created by constructor of Project
				// subclasses
				ontManager.initializeMappingsPersistence(nsPrefixMappingsPersistence);

				loadingCoreVocabularies();

				conn.begin();

				ValidationUtilities.executeWithoutValidation(isValidationEnabled(), conn, (connection) -> {
					// always guarantee that there is an owl:Ontology named after the base URI
					IRI baseURIasIRI = conn.getValueFactory().createIRI(baseURI);

					if (!conn.hasStatement(baseURIasIRI, RDF.TYPE, OWL.ONTOLOGY, false, baseURIasIRI)) {
						conn.add(baseURIasIRI, RDF.TYPE, OWL.ONTOLOGY, baseURIasIRI);
					}
				});

				conn.commit();

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

	protected void loadingCoreVocabularies() throws Exception {
		try (RepositoryConnection conn = ontManager.getRepository().getConnection()) {
			conn.begin();

			Set<Resource> contexts = QueryResults.stream(conn.getContextIDs()).filter(IRI.class::isInstance)
					.map(r -> OntologyManager.computeCanonicalURI((IRI) r)).collect(toSet());

			Map<IRI, ImmutablePair<URL, RDFFormat>> coreVocabularies = new HashMap<>();

			coreVocabularies.putAll(getCoreVocabulariesForLexicalizationModel());
			coreVocabularies.putAll(getCoreVocabulariesForModel());

			MutableBoolean anyWritten = new MutableBoolean(false);

			ValidationUtilities.executeWithoutValidation(isValidationEnabled(), conn, (connection) -> {
				logger.debug("Loading core vocabularies");

				for (Map.Entry<IRI, ImmutablePair<URL, RDFFormat>> entry : coreVocabularies.entrySet()) {
					IRI coreOnt = entry.getKey();
					URL coreOntDocumentURL = entry.getValue().getKey();
					RDFFormat coreOntDocumentFormat = entry.getValue().getValue();

					logger.debug("Declaring core vocabulary: " + coreOnt);
					ontManager.declareSupportOntology(coreOnt, true, false, false);

					if (!contexts.contains(OntologyManager.computeCanonicalURI(coreOnt))) {
						logger.debug("Loading vocabulary: " + coreOnt);
						conn.add(coreOntDocumentURL, coreOnt.stringValue(), coreOntDocumentFormat, coreOnt);
						anyWritten.setValue(true);
					}

				}
			});

			logger.debug("About to commit the loaded triples");
			conn.commit();

			// If anything has been written, assumes a newly created project, so initialize search
			if (anyWritten.isTrue()) {
				conn.begin();
				SearchStrategy searchStrategy = SearchStrategyUtils.instantiateSearchStrategy(exptManager,
						STRepositoryInfoUtils
								.getSearchStrategy(getRepositoryManager().getSTRepositoryInfo("core")));
				ValidationUtilities.executeWithoutValidation(isValidationEnabled(), conn, (conn2) -> {
					searchStrategy.initialize(conn);
				});
				conn.commit();
			}

			IRI skosxlOnt = OntologyManager
					.computeCanonicalURI(SimpleValueFactory.getInstance().createIRI(SKOSXL.NAMESPACE));
			IRI limeOnt = OntologyManager
					.computeCanonicalURI(SimpleValueFactory.getInstance().createIRI(LIME.NAMESPACE));
			IRI decompOnt = OntologyManager
					.computeCanonicalURI(SimpleValueFactory.getInstance().createIRI(DECOMP.NAMESPACE));

			coreVocabularies.entrySet().stream().map(Map.Entry::getKey)
					.map(OntologyManager::computeCanonicalURI).forEach(ont -> {
						if (Objects.equals(ont, skosxlOnt)) {
							conn.setNamespace("skosxl", SKOSXL.NAMESPACE);
						}

						if (Objects.equals(ont, limeOnt)) {
							conn.setNamespace("lime", LIME.NAMESPACE);
						}

						if (Objects.equals(ont, decompOnt)) {
							conn.setNamespace("decomp", DECOMP.NAMESPACE);
						}
					});

			logger.debug("Core vocabularies loaded");
		}
	}

	protected Map<IRI, ImmutablePair<URL, RDFFormat>> getCoreVocabulariesForLexicalizationModel() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		Map<IRI, ImmutablePair<URL, RDFFormat>> coreVocabularies = new HashMap<>();

		// Note: deliberate cascade from OntoLex-Lemon model to RDFS lexicalization model
		switch (getLexicalizationModel().stringValue()) {
		case ONTOLEXLEMON_LEXICALIZATION_MODEL_STRING:
			coreVocabularies.put(vf.createIRI("http://www.w3.org/ns/lemon/ontolex"),
					ImmutablePair.of(ONTOLEX.class.getResource("ontolex.rdf"), RDFFormat.RDFXML));
			coreVocabularies.put(vf.createIRI("http://www.w3.org/ns/lemon/decomp"),
					ImmutablePair.of(ONTOLEX.class.getResource("decomp.rdf"), RDFFormat.RDFXML));
			coreVocabularies.put(vf.createIRI("http://www.w3.org/ns/lemon/synsem"),
					ImmutablePair.of(ONTOLEX.class.getResource("synsem.rdf"), RDFFormat.RDFXML));
			coreVocabularies.put(vf.createIRI("http://www.w3.org/ns/lemon/vartrans"),
					ImmutablePair.of(ONTOLEX.class.getResource("vartrans.rdf"), RDFFormat.RDFXML));
			coreVocabularies.put(vf.createIRI("http://www.w3.org/ns/lemon/lime"),
					ImmutablePair.of(ONTOLEX.class.getResource("lime.rdf"), RDFFormat.RDFXML));
			coreVocabularies.put(vf.createIRI("http://purl.org/dc/terms/"),
					ImmutablePair.of(OntologyManager.class.getResource("dcterms.ttl"), RDFFormat.TURTLE));
			coreVocabularies.put(vf.createIRI("http://www.lexinfo.net/ontology/3.0/lexinfo"),
					ImmutablePair.of(OntologyManager.class.getResource("lexinfo.owl"), RDFFormat.RDFXML));
		case SKOSXL_LEXICALIZATION_MODEL_STRING:
			coreVocabularies.put(vf.createIRI("http://www.w3.org/2008/05/skos-xl"),
					ImmutablePair.of(OntologyManager.class.getResource("skos-xl.rdf"), RDFFormat.RDFXML));
		case SKOS_LEXICALIZATION_MODEL_STRING:
			coreVocabularies.put(vf.createIRI("http://www.w3.org/2004/02/skos/core"),
					ImmutablePair.of(OntologyManager.class.getResource("skos.rdf"), RDFFormat.RDFXML));
		case RDFS_LEXICALIZATION_MODEL_STRING:
			coreVocabularies.put(vf.createIRI("http://www.w3.org/2000/01/rdf-schema"),
					ImmutablePair.of(OntologyManager.class.getResource("rdf-schema.rdf"), RDFFormat.RDFXML));
		}

		return coreVocabularies;
	}

	protected Map<IRI, ImmutablePair<URL, RDFFormat>> getCoreVocabulariesForModel() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		Map<IRI, ImmutablePair<URL, RDFFormat>> coreVocabularies = new HashMap<>();
		coreVocabularies.put(vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns"),
				ImmutablePair.of(OntologyManager.class.getResource("rdf.rdf"), RDFFormat.RDFXML));
		coreVocabularies.put(vf.createIRI("http://www.w3.org/2000/01/rdf-schema"),
				ImmutablePair.of(OntologyManager.class.getResource("rdf-schema.rdf"), RDFFormat.RDFXML));
		coreVocabularies.put(vf.createIRI("http://www.w3.org/2002/07/owl"),
				ImmutablePair.of(OntologyManager.class.getResource("owl.rdf"), RDFFormat.RDFXML));

		// Note: deliberate cascade from OntoLex-Lemon model to SKOS model
		switch (getModel().stringValue()) {
		case ONTOLEXLEMON_LEXICALIZATION_MODEL_STRING:
			coreVocabularies.put(vf.createIRI("http://www.w3.org/ns/lemon/ontolex"),
					ImmutablePair.of(ONTOLEX.class.getResource("ontolex.rdf"), RDFFormat.RDFXML));
		case SKOS_MODEL_STRING:
			coreVocabularies.put(vf.createIRI("http://www.w3.org/2004/02/skos/core"),
					ImmutablePair.of(OntologyManager.class.getResource("skos.rdf"), RDFFormat.RDFXML));
		}

		return coreVocabularies;
	}

	public void deactivate() {
		repositoryManager.shutDown();
	}

	protected abstract void loadTriples() throws RDF4JException;

	public void clearData() throws Exception {
		// clear core repository
		ontManager.clearData();
		// reload core vocabularies
		loadingCoreVocabularies();

		// if support repository is enabled, clear it as well
		if (supportOntManager != null) {
			supportOntManager.clearData();
		}

		// clear version records
		versionManager.clearVersionRecords();

		@Nullable
		Exception ex = null;

		// delete all repositories but the core and support ones
		for (RepositoryInfo repInfo : repositoryManager.getAllRepositoryInfos(true)) {

			if (Objects.equals(repInfo.getId(), Project.CORE_REPOSITORY)
					|| Objects.equals(repInfo.getId(), Project.SUPPORT_REPOSITORY))
				continue;

			try {
				// delete the local handle of the repository and, if any, its remote counterpart too
				repositoryManager.removeRepository(repInfo.getId(), true);
			} catch (Exception e) {
				if (ex == null) {
					ex = new ProjectUpdateException("Errors occurred when deleting repositories");
					ex.addSuppressed(
							new RepositoryException("Error when deleting repository " + repInfo.getId(), e));
				}
			}
		}

		if (ex != null) {
			throw ex;
		}
	}

	private void updateProjectProperties() throws IOException {
		FileOutputStream os = new FileOutputStream(infoSTPFile);
		// properties.storeToXML(os, "local cache references for mirroring remote ontologies");
		stp_properties.store(os, stpComment);
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

	public Map<String, String> getLabels() {
		return labels;
	}

	public String getBaseURI() {
		return stp_properties.getProperty(BASEURI_PROP);
	}

	public String getDefaultNamespace() {
		return stp_properties.getProperty(DEF_NS_PROP);
	}

	public boolean isHistoryEnabled() {
		return Boolean.parseBoolean(getProperty(HISTORY_ENABLED_PROP));
	}

	public boolean isValidationEnabled() {
		return Boolean.parseBoolean(getProperty(VALIDATION_ENABLED_PROP));
	}

	public boolean isBlacklistingEnabled() {
		return Boolean
				.parseBoolean(ObjectUtils.firstNonNull(getProperty(BLACKLISTING_ENABLED_PROP), "false"));
	}

	public boolean isSHACLEnabled() {
		return Boolean.parseBoolean(getProperty(SHACL_ENABLED_PROP));
	}

	public boolean isTrivialInferencerEnabled() {
		return Boolean.parseBoolean(getProperty(TRIVIAL_INFERENCER_ENABLED_PROP));
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
		if (reservedProperties.contains(propName)) {
			throw new ReservedPropertyUpdateException(propName);
		}

		setReservedProperty(propName, propValue);
	}

	public void setReservedProperty(String propName, String propValue) throws ProjectUpdateException {
		String oldValue = stp_properties.getProperty(propName);
		try {
			stp_properties.setProperty(propName, propValue);
			updateProjectProperties();
		} catch (IOException e) {
			stp_properties.setProperty(propName, oldValue);
			throw new ProjectUpdateException(e);
		}
	}

	public void deleteReservedProperty(String propName) throws ProjectUpdateException {
		String oldValue = stp_properties.getProperty(propName);
		try {
			stp_properties.remove(propName);
			updateProjectProperties();
		} catch (IOException e) {
			stp_properties.setProperty(propName, oldValue);
			throw new ProjectUpdateException(e);
		}
	}

	public void removeProperty(String propName, String propValue)
			throws ReservedPropertyUpdateException, ProjectUpdateException {
		logger.debug("removing property: " + propName);
		if (reservedProperties.contains(propName))
			throw new ReservedPropertyUpdateException(propName);

		String oldValue = stp_properties.getProperty(propName);
		try {
			stp_properties.remove(propName);
			updateProjectProperties();
		} catch (IOException e) {
			stp_properties.setProperty(propName, oldValue);
			throw new ProjectUpdateException(e);
		}
	}

	public void setBaseURI(String baseURI) throws ProjectUpdateException {
		try {
			// getOntModel().setBaseURI(baseURI);
			ontManager.setBaseURI(baseURI);
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

	/**
	 *
	 * @param lang
	 * @param label if null removes the label in the given language
	 * @throws ProjectUpdateException
	 */
	public void setLabel(String lang, @Nullable String label) throws ProjectUpdateException {
		try {
			if (label == null) {
				labels.remove(lang);
			} else {
				labels.put(lang, label);
			}
			ObjectMapper mapper = new ObjectMapper();
			stp_properties.setProperty(LABELS_PROP, mapper.writeValueAsString(labels));
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
	 * Returns the core repository associated with this project. Clients should rarely invoke this method, and
	 * use instead the operations found in {@link STServiceContextUtils} (which are aware, for example, of
	 * version dumps).
	 * 
	 * @return
	 */
	public Repository getRepository() {
		return ontManager.getRepository();
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
	public static final String LEFT_DATASET_PROP = "leftDataset";
	public static final String RIGHT_DATASET_PROP = "rightDataset";

	public Resource getMetadataGraph(String extensionPathComponent) {
		return SimpleValueFactory.getInstance().createIRI(AUXILIARY_METADATA_GRAPH_NAME_BASE
				+ extensionPathComponent + AUXILIARY_METADATA_GRAPH_SUFFIX);
	}

	public OntologyManager getOntologyManager() {
		return ontManager;
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
	 * @throws AlreadyExistingRepositoryException
	 * @throws RepositoryNotExistingException
	 */
	public Repository createRepository(@Nullable RepositoryAccess repositoryAccess,
			@Nullable String repositoryId, @Nullable PluginSpecification repoConfigurerSpecification,
			String localRepostoryId, boolean readOnlyWrapper, @Nullable String backendType,
			boolean customizeSearch)
			throws AlreadyExistingRepositoryException, RepositoryNotExistingException {

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
			throw new AlreadyExistingRepositoryException(
					"Local repository already existing:" + localRepostoryId);
		}

		try {
			MutableObject<PluginSpecification> guessedPluginSpec = new MutableObject<>();
			repositoryManager.operateOnUnfoldedManager(CORE_REPOSITORY, (actualRepoMgr, actualRepoId) -> {
				Model repoConfig = actualRepoMgr.getRepositoryConfig(actualRepoId);
				Resource sailImpl = Models
						.objectResource(repoConfig.filter(null, SailRepositorySchema.SAILIMPL, null))
						.orElseThrow(() -> new IllegalStateException("Unable to determine top Sail"));
				Optional<Resource> delegate;
				do {
					delegate = Models.getPropertyResource(repoConfig, sailImpl, SailConfigSchema.DELEGATE);
					sailImpl = delegate.orElse(sailImpl);
				} while (delegate.isPresent());

				String sailType = Models.getProperty(repoConfig, sailImpl, SailConfigSchema.SAILTYPE)
						.orElseThrow(() -> new IllegalStateException("Unable to determine sail type"))
						.stringValue();

				ObjectNode configNode = JsonNodeFactory.instance.objectNode();
				if (sailType.startsWith("openrdf:") || sailType.startsWith("rdf4j:")) { // rdf4j backend ->
																						// NativeStore
					configNode.put("@type",
							"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfiguration");
				} else if (sailType.startsWith("graphdb:")) { // GDB Free
					configNode.put("@type",
							"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.GraphDBFreeConfiguration");
				} else if (sailType.startsWith("owlim:")) { // GDB SE
					configNode.put("@type",
							"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.GraphDBSEConfiguration");
				} else {
					throw new IllegalStateException(
							"Unable to determine the technology stack from the bottom sail-type: "
									+ sailType);
				}
				guessedPluginSpec.setValue(new PluginSpecification(
						"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryConfigurer",
						null, null, configNode));
			});
			if (repositoryAccess.isRemote()) {
				if (repositoryId == null) {
					throw new IllegalArgumentException("The name of a remote repository must be non-null");
				}

				RemoteRepositoryAccess repositoryAccess2 = (RemoteRepositoryAccess) repositoryAccess;

				RepositoryManager remoteRepositoryManager = RemoteRepositoryManager.getInstance(
						repositoryAccess2.getServerURL().toString(), repositoryAccess2.getUsername(),
						repositoryAccess2.getPassword());

				if (repositoryAccess instanceof CreateRemote) { // Create remote
					RepositoryImplConfigurer repoConfigurer = exptManager
							.instantiateExtension(RepositoryImplConfigurer.class, MoreObjects
									.firstNonNull(repoConfigurerSpecification, guessedPluginSpec.getValue()));
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
						throw new AlreadyExistingRepositoryException(
								"Remote repository already exists: " + repositoryId);
					}

					remoteRepositoryManager.addRepositoryConfig(remoteRepositoryConfig);
				} else { // Access remote
					if (!remoteRepositoryManager.hasRepositoryConfig(repositoryId)) {
						throw new RepositoryNotExistingException(
								"Remote repository does not exist: " + repositoryId);
					}
				}

				HTTPRepositoryConfig localRepositoryImplConfig2 = new HTTPRepositoryConfig(Protocol
						.getRepositoryLocation(repositoryAccess2.getServerURL().toString(), repositoryId));
				localRepositoryImplConfig2.setUsername(repositoryAccess2.getUsername());
				localRepositoryImplConfig2.setPassword(repositoryAccess2.getPassword());
				localRepositoryImplConfig = localRepositoryImplConfig2;
			} else {
				RepositoryImplConfigurer repoConfigurer = exptManager.instantiateExtension(
						RepositoryImplConfigurer.class,
						MoreObjects.firstNonNull(repoConfigurerSpecification, guessedPluginSpec.getValue()));
				localRepositoryImplConfig = repoConfigurer.buildRepositoryImplConfig(null);
			}

			if (readOnlyWrapper) {
				localRepositoryImplConfig = new ReadOnlyRepositoryWrapperConfig(localRepositoryImplConfig);
			}

			RepositoryConfig localRepositoryConfig = new RepositoryConfig(localRepostoryId, "",
					localRepositoryImplConfig);
			repositoryManager.addRepositoryConfig(localRepositoryConfig, backendType, customizeSearch);
			return repositoryManager.getRepository(localRepostoryId);
		} catch (ClassCastException | WrongPropertiesException | IllegalArgumentException
				| NoSuchExtensionException | STPropertyAccessException | InvalidConfigurationException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Deletes a repository.
	 * 
	 * @param repositoryId
	 *            the (local) identifier of the repository to delete
	 * @param propagateDelete
	 *            tells whether to delete the remote repository (if any).
	 */
	public void deleteRepository(String repositoryId, boolean propagateDelete) {
		if (Sets.newHashSet("core", "support").contains(repositoryId)) {
			throw new IllegalArgumentException(
					"It is not allowed to delete a repository with this name: " + repositoryId);
		}
		getRepositoryManager().removeRepository(repositoryId, propagateDelete);
	}

	public Repository createRepository(RepositoryAccess repositoryAccess, String repositoryId,
			PluginSpecification repoConfigurerSpecification, String localRepostoryId)
			throws AlreadyExistingRepositoryException, RepositoryNotExistingException {
		return createRepository(repositoryAccess, repositoryId, repoConfigurerSpecification, localRepostoryId,
				false, null, false);
	}

	public Repository createReadOnlyRepository(RepositoryAccess repositoryAccess, String repositoryId,
			PluginSpecification repoConfigurerSpecification, String localRepostoryId, String backendType,
			boolean customizeSearch)
			throws AlreadyExistingRepositoryException, RepositoryNotExistingException {
		return createRepository(repositoryAccess, repositoryId, repoConfigurerSpecification, localRepostoryId,
				true, backendType, customizeSearch);
	}

	public VersionManager getVersionManager() {
		return versionManager;
	}

	public RepositoryLocation getDefaultRepositoryLocation() {
		return defaultRepositoryLocation;
	}

	public Set<RDFResourceRole> getUpdateForRoles() {
		return Collections.unmodifiableSet(updateForRoles);
	}

	/**
	 * Returns the directory associated with this project
	 * 
	 * @return
	 */
	public File getProjectDirectory() {
		return _projectDir;
	}

	public static void checkProjectName(String projectName) throws InvalidProjectNameException {
		ProjectManager.logger.debug("checking if name: " + projectName + " is a valid project name");
		if (projectName == null) {
			throw new InvalidProjectNameException("Project name may not be null", null);
		}

		if (ProjectConsumer.SYSTEM.getName().equalsIgnoreCase(projectName)) {
			throw new InvalidProjectNameException("Project name may not be equal (ignoring case) to SYSTEM",
					projectName);
		}

		if (projectName.matches(".*[:\\\\/*?\"<>|].*")) {
			throw new InvalidProjectNameException("Project name may not contain the characters \\/*?\"<>|",
					projectName);
		}
		ProjectManager.logger.debug("name is valid");
	}

	public String getDescription() {
		return description;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public boolean isOpenAtStartupEnabled() {
		return Boolean.parseBoolean(ObjectUtils.firstNonNull(getProperty(OPEN_AT_STARTUP_PROP), "false"));
	}

	public static Collection<RepositorySummary> getRepositorySummaries(STLocalRepositoryManager repoManager,
			boolean excludeLocal) {
		Collection<RepositoryInfo> repos = repoManager.getAllUserRepositoryInfos();
		Collection<RepositorySummary> summaries = new ArrayList<>();

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
			summaries.add(repSummary);
		}
		return summaries;
	}

}