package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.dynamic.DynamicSTProperties;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Closer;

import it.uniroma2.art.lime.model.repo.LIMERepositoryConnectionWrapper;
import it.uniroma2.art.lime.profiler.LIMEProfiler;
import it.uniroma2.art.lime.profiler.ProfilerException;
import it.uniroma2.art.maple.orchestration.AssessmentException;
import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectDeletionException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedModelException;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetCatalogConnector;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DownloadDescription;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.CorruptedProject;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectInfo;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.project.ProjectStatus;
import it.uniroma2.art.semanticturkey.project.ProjectStatus.Status;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.project.RepositoryLocation;
import it.uniroma2.art.semanticturkey.project.RepositorySummary;
import it.uniroma2.art.semanticturkey.project.SHACLSettings;
import it.uniroma2.art.semanticturkey.project.STLocalRepositoryManager;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.properties.dynamic.STPropertiesSchema;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.projects.PreloadedDataStore;
import it.uniroma2.art.semanticturkey.services.core.projects.PreloadedDataSummary;
import it.uniroma2.art.semanticturkey.services.core.projects.ProjectPropertyInfo;
import it.uniroma2.art.semanticturkey.settings.facets.CorruptedProjectFacets;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacets;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacetsSchemaStore;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacetsStore;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;

//import it.uniroma2.art.semanticturkey.changetracking.ChangeTrackerNotDetectedException;
//import it.uniroma2.art.semanticturkey.changetracking.ChangeTrackerParameterMismatchException;

@STService
public class Projects extends STServiceAdapter {

	private final int MAX_RESULT_QUERY_FACETS = 500;

	private static Logger logger = LoggerFactory.getLogger(Projects.class);

	private final String indexMainDir = "index";

	private final String lucDirName = "facetsIndex";


	private final String PROJECT_NAME = "prjName";
	private final String PROJECT_MODEL = "prjModel";
	private final String PROJECT_LEX_MODEL = "prjLexModel";
	private final String PROJECT_HISTORY = "prjHistoryEnabled";
	private final String PROJECT_VALIDATION = "prjValidationEnabled";
	private final String PROJECT_DESCRIPTION = "prjDescription";

	@Autowired
	private MediationFramework mediationFramework;

	@Autowired
	private PreloadedDataStore preloadedDataStore;

	// TODO understand how to specify remote repository / different sail configurations
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void createProject(ProjectConsumer consumer, String projectName, IRI model,
			IRI lexicalizationModel, String baseURI, boolean historyEnabled, boolean validationEnabled,
			@Optional(defaultValue = "false") boolean blacklistingEnabled, RepositoryAccess repositoryAccess,
			String coreRepoID,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurer\", \"configuration\" : {\"@type\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfigurerConfiguration\"}}") PluginSpecification coreRepoSailConfigurerSpecification,
			@Optional String coreBackendType, String supportRepoID,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurer\", \"configuration\" : {\"@type\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfigurerConfiguration\"}}") PluginSpecification supportRepoSailConfigurerSpecification,
			@Optional String supportBackendType,
			@Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory\"}") PluginSpecification uriGeneratorSpecification,
			@Optional PluginSpecification renderingEngineSpecification,
			@Optional @JsonSerialized List<Pair<RDFResourceRole, String>> resourceMetadataAssociations,
			@Optional String preloadedDataFileName, @Optional RDFFormat preloadedDataFormat,
			@Optional TransitiveImportMethodAllowance transitiveImportAllowance, @Optional String leftDataset,
			@Optional String rightDataset, @Optional boolean shaclEnabled,
			@Optional @JsonSerialized SHACLSettings shaclSettings, @Optional boolean trivialInferenceEnabled)
			throws ProjectInconsistentException, InvalidProjectNameException, ProjectInexistentException,
			ProjectAccessException, ForbiddenProjectAccessException, DuplicatedResourceException,
			ProjectCreationException, ClassNotFoundException, WrongPropertiesException,
			UnsupportedPluginConfigurationException, UnloadablePluginConfigurationException, RBACException,
			UnsupportedModelException, UnsupportedLexicalizationModelException, InvalidConfigurationException,
			STPropertyAccessException, IOException, ReservedPropertyUpdateException, ProjectUpdateException,
			STPropertyUpdateException, NoSuchConfigurationManager, PropertyNotFoundException {

		List<Object> preloadRelatedArgs = Arrays.asList(preloadedDataFileName, preloadedDataFormat,
				transitiveImportAllowance);
		if (!preloadRelatedArgs.stream().allMatch(java.util.Objects::nonNull)
				&& !preloadRelatedArgs.stream().noneMatch(java.util.Objects::nonNull)) {
			throw new IllegalArgumentException(
					"All preload-related arguments must be specified together, or none of them can be specified");
		}

		// If no rendering engine has been configured, guess the best one based on the model type
		if (renderingEngineSpecification == null) {
			String renderingEngineFactoryID = Project.determineBestRenderingEngine(lexicalizationModel);
			renderingEngineSpecification = new PluginSpecification(renderingEngineFactoryID, null,
					new Properties(), null);
		}

		if (shaclSettings != null) {
			if (!shaclEnabled) {
				throw new IllegalArgumentException(
						"It is not allowed to specify SHACL-related settings if SHACL is not enabled");
			}

			if (!repositoryAccess.isCreation()) {
				throw new IllegalArgumentException(
						"It is not allowed to sprecify SHACL-related settings if accessing an existing repository");
			}
		}

		uriGeneratorSpecification.expandDefaults();
		renderingEngineSpecification.expandDefaults();

		Set<IRI> failedImports = new HashSet<>();

		File preloadedDataFile = preloadedDataFileName != null
				? preloadedDataStore.startConsumingPreloadedData(preloadedDataFileName)
				: null;
		boolean deletePreloadedDataFile = false;
		try {
			ProjectManager.createProject(consumer, projectName, model, lexicalizationModel, baseURI.trim(),
					historyEnabled, validationEnabled, blacklistingEnabled, repositoryAccess, coreRepoID,
					coreRepoSailConfigurerSpecification, coreBackendType, supportRepoID,
					supportRepoSailConfigurerSpecification, supportBackendType, uriGeneratorSpecification,
					renderingEngineSpecification, resourceMetadataAssociations, preloadedDataFile,
					preloadedDataFormat, transitiveImportAllowance, failedImports, leftDataset, rightDataset,
					shaclEnabled, shaclSettings, trivialInferenceEnabled);
			deletePreloadedDataFile = true;
		} finally {
			if (preloadedDataFileName != null) {
				preloadedDataStore.finishConsumingPreloadedData(preloadedDataFileName,
						deletePreloadedDataFile);
			}
		}
		//create the index about the facets of this project
		recreateFacetIndexForProjectAPI(projectName);
	}

	/**
	 * Returns an empty form for SHACL settings upon project creation.
	 * 
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'C')")
	public SHACLSettings createEmptySHACLSettingsForm() {
		return new SHACLSettings();
	}

	/**
	 * 
	 * @param consumer
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @param userDependent
	 *            if true, returns only the projects accessible by the logged user (the user has a role
	 *            assigned in it)
	 * @param onlyOpen
	 *            if true, return only the open projects
	 * @return
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	public List<ProjectInfo> listProjects(@Optional(defaultValue = "SYSTEM") ProjectConsumer consumer,
			@Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel,
			@Optional(defaultValue = "false") boolean userDependent,
			@Optional(defaultValue = "false") boolean onlyOpen) throws ProjectAccessException, PropertyNotFoundException,
			ProjectInexistentException, IOException, InvalidProjectNameException {

		logger.debug("listProjects, asked by consumer: " + consumer);

		List<ProjectInfo> listProjInfo = new ArrayList<>();

		Collection<AbstractProject> projects = ProjectManager.listProjects(consumer);

		for (AbstractProject absProj : projects) {
			ProjectInfo projInfo = getProjectInfoHelper(consumer, requestedAccessLevel, requestedLockLevel,
					userDependent, onlyOpen, absProj);
			if (projInfo != null) {
				listProjInfo.add(projInfo);
			}
		}

		//check if the lucene dir (for the facets) exists, if not, create the indexes
		if(!isLuceneDirPresent()){
			//create the indexes
			createFacetIndexAPI();
		}

		return listProjInfo;
	}

	/**
	 * Returns the projects where there is at least a user with the given role
	 * 
	 * @param consumer
	 * @param role
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @param onlyOpen
	 * @return
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	public List<ProjectInfo> listProjectsPerRole(@Optional(defaultValue = "SYSTEM") ProjectConsumer consumer,
			String role, @Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel,
			@Optional(defaultValue = "false") boolean onlyOpen) throws ProjectAccessException {
		List<ProjectInfo> listProjInfo = new ArrayList<>();

		for (AbstractProject absProj : ProjectManager.listProjects(consumer)) {
			ProjectInfo projInfo = getProjectInfoHelper(consumer, requestedAccessLevel, requestedLockLevel,
					false, onlyOpen, absProj);
			if (projInfo != null) {
				Collection<ProjectUserBinding> puBindings = ProjectUserBindingsManager
						.listPUBindingsOfProject(absProj);
				for (ProjectUserBinding pub : puBindings) { // looks into the bindings if there is at least
															// one with the given role
					if (pub.getRoles().stream().anyMatch(r -> r.getName().equals(role))) { // the PU binding
																							// has the given
																							// role
						listProjInfo.add(projInfo);
						break; // project added, no need to look for other PUBindings
					}
				}
			}
		}
		return listProjInfo;
	}

	/**
	 * Returns information
	 * 
	 * @param consumer
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @param projectName
	 * @return
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation
	public ProjectInfo getProjectInfo(@Optional(defaultValue = "SYSTEM") ProjectConsumer consumer,
			@Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
			@Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel, String projectName)
			throws IllegalStateException, ProjectAccessException, InvalidProjectNameException,
			ProjectInexistentException {
		Project proj = ProjectManager.getProject(projectName, true);

		return getProjectInfoHelper(consumer, requestedAccessLevel, requestedLockLevel, false, false, proj);
	}

	/**
	 * Returns metadata about a project. If either <code>userDependent</code> or <code>onlyOpen</code> is
	 * <code>true</code>, then this operation might return <code>null</code>.
	 * 
	 * @param consumer
	 * @param requestedAccessLevel
	 * @param requestedLockLevel
	 * @param userDependent
	 *            if true, returns only the projects accessible by the logged user (the user has a role
	 *            assigned in it)
	 * @param onlyOpen
	 *            if true, return only the open projects
	 * @param absProj
	 * @return
	 * @throws ProjectAccessException
	 */
	public ProjectInfo getProjectInfoHelper(ProjectConsumer consumer,
			ProjectACL.AccessLevel requestedAccessLevel, ProjectACL.LockLevel requestedLockLevel,
			boolean userDependent, boolean onlyOpen, AbstractProject absProj) {
		String name = absProj.getName();
		String baseURI = null;
		String defaultNamespace = null;
		String model = null;
		String lexicalizationModel = null;
		Map<String, String> facets = null;
		boolean historyEnabled = false;
		boolean validationEnabled = false;
		boolean shaclEnabled = false;
		boolean open = false;
		AccessResponse access = null;
		RepositoryLocation repoLocation = new RepositoryLocation(null);
		ProjectStatus status = new ProjectStatus(Status.ok);
		String description = null;
		ProjectFacets facets2 = null;

		if (absProj instanceof Project) {
			Project proj = (Project) absProj;

			baseURI = proj.getBaseURI();
			defaultNamespace = proj.getDefaultNamespace();
			model = proj.getModel().stringValue();
			lexicalizationModel = proj.getLexicalizationModel().stringValue();
			facets = proj.getFacets();
			historyEnabled = proj.isHistoryEnabled();
			validationEnabled = proj.isValidationEnabled();
			shaclEnabled = proj.isSHACLEnabled();
			open = ProjectManager.isOpen(proj);
			access = ProjectManager.checkAccessibility(consumer, proj, requestedAccessLevel,
					requestedLockLevel);
			repoLocation = proj.getDefaultRepositoryLocation();
			description = proj.getDescription();

			if (onlyOpen && !open) {
				return null;
			}
			if (userDependent && !ProjectUserBindingsManager
					.hasUserAccessToProject(UsersManager.getLoggedUser(), proj)) {
				return null;
			}
			try {
				facets2 = (ProjectFacets) exptManager.getSettings(proj, UsersManager.getLoggedUser(),
						ProjectFacetsStore.class.getName(), Scope.PROJECT);
			} catch (IllegalStateException | STPropertyAccessException | NoSuchSettingsManager e) {
				facets2 = new CorruptedProjectFacets(e);
			}

		} else { // absProj instanceof CorruptedProject
			CorruptedProject proj = (CorruptedProject) absProj;
			status = new ProjectStatus(Status.corrupted, proj.getCauseOfCorruption().getMessage());
		}
		return new ProjectInfo(name, open, baseURI, defaultNamespace, model, lexicalizationModel,
				historyEnabled, validationEnabled, shaclEnabled, facets, access, repoLocation, status,
				description, facets2);
	}

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
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public JsonNode getAccessStatusMap()
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode responseNode = jsonFactory.arrayNode();

		// list and sort alphabetically
		List<AbstractProject> projects = new ArrayList<>(ProjectManager.listProjects());
		projects.sort(Comparator.comparing(AbstractProject::getName));

		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project) {
				Project project = (Project) absProj;
				JsonNode projectNode = createProjectAclNode(project);
				responseNode.add(projectNode);
			}
		}
		return responseNode;
	}

	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public JsonNode getAccessStatus(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		Project project = ProjectManager.getProjectDescription(projectName);
		return createProjectAclNode(project);
	}

	private JsonNode createProjectAclNode(Project project)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

		ObjectNode projectNode = jsonFactory.objectNode();
		projectNode.set("name", jsonFactory.textNode(project.getName()));

		ArrayNode consumerArrayNode = jsonFactory.arrayNode();

		List<AbstractProject> consumers = new ArrayList<>(ProjectManager.listProjects());
		consumers.sort(Comparator.comparing(AbstractProject::getName));

		consumers.removeIf(c -> c.getName().equals(project.getName())); // remove itself from its possible
																		// consumers

		ProjectACL projectAcl = project.getACL();

		// status for SYSTEM
		ProjectConsumer consumer = ProjectConsumer.SYSTEM;
		JsonNode consumerAclNode = createConsumerAclNode(project, consumer);
		consumerArrayNode.add(consumerAclNode);
		// ACL for other ProjectConsumer
		for (AbstractProject absCons : consumers) {
			if (absCons instanceof Project) {
				consumer = absCons;
				consumerAclNode = createConsumerAclNode(project, consumer);
				consumerArrayNode.add(consumerAclNode);
			}
		}

		projectNode.set("consumers", consumerArrayNode);

		AccessLevel univAclLevel = projectAcl.getUniversalAccessLevel();
		String universalAclStr = univAclLevel != null ? univAclLevel.name() : null;
		projectNode.set("universalACLLevel", jsonFactory.textNode(universalAclStr));

		// LOCK for the project
		ObjectNode lockNode = jsonFactory.objectNode();
		lockNode.set("availableLockLevel", jsonFactory.textNode(projectAcl.getLockLevel().name()));
		ProjectConsumer lockingConsumer = ProjectManager.getLockingConsumer(project.getName());
		String lockingConsumerName = null;
		String acquiredLockLevel = null;
		if (lockingConsumer != null) { // the project could be not locked by any consumer
			lockingConsumerName = lockingConsumer.getName();
			acquiredLockLevel = ProjectManager.getLockingLevel(project.getName(), lockingConsumer).name();
		}
		lockNode.set("lockingConsumer", jsonFactory.textNode(lockingConsumerName));
		lockNode.set("acquiredLockLevel", jsonFactory.textNode(acquiredLockLevel));
		projectNode.set("lock", lockNode);

		return projectNode;
	}

	private JsonNode createConsumerAclNode(Project project, ProjectConsumer consumer)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

		ObjectNode consumerNode = jsonFactory.objectNode();
		consumerNode.set("name", jsonFactory.textNode(consumer.getName()));

		ProjectACL projectAcl = project.getACL();

		String availableAclLevel = null;
		// universal access level is valid for every project consumers except for SYSTEM
		AccessLevel aclUniversal = (consumer != ProjectConsumer.SYSTEM) ? projectAcl.getUniversalAccessLevel()
				: null;
		AccessLevel aclForConsumer = projectAcl.getAccessLevelForConsumer(consumer);
		if (aclUniversal != null) {
			availableAclLevel = aclUniversal.name();
		} else if (aclForConsumer != null) {
			availableAclLevel = aclForConsumer.name();
		}
		consumerNode.set("availableACLLevel", jsonFactory.textNode(availableAclLevel));

		String acquiredAclLevel = null;
		AccessLevel accessedLevel = ProjectManager.getAccessedLevel(project.getName(), consumer);
		if (accessedLevel != null) {
			acquiredAclLevel = accessedLevel.name();
		}
		consumerNode.set("acquiredACLLevel", jsonFactory.textNode(acquiredAclLevel));

		return consumerNode;
	}

	/**
	 * Update the AccessLevel of the current project
	 * 
	 * @param consumerName
	 * @param accessLevel
	 *            if not provided revoke any access level assigned from the project to the consumer
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void updateAccessLevel(String consumerName, @Optional AccessLevel accessLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = getProject();
		if (accessLevel != null) {
			project.getACL().grantAccess(ProjectManager.getProjectDescription(consumerName), accessLevel);
		} else {
			project.getACL().revokeAccess(ProjectManager.getProjectDescription(consumerName));
		}
	}

	/**
	 *
	 * @param projectName
	 * @param consumerName
	 * @param accessLevel
	 *            if not provided revoke any access level assigned from the project to the consumer
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void updateProjectAccessLevel(String projectName, String consumerName,
			@Optional AccessLevel accessLevel) throws InvalidProjectNameException, ProjectInexistentException,
			ProjectAccessException, ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProject(projectName, true);
		if (accessLevel != null) {
			project.getACL().grantAccess(ProjectManager.getProjectDescription(consumerName), accessLevel);
		} else {
			project.getACL().revokeAccess(ProjectManager.getProjectDescription(consumerName));
		}
	}

	/**
	 * Update the universal (for every consumer) AccessLevel of the current project
	 * 
	 * @param accessLevel
	 *            if not provided revoke any universal access level assigned from the project
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void updateUniversalAccessLevel(@Optional AccessLevel accessLevel)
			throws ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = getProject();
		if (accessLevel != null) {
			project.getACL().grantUniversalAccess(accessLevel);
		} else {
			project.getACL().revokeUniversalAccess();
		}
	}

	/**
	 * Update the universal (for every consumer) AccessLevel of the given project
	 * 
	 * @param projectName
	 * @param accessLevel
	 *            if not provided revoke any universal access level assigned from the project
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void updateUniversalProjectAccessLevel(String projectName, @Optional AccessLevel accessLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProject(projectName, true);
		if (accessLevel != null) {
			project.getACL().grantUniversalAccess(accessLevel);
		} else {
			project.getACL().revokeUniversalAccess();
		}
	}

	/**
	 * Updates the lock level of the accessed project
	 *
	 * @param lockLevel
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 * @throws ReservedPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'U')")
	public void updateLockLevel(LockLevel lockLevel)
			throws ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = getProject();
		project.getACL().setLockableWithLevel(lockLevel);
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
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void updateProjectLockLevel(String projectName, LockLevel lockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProject(projectName, true);
		project.getACL().setLockableWithLevel(lockLevel);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void deleteProject(ProjectConsumer consumer, String projectName)
			throws ProjectDeletionException, ProjectAccessException, ProjectUpdateException,
			ReservedPropertyUpdateException, InvalidProjectNameException, ProjectInexistentException {
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
	 * @throws ProjectBindingException
	 * @throws RBACException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void accessProject(ProjectConsumer consumer, String projectName,
			ProjectACL.AccessLevel requestedAccessLevel, ProjectACL.LockLevel requestedLockLevel)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ForbiddenProjectAccessException, RBACException {
		ProjectManager.accessProject(consumer, projectName, requestedAccessLevel, requestedLockLevel);
	}

	/**
	 * see {@link ProjectManager#disconnectFromProject(ProjectConsumer, String)}
	 * 
	 * @param consumer
	 * @param projectName
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void disconnectFromProject(ProjectConsumer consumer, String projectName) {

		ProjectManager.disconnectFromProject(consumer, projectName);
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
	 * @throws IOException
	 * @throws DuplicatedResourceException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void cloneProject(String projectName, String newProjectName) throws InvalidProjectNameException,
			DuplicatedResourceException, IOException, ProjectInexistentException, ProjectAccessException {

		logger.debug("requested to export current project");

		ProjectManager.cloneProjectToNewProject(projectName, newProjectName);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public void exportProject(HttpServletResponse oRes,
			@RequestParam(value = "projectName") String projectName)
			throws IOException, ProjectAccessException {
		File tempServerFile = File.createTempFile("export", ".zip");
		logger.debug("requested to export current project");
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
	 * @param importPackage
	 * @param newProjectName
	 * @throws InvalidProjectNameException
	 * @throws ProjectUpdateException
	 * @throws ProjectInconsistentException
	 * @throws DuplicatedResourceException
	 * @throws ProjectCreationException
	 * @throws IOException
	 * @throws ProjectBindingException
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void importProject(MultipartFile importPackage, String newProjectName)
			throws IOException, ProjectCreationException, DuplicatedResourceException, ProjectUpdateException,
			InvalidProjectNameException {

		logger.debug("requested to import project from file: " + importPackage);

		File projectFile = File.createTempFile("prefix", "suffix");
		importPackage.transferTo(projectFile);
		ProjectManager.importProject(projectFile, newProjectName);
	}

	/**
	 * this service returns a list name-value for all the property of a given project. Returns a response with
	 * elements called {@code propertyTag} with attributes {@code propNameAttr} for property name and
	 * 
	 * @param projectName
	 *            (optional)the project queried for properties
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public Collection<ProjectPropertyInfo> getProjectPropertyMap(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, IOException {

		return ProjectManager.getProjectPropertyMap(projectName).entrySet().stream()
				.map(entry -> new ProjectPropertyInfo(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * this service returns a list name-value for all the property of a given project. Returns a response with
	 * elements called {@code propertyTag} with attributes {@code propNameAttr} for property name and
	 * 
	 * @param projectName
	 *            (optional)the project queried for properties
	 * @return
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public String getProjectPropertyFileContent(String projectName)
			throws InvalidProjectNameException, ProjectInexistentException, IOException {
		return ProjectManager.getProjectPropertyFileContent(projectName);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void saveProjectPropertyFileContent(String projectName, String content)
			throws InvalidProjectNameException, ProjectInexistentException, IOException {
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
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setProjectProperty(String projectName, String propName, @Optional String propValue)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException, ReservedPropertyUpdateException {
		Project project = ProjectManager.getProjectDescription(projectName);
		if (propValue != null) {
			project.setProperty(propName, propValue);
		} else {
			project.removeProperty(propName, propValue);
		}
	}

	/**
	 * Sets the "dir" facet to the given project. If the value is null, the facet is removed
	 * 
	 * @param projectName
	 * @param facetValue
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setProjectFacetDir(String projectName, @Optional String facetValue)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			ProjectUpdateException {
		Project project = ProjectManager.getProject(projectName, true);
		if (facetValue != null) {
			project.setFacetDir(facetValue);
		} else {
			project.removeFacetDir();
		}
	}

	/**
	 * Sets the facets of a project
	 * 
	 * @param projectName
	 * @param facets
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws NoSuchSettingsManager
	 * @throws IllegalStateException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setProjectFacets(String projectName, ObjectNode facets)
			throws IllegalStateException, NoSuchSettingsManager, STPropertyUpdateException,
			WrongPropertiesException, STPropertyAccessException, ProjectAccessException,
			InvalidProjectNameException, ProjectInexistentException, IOException, PropertyNotFoundException {
		Project project = ProjectManager.getProject(projectName, true);
		exptManager.storeSettings(ProjectFacetsStore.class.getName(), project, UsersManager.getLoggedUser(),
				Scope.PROJECT, facets);
		//update the index about the facets of this project
		recreateFacetIndexForProjectAPI(projectName);
	}

	/**
	 * Returns the facets of a project
	 * 
	 * @param projectName
	 * 
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyAccessException
	 * @throws IllegalStateException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 * 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public ProjectFacets getProjectFacets(String projectName)
			throws IllegalStateException, STPropertyAccessException, NoSuchSettingsManager,
			ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		Project project = ProjectManager.getProject(projectName, true);
		return (ProjectFacets) exptManager.getSettings(project, UsersManager.getLoggedUser(),
				ProjectFacetsStore.class.getName(), Scope.PROJECT);
	}

	/**
	 * Sets the schema of project facets
	 * 
	 * @param facetsSchema
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setProjectFacetsSchema(ObjectNode facetsSchema)
			throws IllegalStateException, NoSuchSettingsManager, STPropertyUpdateException,
			WrongPropertiesException, STPropertyAccessException, ProjectAccessException,
			InvalidProjectNameException, ProjectInexistentException {
		exptManager.storeSettings(ProjectFacetsSchemaStore.class.getName(), null,
				UsersManager.getLoggedUser(), Scope.SYSTEM, facetsSchema);
	}

	/**
	 * Returns the schema of project facets
	 * 
	 * @throws NoSuchSettingsManager
	 * @throws STPropertyAccessException
	 * @throws IllegalStateException
	 * 
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public STPropertiesSchema getProjectFacetsSchema()
			throws IllegalStateException, STPropertyAccessException, NoSuchSettingsManager {
		return (STPropertiesSchema) exptManager.getSettings(null, UsersManager.getLoggedUser(),
				ProjectFacetsSchemaStore.class.getName(), Scope.SYSTEM);
	}

	/**
	 *
	 * @param oldValue
	 * @param newValue
	 * @throws ProjectAccessException
	 * @throws ProjectUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void renameProjectFacetDir(String oldValue, String newValue)
			throws ProjectAccessException, ProjectUpdateException {
		Collection<AbstractProject> projects = ProjectManager.listProjects();
		for (AbstractProject absProj : projects) {
			if (absProj instanceof Project) {
				Project project = (Project) absProj;
				if (oldValue.equals(project.getFacetDir())) {
					project.setFacetDir(newValue);
				}
			}
		}
	}

	/**
	 * Returns the repositories associated with a (closed) project. Optionally, it is possible to skip local
	 * repositories.
	 * 
	 * @param projectName
	 * @param excludeLocal
	 * @throws ProjectAccessException
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(project)', 'R')")
	public Collection<RepositorySummary> getRepositories(String projectName,
			@Optional(defaultValue = "false") boolean excludeLocal)
			throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {

		Collection<RepositorySummary> rv = new ArrayList<>();

		ProjectManager.handleProjectExclusively(projectName, project -> {
			STLocalRepositoryManager repoManager = new STLocalRepositoryManager(
					project.getProjectDirectory());
			repoManager.init();
			try {
				Collection<RepositorySummary> summaries = Project.getRepositorySummaries(repoManager,
						excludeLocal);
				rv.addAll(summaries);
			} finally {
				repoManager.shutDown();
			}
		});

		return rv;
	}

	/**
	 * Modifies the access credentials of a repository associated with a given (closed) project. The new
	 * username and password are optional: if they are not given, they are considered <code>null</code>, thus
	 * indicating an unprotected repository.
	 * 
	 * @param projectName
	 * @param repositoryID
	 * @param newUsername
	 * @param newPassword
	 * @throws ProjectAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void modifyRepositoryAccessCredentials(String projectName, String repositoryID,
			@Optional String newUsername, @Optional String newPassword)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		ProjectManager.handleProjectExclusively(projectName, project -> {
			STLocalRepositoryManager repoManager = new STLocalRepositoryManager(
					project.getProjectDirectory());
			repoManager.init();
			try {
				repoManager.modifyAccessCredentials(repositoryID, newUsername, newPassword);
			} finally {
				repoManager.shutDown();
			}
		});
	}

	/**
	 * Modifies the access credentials of (possibly) many repositories at once. The repositories shall match
	 * the provided <code>serverURL</code> and <code>currentUsername</code> (only if
	 * <code>matchUsername</code> is <code>true</code>). When username matching is active, a <code>null</code>
	 * value for <code>currentUsername</code> indicates repositories with no associated username.
	 * 
	 * @param projectName
	 * @param serverURL
	 * @param matchUsername
	 * @param currentUsername
	 * @param newUsername
	 * @param newPassword
	 * @throws ProjectAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void batchModifyRepostoryAccessCredentials(String projectName, String serverURL,
			@Optional(defaultValue = "false") boolean matchUsername, @Optional String currentUsername,
			@Optional String newUsername, @Optional String newPassword)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		ProjectManager.handleProjectExclusively(projectName, project -> {
			STLocalRepositoryManager repoManager = new STLocalRepositoryManager(
					project.getProjectDirectory());
			repoManager.init();
			try {
				repoManager.batchModifyAccessCredentials(serverURL, matchUsername, currentUsername,
						newUsername, newPassword);
			} finally {
				repoManager.shutDown();
			}
		});

	}

	/**
	 * Preloads data contained provided in the request body.
	 * 
	 * @param preloadedData
	 * @param preloadedDataFormat
	 * @return
	 * @throws IOException
	 * @throws ProfilerException
	 * @throws RepositoryException
	 * @throws RDFParseException
	 * @throws AssessmentException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public PreloadedDataSummary preloadDataFromFile(MultipartFile preloadedData,
			RDFFormat preloadedDataFormat) throws IOException, RDFParseException, RepositoryException,
			ProfilerException, AssessmentException, STPropertyAccessException {
		File preloadedDataFile = preloadedDataStore.preloadData(preloadedData::transferTo);

		String baseURI = null;
		IRI model = null;
		IRI lexicalizationModel = null;

		return preloadDataInternal(baseURI, model, lexicalizationModel, preloadedDataFile,
				preloadedDataFormat);
	}

	/**
	 * Preloads data from URL.
	 *
	 * @param preloadedDataURL
	 * @param preloadedDataFormat
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ProfilerException
	 * @throws RepositoryException
	 * @throws RDFParseException
	 * @throws AssessmentException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public PreloadedDataSummary preloadDataFromURL(URL preloadedDataURL,
			@Optional RDFFormat preloadedDataFormat)
			throws FileNotFoundException, IOException, RDFParseException, RepositoryException,
			ProfilerException, AssessmentException, STPropertyAccessException {

		logger.debug("Preload data from URL = {} (format = {})", preloadedDataURL, preloadedDataFormat);

		File preloadedDataFile;

		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		try (CloseableHttpClient httpClient = httpClientBuilder.useSystemProperties().build()) {
			HttpGet request = new HttpGet(preloadedDataURL.toExternalForm());
			Set<RDFFormat> rdfFormats = preloadedDataFormat != null
					? Collections.singleton(preloadedDataFormat)
					: RDFParserRegistry.getInstance().getKeys();
			List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, false, null);
			acceptParams.forEach(acceptParam -> request.addHeader("Accept", acceptParam));
			request.addHeader("Accept", "application/zip;q=0.5");
			request.addHeader("Accept", "application/gzip;q=0.5");
			request.addHeader("Accept", "*/*;q=0.1");

			try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
				HttpEntity httpEntity = httpResponse.getEntity();
				if (preloadedDataFormat == null) {
					Header contentTypeHeader = httpEntity.getContentType();
					if (contentTypeHeader != null) {
						ContentType contentType = ContentType.parse(contentTypeHeader.getValue());
						String mime = contentType.getMimeType();
						// only process non-archive mime types
						if (!Arrays.asList("application/zip", "application/gzip").contains(mime)) {
							preloadedDataFormat = Rio.getParserFormatForMIMEType(mime)
									.orElseThrow(Rio.unsupportedFormat(mime));
						}
					}

					if (preloadedDataFormat == null) { // not provided, nor obtained through MIME type
						// this should also handle filenames decorated by archive formats e.g. .nt.gz
						preloadedDataFormat = Rio.getParserFormatForFileName(preloadedDataURL.getPath())
								.orElse(null);
					}
				}

				preloadedDataFile = preloadedDataStore.preloadData(f -> {
					try (OutputStream out = new FileOutputStream(f)) {
						IOUtils.copy(httpEntity.getContent(), out);
					}
				});
			}
		}

		String baseURI = null;
		IRI model = null;
		IRI lexicalizationModel = null;

		return preloadDataInternal(baseURI, model, lexicalizationModel, preloadedDataFile,
				preloadedDataFormat);
	}

	/**
	 * Preloads data from a catalog.
	 * 
	 * @param connectorId
	 * @param datasetId
	 * @return
	 * @throws IOException
	 * @throws ProfilerException
	 * @throws RepositoryException
	 * @throws RDFParseException
	 * @throws AssessmentException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public PreloadedDataSummary preloadDataFromCatalog(String connectorId, String datasetId)
			throws IOException, RDFParseException, RepositoryException, ProfilerException,
			AssessmentException, STPropertyAccessException {
		DatasetCatalogConnector datasetCatalogConnector = (DatasetCatalogConnector) ((NonConfigurableExtensionFactory<?>) exptManager
				.getExtension(connectorId)).createInstance();

		DatasetDescription datasetDescrition = datasetCatalogConnector.describeDataset(datasetId);
		URL dataDump = datasetDescrition.getDataDumps().stream().map(DownloadDescription::getAccessURL)
				.findAny().orElse(null);
		if (dataDump == null) {
			IRI ontologyIRI = datasetDescrition.getOntologyIRI();
			if (ontologyIRI == null) {
				throw new IOException("Missing data dump for preloaded dataset");
			} else {
				dataDump = new URL(ontologyIRI.toString());
			}
		}

		return preloadDataFromURL(dataDump, null);
	}

	private PreloadedDataSummary preloadDataInternal(@Nullable String baseURI, @Nullable IRI model,
			@Nullable IRI lexicalizationModel, File preloadedDataFile, RDFFormat preloadedDataFormat)
			throws RDFParseException, RepositoryException, IOException, ProfilerException,
			AssessmentException, STPropertyAccessException {

		if (!preloadedDataFile.exists()) {
			throw new FileNotFoundException(preloadedDataFile.getPath() + ": not existing");
		}

		if (!preloadedDataFile.isFile()) {
			throw new FileNotFoundException(preloadedDataFile.getPath() + ": not a normal file");
		}

		long dataSize = preloadedDataFile.length();

		List<PreloadedDataSummary.PreloadWarning> preloadWarnings = new ArrayList<>();

		if (baseURI == null || model == null || lexicalizationModel == null) {
			String profilerDataSizeTresholdString = STPropertiesManager
					.getSystemSetting(STPropertiesManager.PRELOAD_PROFILER_TRESHOLD_BYTES);
			long profilerDataSizeTreshold;
			if (profilerDataSizeTresholdString != null) {
				profilerDataSizeTreshold = Long.parseLong(profilerDataSizeTresholdString);
			} else {
				profilerDataSizeTreshold = FileUtils.ONE_MB;
			}
			if (dataSize > profilerDataSizeTreshold) { // preloaded data too big to profile
				preloadWarnings = new ArrayList<>(1);
				preloadWarnings
						.add(new PreloadedDataSummary.ProfilerSizeTresholdExceeded(profilerDataSizeTreshold));
			} else { // profile the preloaded data to obtain the necessary information
				preloadWarnings = new ArrayList<>();

				try (Closer closer = Closer.create()) {
					// metadata repository
					SailRepository metadataRepo = new SailRepository(new MemoryStore());
					metadataRepo.init();
					closer.register(metadataRepo::shutDown);

					// data repository
					SailRepository dataRepo = new SailRepository(new MemoryStore());
					dataRepo.init();
					closer.register(dataRepo::shutDown);

					try (LIMERepositoryConnectionWrapper metadataConn = new LIMERepositoryConnectionWrapper(
							metadataRepo, metadataRepo.getConnection());
							RepositoryConnection dataConn = dataRepo.getConnection()) {
						ValueFactory vf = dataConn.getValueFactory();

						IRI metadataBaseURI = vf.createIRI(
								"http://example.org/" + UUID.randomUUID().toString() + "/void.ttl");
						IRI dataGraph = vf.createIRI("urn:uuid:" + UUID.randomUUID().toString());

						// load preloaded data to the data repository
						dataConn.add(preloadedDataFile, null, preloadedDataFormat, dataGraph);

						// profile the preloaded data
						LIMEProfiler profiler = new LIMEProfiler(metadataConn, metadataBaseURI, dataConn,
								dataGraph);
						profiler.profile();

						// export the profile as a Model
						Model profile = new LinkedHashModel();
						StatementCollector collector = new StatementCollector(profile);
						metadataConn.export(collector);

						// Extract information from the profile
						IRI mainDataset = metadataConn.getMainDataset(false).filter(IRI.class::isInstance)
								.map(IRI.class::cast).orElse(null);

						if (lexicalizationModel == null) {
							lexicalizationModel = mediationFramework
									.assessLexicalizationModel(mainDataset, profile).orElse(null);
						}

						logger.debug("main dataset = {}", mainDataset);
						logger.debug("profile = {}", new Object() {
							@Override
							public String toString() {
								StringWriter writer = new StringWriter();
								Rio.write(profile, Rio.createWriter(RDFFormat.TURTLE, writer));
								return writer.toString();
							}
						});

						if (model == null) {
							model = Models.objectIRI(QueryResults.asModel(
									metadataConn.getStatements(mainDataset, DCTERMS.CONFORMS_TO, null)))
									.orElse(null);
						}

						// Extract the baseURI as the ontology IRI
						java.util.Optional<IRI> baseURIHolder = Iterations
								.stream(dataConn.getStatements(null, RDF.TYPE, OWL.ONTOLOGY))
								.filter(s -> s.getSubject() instanceof IRI).map(s -> (IRI) s.getSubject())
								.findAny();

						if (baseURIHolder.isPresent()) { // gets the base URI from the ontology object
							baseURI = baseURIHolder.get().stringValue();
						} else { // otherwise, determine the base URI from the data
							TupleQuery nsQuery = dataConn.prepareTupleQuery(
							// @formatter:off
								"SELECT ?ns (COUNT(*) as ?count)  WHERE {\n" + 
								"    GRAPH ?dataGraph {\n" + 
								"    	?s ?p ?o .\n" + 
								"    }\n" + 
								"}\n" + 
								"GROUP BY (REPLACE(STR(?s), \"^([^#]*(#|\\\\/))(.*)$\", \"$1\") as ?ns)\n" + 
								"ORDER BY DESC(?count)\n" +
								"LIMIT 1"
								// @formatter:on
							);
							nsQuery.setBinding("dataGraph", dataGraph);
							BindingSet bs = QueryResults.singleResult(nsQuery.evaluate());
							if (bs != null && bs.hasBinding("ns")) {
								baseURI = bs.getValue("ns").stringValue(); // possible trailing # stripped
																			// later
							}

						}

						if (baseURI != null && baseURI.endsWith("#")) {
							baseURI = baseURI.substring(0, baseURI.length() - 1);
						}
					}
				}
			}
		}

		return new PreloadedDataSummary(baseURI, model, lexicalizationModel, preloadedDataFile,
				preloadedDataFormat, preloadWarnings);
	}



	//** ALL SERVICES AND APIS DEALING WITH THE LUCENE INDEX FOR THE FACETS**//

	@STServiceOperation
	public Map<String, List<String>> getFacetsAndValue() throws IOException {
		Map<String, List<String>> facetValueListMap = new HashMap<>();

		Query query = new MatchAllDocsQuery();
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			int maxResults = MAX_RESULT_QUERY_FACETS;
			IndexSearcher searcher = createSearcher();
			TopDocs topDocs = searcher.search(query, maxResults);
			for(ScoreDoc scoreDoc : topDocs.scoreDocs){
				Document doc = searcher.doc(scoreDoc.doc);
				for(IndexableField indexableField : doc.getFields()){
					String name = indexableField.name();
					String value = indexableField.stringValue();
					if(!name.equals(PROJECT_NAME) && !name.equals(PROJECT_DESCRIPTION)) {
						if(!facetValueListMap.containsKey(name)){
							facetValueListMap.put(name, new ArrayList<>());
						}
						if(!facetValueListMap.get(name).contains(value)){
							facetValueListMap.get(name).add(value);
						}
					}
				}
			}
		}  finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}

		return facetValueListMap;
	}

	/**
	 * Create the Lucene index for the facets in ALL projects
	 */
	//@STServiceOperation
	@STServiceOperation(method = RequestMethod.POST)
	// TODO decide the @PreAuthorize
	public void createFacetIndex() throws PropertyNotFoundException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {
		createFacetIndexAPI();
	}

	private void createFacetIndexAPI() throws PropertyNotFoundException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
			try (IndexWriter writer = new IndexWriter(directory, config)) {
				//clear all indexes
				writer.deleteAll();

				//iterate over the existing projects
				Collection<AbstractProject> abstractProjectCollection = ProjectManager.listProjects(ProjectConsumer.SYSTEM);
				List<ProjectInfo> projInfoList = new ArrayList<>();
				for (AbstractProject abstractProject : abstractProjectCollection) {
					ProjectInfo projInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, AccessLevel.R, LockLevel.NO,
							false, false, abstractProject);
					if (projInfo != null) {
						projInfoList.add(projInfo);
					}
				}
				//add the projects facets and proeprties to the index (one project at a time)
				for (ProjectInfo projectInfo : projInfoList) {
					addProjectToIndex(projectInfo, false, writer);
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	/**
	 * Create the Lucene index for the facets in ALL projects
	 */
	//@STServiceOperation
	@STServiceOperation(method = RequestMethod.POST)
	// TODO decide the @PreAuthorize
	public void recreateFacetIndexForProject(String projectName) throws PropertyNotFoundException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {

		recreateFacetIndexForProjectAPI(projectName);
	}

	private void recreateFacetIndexForProjectAPI(String projectName) throws PropertyNotFoundException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Project project = ProjectManager.getProject(projectName);

			ProjectInfo projectInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, AccessLevel.R, LockLevel.NO,
					false, false, project);
			if(projectInfo == null){
				throw new ProjectAccessException(projectName);
			}
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
			try (IndexWriter writer = new IndexWriter(directory, config)) {
				addProjectToIndex(projectInfo, true, writer);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}


	private void addProjectToIndex(ProjectInfo projectInfo, boolean removePrevIndex, IndexWriter writer) throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
			IOException {
		ProjectForIndex projectForIndex = new ProjectForIndex();

		String projectName = projectInfo.getName();

		projectForIndex.addNameValue(PROJECT_NAME, projectInfo.getName());
		projectForIndex.addNameValue(PROJECT_MODEL ,projectInfo.getModel());
		projectForIndex.addNameValue(PROJECT_LEX_MODEL, projectInfo.getLexicalizationModel());
		projectForIndex.addNameValue(PROJECT_HISTORY, Boolean.toString(projectInfo.isHistoryEnabled()));
		projectForIndex.addNameValue(PROJECT_VALIDATION, Boolean.toString(projectInfo.isValidationEnabled()));
		projectForIndex.addNameValue(PROJECT_DESCRIPTION, projectInfo.getDescription());

		//for each project, get all the facets
		ProjectFacets projectFacets = projectInfo.getFacets2();
		for(String propName : projectFacets.getProperties()) {
			try {
				String propValue = null;
				Object value = projectFacets.getPropertyValue(propName);
				if(value instanceof DynamicSTProperties) {
					getValuesFromSTProperties((STProperties) value, projectForIndex);
				} else if(value != null) {
					propValue = normalizeFacetValue(value);
					if(propValue!=null && !propValue.isEmpty()) {
						projectForIndex.addNameValue(propName, propValue.toString());
					}
				}
			} catch (PropertyNotFoundException e) {
				//the facet was not found, so skip it and pass to the next one
			}
		}

		//remove the previous entry, if needed
		if(removePrevIndex){
			BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
			builderBoolean.add(new TermQuery(new Term(PROJECT_NAME, projectName)), BooleanClause.Occur.MUST);
			writer.deleteDocuments(builderBoolean.build());
		}
		//add ProjectForIndex in the index
		addProjectForIndexToIndex(projectForIndex, writer);
	}

	private String normalizeFacetValue(Object value){
		if(value==null){
			return "";
		}
		if(value instanceof IRI){
			return NTriplesUtil.toNTriplesString((IRI)value);
		} else if(value instanceof Literal){
			return NTriplesUtil.toNTriplesString((Literal) value);
		} else {
			return value.toString();
		}
	}

	private void getValuesFromSTProperties(STProperties stProperties, ProjectForIndex projectForIndex) throws PropertyNotFoundException {
		Collection<String> propertiesList = stProperties.getProperties();
		for(String propName : propertiesList) {
			if(stProperties.getPropertyValue(propName) == null){
				//no value, so skip it
				continue;
			}
			//TODO manage Obejct (toNT or toString)
			String valueString = stProperties.getPropertyValue(propName).toString();
			projectForIndex.addNameValue(propName, valueString);
		}
	}


	@STServiceOperation(method = RequestMethod.POST)
	public Map<String, List<ProjectInfo>> retrieveProjects(@Optional(defaultValue="")String bagOf, @JsonSerialized List<List<Map<String, Object>>> orQueryList) throws IOException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		Map<String, List<ProjectInfo>> facetToProjeInfoListMap = new HashMap<>();

		//bagOf and query cannot be both empy/null
		if((bagOf==null || bagOf.isEmpty()) && (orQueryList==null || orQueryList.isEmpty())){
			throw new IllegalArgumentException("bagOf and query cannot be both null/empty");
		}

		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			List<ProjectInfo> projectInfoList = new ArrayList<>();
			Query queryLuc;
			//if the query is not empty, construct a BooleanQuery
			if(orQueryList == null || orQueryList.isEmpty()){
				//get all the document (Project)
				queryLuc = new MatchAllDocsQuery();
			} else {
				//prepare a boolean query according to the inpurt query List
				BooleanQuery.Builder orBuilderBoolean = new BooleanQuery.Builder();
				for(List<Map<String, Object>> andQueryList : orQueryList){
					BooleanQuery.Builder andBuilderBoolean = new BooleanQuery.Builder();
					for(Map<String, Object> andQuery : andQueryList) {
						for (String facetName : andQuery.keySet()) {
							String facetValue = normalizeFacetValue(andQuery.get(facetName));
							andBuilderBoolean.add(new TermQuery(new Term(facetName, facetValue)), BooleanClause.Occur.MUST);
						}
						orBuilderBoolean.add(andBuilderBoolean.build(), BooleanClause.Occur.SHOULD);
					}
				}
				queryLuc = orBuilderBoolean.build();
			}

			//execute the query
			int maxResults = MAX_RESULT_QUERY_FACETS;
			IndexSearcher searcher = createSearcher();
			TopDocs topDocs = searcher.search(queryLuc, maxResults);

			//now, order the results according to the facet of the bagOf parameter (if specified)
			for(ScoreDoc sd : topDocs.scoreDocs){
				Document doc = searcher.doc(sd.doc);
				String projectName = doc.get(PROJECT_NAME);
				Project project = ProjectManager.getProjectDescription(projectName);
				ProjectInfo projectInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, AccessLevel.R, LockLevel.NO,
						false, false, project);

				String facetValue;
				if(bagOf!=null && !bagOf.isEmpty()){
					String facetName = bagOf;
					facetValue = doc.get(facetName);
					if(facetValue == null){
						facetValue = "";
					}
				} else {
					//no need to divide the results
					facetValue = "";
				}
				if(!facetToProjeInfoListMap.containsKey(facetValue)){
					facetToProjeInfoListMap.put(facetValue, new ArrayList<>());
				}
				facetToProjeInfoListMap.get(facetValue).add(projectInfo);
			}

		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
		return facetToProjeInfoListMap;
	}

	private class ProjectForIndex {
		Map<String, String> nameValueForIndexMap = new HashMap<>();

		public ProjectForIndex() {
		}

		public Set<String> getNameList() {
			return nameValueForIndexMap.keySet();
		}

		public String getValue(String name){
			return nameValueForIndexMap.get(name);
		}

		public boolean addNameValue(String name, String value){
			if(nameValueForIndexMap.containsKey(name)){
				return false;
			}
			nameValueForIndexMap.put(name, value);
			return true;
		}
	}

	private IndexSearcher createSearcher() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		IndexReader reader = DirectoryReader.open(directory);
		return new IndexSearcher(reader);
	}


	private boolean isLuceneDirPresent (){
		String mainIndexPath = Resources.getSemTurkeyDataDir()+File.separator+indexMainDir;
		File mainIndexDir = new File(mainIndexPath);
		if(!mainIndexDir.exists()){
			mainIndexDir.mkdir();
		}
		File luceneIndexDir = new File(mainIndexDir, lucDirName);
		if(!luceneIndexDir.exists()){
			return false;
		}
		return true;
	}

	private File getLuceneDir() {

		String mainIndexPath = Resources.getSemTurkeyDataDir()+File.separator+indexMainDir;
		File mainIndexDir = new File(mainIndexPath);
		if(!mainIndexDir.exists()){
			mainIndexDir.mkdir();
		}
		//String luceneIndexDirPath = Resources.getSemTurkeyDataDir()+File.separator+lucDirName;
		//File luceneIndexDir = new File(luceneIndexDirPath);
		File luceneIndexDir = new File(mainIndexDir, lucDirName);
		if(!luceneIndexDir.exists()) {
			luceneIndexDir.mkdir();
		}

		return luceneIndexDir;
	}

	private void addProjectForIndexToIndex(ProjectForIndex projectForIndex, IndexWriter writer) throws IOException {
		Document doc = new Document();
		for(String propName : projectForIndex.getNameList()){
			String propValue = projectForIndex.getValue(propName);
			if(propValue != null && !propValue.isEmpty()) {
				doc.add(new StringField(propName, propValue, Field.Store.YES));
			}
		}
		writer.addDocument(doc);
	}

}