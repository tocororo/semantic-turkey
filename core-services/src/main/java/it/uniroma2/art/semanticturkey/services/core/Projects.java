package it.uniroma2.art.semanticturkey.services.core;

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
import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerFactory;
import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.email.EmailApplicationContext;
import it.uniroma2.art.semanticturkey.email.EmailService;
import it.uniroma2.art.semanticturkey.email.EmailServiceFactory;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.ExceptionDAO;
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
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
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
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo;
import it.uniroma2.art.semanticturkey.properties.DataSize;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.properties.dynamic.STPropertiesSchema;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
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
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings;
import it.uniroma2.art.semanticturkey.settings.core.PreloadProfilerSettings;
import it.uniroma2.art.semanticturkey.settings.core.PreloadSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.settings.facets.CorruptedProjectFacets;
import it.uniroma2.art.semanticturkey.settings.facets.CustomProjectFacetsSchemaStore;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacets;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacetsIndexUtils;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacetsStore;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBinding;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersGroup;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.vocabulary.SUPPORT;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
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
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
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
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.config.SailConfigSchema;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailFactory;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@STService
public class Projects extends STServiceAdapter {

    private static Logger logger = LoggerFactory.getLogger(Projects.class);

    @Autowired
    private MediationFramework mediationFramework;

    @Autowired
    private PreloadedDataStore preloadedDataStore;

    /**
     * Returns the backend type of the context repository
     *
     * @return
     */
    @STServiceOperation
    public String getContextRepositoryBackend() {
        String repId = STServiceContextUtils.getRepostoryId(stServiceContext);
        java.util.Optional<STRepositoryInfo> repInfo = getProject().getRepositoryManager().getSTRepositoryInfo(repId);
        return repInfo.map(STRepositoryInfo::getBackendType).orElse(null);
    }

    // TODO understand how to specify remote repository / different sail configurations
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isSuperUser(false)")
    public void createProject(ProjectConsumer consumer, String projectName, IRI model,
            IRI lexicalizationModel, String baseURI, boolean historyEnabled, boolean validationEnabled,
            @Optional(defaultValue = "false") boolean blacklistingEnabled, RepositoryAccess repositoryAccess,
            String coreRepoID,
            @Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryConfigurer\", \"configuration\" : {\"@type\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfiguration\"}}") PluginSpecification coreRepoSailConfigurerSpecification,
            @Optional String coreBackendType, String supportRepoID,
            @Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryConfigurer\", \"configuration\" : {\"@type\" : \"it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfiguration\"}}") PluginSpecification supportRepoSailConfigurerSpecification,
            @Optional String supportBackendType,
            @Optional(defaultValue = "{\"factoryId\" : \"it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGenerator\", \"configuration\" : {\"@type\" : \"it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGeneratorConfiguration\"}}") PluginSpecification uriGeneratorSpecification,
            @Optional PluginSpecification renderingEngineSpecification,
            @Optional @JsonSerialized List<Pair<RDFResourceRole, String>> resourceMetadataAssociations,
            @Optional String preloadedDataFileName, @Optional RDFFormat preloadedDataFormat,
            @Optional TransitiveImportMethodAllowance transitiveImportAllowance, @Optional String leftDataset,
            @Optional String rightDataset, @Optional boolean shaclEnabled,
            @Optional @JsonSerialized SHACLSettings shaclSettings, @Optional boolean trivialInferenceEnabled,
            @Optional(defaultValue = "false") boolean openAtStartup,
            @Optional(defaultValue = "false") boolean globallyAccessible,
            @Optional Literal label, @Optional(defaultValue = "false") boolean undoEnabled)
            throws ProjectInconsistentException, InvalidProjectNameException, ProjectInexistentException,
            ProjectAccessException, ForbiddenProjectAccessException, DuplicatedResourceException,
            ProjectCreationException, ClassNotFoundException, WrongPropertiesException, RBACException,
            UnsupportedModelException, UnsupportedLexicalizationModelException, InvalidConfigurationException,
            STPropertyAccessException, IOException, ReservedPropertyUpdateException, ProjectUpdateException,
            STPropertyUpdateException, NoSuchConfigurationManager, PropertyNotFoundException, ProjectBindingException {

        List<Object> preloadRelatedArgs = Arrays.asList(preloadedDataFileName, preloadedDataFormat,
                transitiveImportAllowance);
        if (!preloadRelatedArgs.stream().allMatch(java.util.Objects::nonNull)
                && !preloadRelatedArgs.stream().noneMatch(java.util.Objects::nonNull)) {
            throw new IllegalArgumentException(
                    "All preload-related arguments must be specified together, or none of them can be specified");
        }

        // If no rendering engine has been configured, guess the best one based on the model type
        if (renderingEngineSpecification == null) {
            renderingEngineSpecification = BaseRenderingEngine
                    .getRenderingEngineSpecificationForLexicalModel(lexicalizationModel)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unsupported lexicalization model: " + lexicalizationModel));
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

        Set<IRI> failedImports = new HashSet<>();

        File preloadedDataFile = preloadedDataFileName != null
                ? preloadedDataStore.startConsumingPreloadedData(preloadedDataFileName)
                : null;
        boolean deletePreloadedDataFile = false;
        try {
            ProjectManager.createProject(consumer, projectName, label, model, lexicalizationModel, baseURI.trim(),
                    historyEnabled, validationEnabled, blacklistingEnabled, repositoryAccess, coreRepoID,
                    coreRepoSailConfigurerSpecification, coreBackendType, supportRepoID,
                    supportRepoSailConfigurerSpecification, supportBackendType, uriGeneratorSpecification,
                    renderingEngineSpecification, resourceMetadataAssociations, preloadedDataFile,
                    preloadedDataFormat, transitiveImportAllowance, failedImports, leftDataset, rightDataset,
                    shaclEnabled, shaclSettings, trivialInferenceEnabled, openAtStartup, globallyAccessible, undoEnabled);
            deletePreloadedDataFile = true;
        } finally {
            if (preloadedDataFileName != null) {
                preloadedDataStore.finishConsumingPreloadedData(preloadedDataFileName,
                        deletePreloadedDataFile);
            }
        }
        // create the index about the facets of this project
        Project project = ProjectManager.getProject(projectName);
        ProjectInfo projectInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, ProjectACL.AccessLevel.R,
                ProjectACL.LockLevel.NO, false, false, project);
        if (projectInfo == null) {
            throw new ProjectAccessException(projectName);
        }
        ProjectFacetsIndexUtils.recreateFacetIndexForProjectAPI(projectName, projectInfo);

        STUser loggedUser = UsersManager.getLoggedUser();
        if (loggedUser.isSuperUser(true)) {
            //set the superuser as PM of the created project
            ProjectUserBindingsManager.addRolesToPUBinding(loggedUser, project, Arrays.asList(RBACManager.DefaultRole.PROJECTMANAGER));
            //send notification to administrators
            try {
                EmailService emailService = EmailServiceFactory.getService(EmailApplicationContext.VB); //uses VB service since in SV superuser is not used
                emailService.sendProjCreationMailToAdmin(loggedUser, project);
            } catch (Exception e) { //catch generic Exception in order to avoid annoying exception raised to the client when the configuration is invalid
                logger.error(Utilities.printFullStackTrace(e));
            }
        }
    }

    /**
     * Returns an empty form for SHACL settings upon project creation.
     *
     * @return
     */
    @STServiceOperation
    @PreAuthorize("@auth.isSuperUser(false)")
    public SHACLSettings createEmptySHACLSettingsForm() {
        return new SHACLSettings();
    }

    @STServiceOperation()
    public Boolean projectExists(String projectName) throws InvalidProjectNameException {
        return ProjectManager.projectExists(projectName);
    }

    /**
     * @param consumer
     * @param requestedAccessLevel
     * @param requestedLockLevel
     * @param userDependent        if true, returns only the projects accessible by the logged user (the user has a role
     *                             assigned in it)
     * @param onlyOpen             if true, return only the open projects
     * @return
     * @throws ProjectAccessException
     */
    @STServiceOperation
    public List<ProjectInfo> listProjects(@Optional(defaultValue = "SYSTEM") ProjectConsumer consumer,
            @Optional(defaultValue = "R") ProjectACL.AccessLevel requestedAccessLevel,
            @Optional(defaultValue = "NO") ProjectACL.LockLevel requestedLockLevel,
            @Optional(defaultValue = "false") boolean userDependent,
            @Optional(defaultValue = "false") boolean onlyOpen) throws ProjectAccessException,
            PropertyNotFoundException, IOException, InvalidProjectNameException {

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

        // check if the lucene dir (for the facets) exists, if not, create the indexes
        createFacetIndexIfNeeded();

        return listProjInfo;
    }


    protected void createFacetIndexIfNeeded() throws ProjectAccessException, PropertyNotFoundException,
            IOException, InvalidProjectNameException {
        // check if the lucene dir (for the facets) exists, if not, create the indexes
        if (!ProjectFacetsIndexUtils.isLuceneDirPresent()) {
            // iterate over the existing projects
            Collection<AbstractProject> abstractProjectCollection = ProjectManager
                    .listProjects(ProjectConsumer.SYSTEM);
            List<ProjectInfo> projInfoList = new ArrayList<>();
            for (AbstractProject abstractProject : abstractProjectCollection) {
                ProjectInfo projInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, ProjectACL.AccessLevel.R,
                        ProjectACL.LockLevel.NO, false, false, abstractProject);
                if (projInfo != null) {
                    projInfoList.add(projInfo);
                }
            }
            // create the indexes
            ProjectFacetsIndexUtils.createFacetIndexAPI(projInfoList);
        }
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
            @Optional(defaultValue = "false") boolean userDependent,
            @Optional(defaultValue = "false") boolean onlyOpen) throws ProjectAccessException {
        List<ProjectInfo> listProjInfo = new ArrayList<>();

        for (AbstractProject absProj : ProjectManager.listProjects(consumer)) {
            ProjectInfo projInfo = getProjectInfoHelper(consumer, requestedAccessLevel, requestedLockLevel,
                    userDependent, onlyOpen, absProj);
            if (projInfo != null) {
                Collection<ProjectUserBinding> puBindings = ProjectUserBindingsManager
                        .listPUBindingsOfProject(absProj);
                for (ProjectUserBinding pub : puBindings) { // looks into the bindings if there is at least
                    // one with the given role
                    if (pub.getRoles().stream().anyMatch(r -> r.getName().equals(role))) {
                        // the PU binding has the given role
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
     * @param userDependent        if true, returns only the projects accessible by the logged user (the user has a role
     *                             assigned in it)
     * @param onlyOpen             if true, return only the open projects
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
        boolean historyEnabled = false;
        boolean validationEnabled = false;
        boolean blacklistingEnabled = false;
        boolean shaclEnabled = false;
        boolean undoEnabled = false;
        boolean open = false;
        AccessResponse access = null;
        RepositoryLocation repoLocation = new RepositoryLocation(null);
        ProjectStatus status = new ProjectStatus(Status.ok);
        Map<String, String> labels = null;
        String description = null;
        ProjectFacets facets = null;
        String createdAt = null;
        boolean openAtStartup = false;

        if (absProj instanceof Project) {
            Project proj = (Project) absProj;

            baseURI = proj.getBaseURI();
            defaultNamespace = proj.getDefaultNamespace();
            model = proj.getModel().stringValue();
            lexicalizationModel = proj.getLexicalizationModel().stringValue();
            historyEnabled = proj.isHistoryEnabled();
            validationEnabled = proj.isValidationEnabled();
            blacklistingEnabled = proj.isBlacklistingEnabled();
            shaclEnabled = proj.isSHACLEnabled();
            undoEnabled = proj.isUndoEnabled();
            open = ProjectManager.isOpen(proj);
            access = ProjectManager.checkAccessibility(consumer, proj, requestedAccessLevel,
                    requestedLockLevel);
            repoLocation = proj.getDefaultRepositoryLocation();
            labels = proj.getLabels();
            description = proj.getDescription();
            createdAt = proj.getCreatedAt();
            openAtStartup = proj.isOpenAtStartupEnabled();
            if (onlyOpen && !open) {
                return null;
            }
            STUser user = UsersManager.getLoggedUser();
            UsersGroup group = ProjectUserBindingsManager.getUserGroup(user, proj);
            if (userDependent && !ProjectUserBindingsManager
                    .hasUserAccessToProject(user, proj)) {
                return null;
            }
            try {
                facets = (ProjectFacets) exptManager.getSettings(proj, user, group,
                        ProjectFacetsStore.class.getName(), Scope.PROJECT);
            } catch (IllegalStateException | STPropertyAccessException | NoSuchSettingsManager e) {
                facets = new CorruptedProjectFacets(e);
            }

        } else { // absProj instanceof CorruptedProject
            CorruptedProject proj = (CorruptedProject) absProj;
            status = new ProjectStatus(Status.corrupted, proj.getCauseOfCorruption().getMessage());
        }
        return new ProjectInfo(name, open, baseURI, defaultNamespace, model, lexicalizationModel,
                historyEnabled, validationEnabled, blacklistingEnabled, shaclEnabled, undoEnabled, access,
                repoLocation, status, labels, description, createdAt, openAtStartup, facets);
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
     * @param accessLevel  if not provided revoke any access level assigned from the project to the consumer
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
     * @param projectName
     * @param consumerName
     * @param accessLevel  if not provided revoke any access level assigned from the project to the consumer
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
     * @param accessLevel if not provided revoke any universal access level assigned from the project
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
     * @param accessLevel if not provided revoke any universal access level assigned from the project
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
    public void setProjectLabels(String projectName, Map<String, String> labels)
            throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
            ProjectUpdateException {
        Project project = ProjectManager.getProject(projectName, true);
        project.setLabels(labels);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void deleteProject(ProjectConsumer consumer, String projectName) throws ProjectDeletionException,
            ProjectAccessException, ProjectUpdateException, ReservedPropertyUpdateException,
            InvalidProjectNameException, ProjectInexistentException, IOException {
        ProjectManager.deleteProject(projectName);
        // delete the project from the Lucene index as well
        ProjectFacetsIndexUtils.deleteProjectFromFacetIndex(projectName);
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
            ForbiddenProjectAccessException {
        ProjectManager.accessProject(consumer, projectName, requestedAccessLevel, requestedLockLevel);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public Map<String, ExceptionDAO> accessAllProjects(
            @Optional(defaultValue = "SYSTEM") ProjectConsumer consumer,
            @Optional(defaultValue = "RW") ProjectACL.AccessLevel requestedAccessLevel,
            @Optional(defaultValue = "R") ProjectACL.LockLevel requestedLockLevel,
            @Optional(defaultValue = "false") boolean onlyProjectsAtStartup) throws ProjectAccessException {

        Map<String, ExceptionDAO> projectExceptionMap = new HashMap<>();

        // iterate over the existing projects
        Collection<AbstractProject> abstractProjectCollection = ProjectManager
                .listProjects(ProjectConsumer.SYSTEM);
        for (AbstractProject abstractProject : abstractProjectCollection) {
            ProjectInfo projInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, ProjectACL.AccessLevel.R,
                    ProjectACL.LockLevel.NO, false, false, abstractProject);
            if (!projInfo.isOpen()) {
                // if the project is closed, open it, if requested
                try {
                    if (onlyProjectsAtStartup) {
                        // check if this is one of the project that should be open at startup, is so, open in
                        if (projInfo.isOpenAtStartup()) {
                            ProjectManager.accessProject(consumer, projInfo.getName(), requestedAccessLevel,
                                    requestedLockLevel);
                        }
                    } else {
                        ProjectManager.accessProject(consumer, projInfo.getName(), requestedAccessLevel,
                                requestedLockLevel);
                    }
                } catch (InvalidProjectNameException | ProjectInexistentException | ProjectAccessException
                        | ForbiddenProjectAccessException e) {
                    // take note of the problematic project
                    projectExceptionMap.put(projInfo.getName(), ExceptionDAO.valueOf(e));
                }
            }
        }
        return projectExceptionMap;
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void disconnectFromAllProjects(@Optional(defaultValue = "SYSTEM") ProjectConsumer consumer)
            throws ProjectAccessException {
        // iterate over the existing projects
        Collection<AbstractProject> abstractProjectCollection = ProjectManager
                .listProjects(ProjectConsumer.SYSTEM);
        List<ProjectInfo> projInfoList = new ArrayList<>();
        for (AbstractProject abstractProject : abstractProjectCollection) {
            ProjectInfo projInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, ProjectACL.AccessLevel.R,
                    ProjectACL.LockLevel.NO, false, false, abstractProject);
            if (projInfo.isOpen()) {
                // if the project is opened, close it
                String projectName = projInfo.getName();
                ProjectManager.disconnectFromProject(consumer, projectName);
            }
        }
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
    @PreAuthorize("@auth.isAdmin()")
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
     * @param projectName (optional)the project queried for properties
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
     * @param projectName (optional)the project queried for properties
     * @return
     * @throws InvalidProjectNameException
     * @throws ProjectInexistentException
     * @throws ProjectAccessException
     * @throws IOException
     */
    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
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
        exptManager.storeSettings(ProjectFacetsStore.class.getName(), project, UsersManager.getLoggedUser(), null,
                Scope.PROJECT, facets);
        // update the index about the facets of this project
        ProjectInfo projectInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, ProjectACL.AccessLevel.R,
                ProjectACL.LockLevel.NO, false, false, project);
        if (projectInfo == null) {
            throw new ProjectAccessException(projectName);
        }

        ProjectFacetsIndexUtils.recreateFacetIndexForProjectAPI(projectName, projectInfo);
    }

    /**
     * Returns the facets of a project
     *
     * @param projectName
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws IllegalStateException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws ProjectAccessException
     */
    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public ProjectFacets getProjectFacets(String projectName)
            throws IllegalStateException, STPropertyAccessException, NoSuchSettingsManager,
            ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
        Project project = ProjectManager.getProject(projectName, true);
        STUser user = UsersManager.getLoggedUser();
        UsersGroup group = ProjectUserBindingsManager.getUserGroup(user, project);
        return (ProjectFacets) exptManager.getSettings(project, user, group,
                ProjectFacetsStore.class.getName(), Scope.PROJECT);
    }

    /**
     * Returns an uninitialized form for project facets. Differently from {@link #getProjectFacets(String)},
     * this operation doesn't accept a project name as argument nor does it look at the current project
     *
     * @return
     * @throws IllegalStateException
     * @throws STPropertyAccessException
     * @throws NoSuchSettingsManager
     * @throws ProjectAccessException
     * @throws InvalidProjectNameException
     * @throws ProjectInexistentException
     */
    @STServiceOperation
    public ProjectFacets getProjectFacetsForm() throws IllegalStateException, STPropertyAccessException {
        return STPropertiesManager.loadSTPropertiesFromObjectNodes(ProjectFacets.class, false,
                STPropertiesManager.createObjectMapper(exptManager), JsonNodeFactory.instance.objectNode());
    }

    /**
     * Returns the schema of custom project facets
     *
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws IllegalStateException
     */
    @STServiceOperation
    public STPropertiesSchema getCustomProjectFacetsSchema()
            throws IllegalStateException, STPropertyAccessException, NoSuchSettingsManager {
        STUser user = UsersManager.getLoggedUser();
        return (STPropertiesSchema) exptManager.getSettings(null, user, null,
                CustomProjectFacetsSchemaStore.class.getName(), Scope.SYSTEM);
    }

    /**
     * Sets the schema of custom project facets
     *
     * @param facetsSchema
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void setCustomProjectFacetsSchema(ObjectNode facetsSchema)
            throws IllegalStateException, NoSuchSettingsManager, STPropertyUpdateException,
            WrongPropertiesException, STPropertyAccessException {
        exptManager.storeSettings(CustomProjectFacetsSchemaStore.class.getName(), null,
                UsersManager.getLoggedUser(), null, Scope.SYSTEM, facetsSchema);
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
    @PreAuthorize("@auth.isAdmin()")
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
    @PreAuthorize("@auth.isSuperUser(false)")
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
    @PreAuthorize("@auth.isSuperUser(false)")
    public PreloadedDataSummary preloadDataFromURL(URL preloadedDataURL,
            @Optional RDFFormat preloadedDataFormat) throws IOException, RDFParseException,
            RepositoryException, ProfilerException, AssessmentException, STPropertyAccessException {

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
    @PreAuthorize("@auth.isSuperUser(false)")
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

        DataSize dataSize = new DataSize(preloadedDataFile.length(), DataSize.DataUnit.B);

        List<PreloadedDataSummary.PreloadWarning> preloadWarnings = new ArrayList<>();

        if (baseURI == null || model == null || lexicalizationModel == null) {
            CoreSystemSettings coreSystemSettings;
            try {
                coreSystemSettings = (CoreSystemSettings) exptManager.getSettings(null, UsersManager.getLoggedUser(), null, SemanticTurkeyCoreSettingsManager.class.getName(), Scope.SYSTEM);
            } catch (NoSuchSettingsManager e) {
                throw new RuntimeException(e); // this should never happen
            }

            PreloadSettings preloadSettings = java.util.Optional.ofNullable(coreSystemSettings.preload).orElseGet(PreloadSettings::new);
            PreloadProfilerSettings preloadProfilerSettings = java.util.Optional.ofNullable(preloadSettings.profiler).orElseGet(PreloadProfilerSettings::new);

            DataSize profilerDataSizeThreshold = java.util.Optional.ofNullable(preloadProfilerSettings.threshold).orElseGet(() -> new DataSize(1, DataSize.DataUnit.MiB));

            if (dataSize.compareTo(profilerDataSizeThreshold) > 0) { // preloaded data too big to profile
                preloadWarnings = new ArrayList<>(1);
                preloadWarnings
                        .add(new PreloadedDataSummary.ProfilerSizeTresholdExceeded(profilerDataSizeThreshold));
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
                                "http://example.org/" + UUID.randomUUID() + "/void.ttl");
                        IRI dataGraph = vf.createIRI("urn:uuid:" + UUID.randomUUID());

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

    // ** ALL SERVICES AND APIS DEALING WITH THE LUCENE INDEX FOR THE FACETS**//

    @STServiceOperation
    public Map<String, List<String>> getFacetsAndValue() throws IOException {
        Map<String, List<String>> facetValueListMap = new HashMap<>();

        Query query = new MatchAllDocsQuery();
        // classloader magic
        ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
        try {
            int maxResults = ProjectFacetsIndexUtils.MAX_RESULT_QUERY_FACETS;
            IndexSearcher searcher = ProjectFacetsIndexUtils.createSearcher();
            TopDocs topDocs = searcher.search(query, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                for (IndexableField indexableField : doc.getFields()) {
                    String name = indexableField.name();
                    String value = indexableField.stringValue();
                    if (!name.equals(ProjectFacetsIndexUtils.PROJECT_NAME)
                            && !name.equals(ProjectFacetsIndexUtils.PROJECT_DESCRIPTION)) {
                        if (!facetValueListMap.containsKey(name)) {
                            facetValueListMap.put(name, new ArrayList<>());
                        }
                        if (!facetValueListMap.get(name).contains(value)) {
                            facetValueListMap.get(name).add(value);
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
        }

        return facetValueListMap;
    }

    /**
     * Create the Lucene index for the facets in ALL projects
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void createFacetIndex() throws PropertyNotFoundException, InvalidProjectNameException,
            ProjectAccessException, IOException {

        // iterate over the existing projects
        Collection<AbstractProject> abstractProjectCollection = ProjectManager
                .listProjects(ProjectConsumer.SYSTEM);
        List<ProjectInfo> projInfoList = new ArrayList<>();
        for (AbstractProject abstractProject : abstractProjectCollection) {
            ProjectInfo projInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, ProjectACL.AccessLevel.R,
                    ProjectACL.LockLevel.NO, false, false, abstractProject);
            if (projInfo != null) {
                projInfoList.add(projInfo);
            }
        }
        // create the indexes
        ProjectFacetsIndexUtils.createFacetIndexAPI(projInfoList);
    }

    /**
     * Create the Lucene index for the facets in ALL projects
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void recreateFacetIndexForProject(String projectName) throws PropertyNotFoundException,
            InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, IOException {
        Project project = ProjectManager.getProject(projectName);
        ProjectInfo projectInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, ProjectACL.AccessLevel.R,
                ProjectACL.LockLevel.NO, false, false, project);
        if (projectInfo == null) {
            throw new ProjectAccessException(projectName);
        }
        ProjectFacetsIndexUtils.recreateFacetIndexForProjectAPI(projectName, projectInfo);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public Map<String, List<ProjectInfo>> retrieveProjects(@Optional String bagOf,
            @Optional @JsonSerialized List<List<Map<String, Object>>> orQueryList,
            @Optional(defaultValue = "false") boolean userDependent,
            @Optional(defaultValue = "false") boolean onlyOpen)
            throws IOException, InvalidProjectNameException, ProjectAccessException, PropertyNotFoundException {
        Map<String, List<ProjectInfo>> facetToProjeInfoListMap = new HashMap<>();

        // bagOf and query cannot be both empty/null
        if ((bagOf == null || bagOf.isEmpty()) && (orQueryList == null || orQueryList.isEmpty())) {
            throw new IllegalArgumentException("bagOf and orQueryList cannot be both null/empty");
        }

        // classloader magic
        ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
        try {
            List<ProjectInfo> projectInfoList = new ArrayList<>();
            Query queryLuc;
            // if the query is not empty, construct a BooleanQuery
            if (orQueryList == null || orQueryList.isEmpty()) {
                // get all the document (Project)
                queryLuc = new MatchAllDocsQuery();
            } else {
                // prepare a boolean query according to the inpurt query List
                BooleanQuery.Builder orBuilderBoolean = new BooleanQuery.Builder();
                for (List<Map<String, Object>> andQueryList : orQueryList) {
                    BooleanQuery.Builder andBuilderBoolean = new BooleanQuery.Builder();
                    for (Map<String, Object> andQuery : andQueryList) {
                        for (String facetName : andQuery.keySet()) {
                            String facetValue = ProjectFacetsIndexUtils
                                    .normalizeFacetValue(andQuery.get(facetName));
                            andBuilderBoolean.add(new TermQuery(new Term(facetName, facetValue)),
                                    BooleanClause.Occur.MUST);
                        }
                        orBuilderBoolean.add(andBuilderBoolean.build(), BooleanClause.Occur.SHOULD);
                    }
                }
                queryLuc = orBuilderBoolean.build();
            }

            // execute the query
            createFacetIndexIfNeeded();
            int maxResults = ProjectFacetsIndexUtils.MAX_RESULT_QUERY_FACETS;
            IndexSearcher searcher = ProjectFacetsIndexUtils.createSearcher();
            TopDocs topDocs = searcher.search(queryLuc, maxResults);

            List<String> notExistingProjectList = new ArrayList<>();

            // now, order the results according to the facet of the bagOf parameter (if specified)
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                ProjectInfo projectInfo = null;
                String projectName = doc.get(ProjectFacetsIndexUtils.PROJECT_NAME);
                try {
                    Project project = ProjectManager.getProjectDescription(projectName);
                    projectInfo = getProjectInfoHelper(ProjectConsumer.SYSTEM, AccessLevel.R,
                            LockLevel.NO, userDependent, onlyOpen, project);
                } catch (ProjectInexistentException e) {
                    // the project does not exist, so remove it
                    notExistingProjectList.add(projectName);
                }
                if (projectInfo == null) {
                    continue;
                }

                String facetValue;
                if (bagOf != null && !bagOf.isEmpty()) {
                    String facetName = bagOf;
                    facetValue = doc.get(facetName);
                    if (facetValue == null) {
                        facetValue = "";
                    }
                } else {
                    // no need to divide the results
                    facetValue = "";
                }
                if (!facetToProjeInfoListMap.containsKey(facetValue)) {
                    facetToProjeInfoListMap.put(facetValue, new ArrayList<>());
                }
                facetToProjeInfoListMap.get(facetValue).add(projectInfo);
            }

            // remove all the not existing projects from the index
            for (String projectName : notExistingProjectList) {
                ProjectFacetsIndexUtils.deleteProjectFromFacetIndex(projectName);
            }

        } finally {
            Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
        }
        return facetToProjeInfoListMap;
    }

    /**
     * Enables/disables blacklisting in a <em>closed</em> project with <em>validation</em> already enabled
     *
     * @param projectName
     * @param blacklistingEnabled
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws ProjectAccessException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void setBlacklistingEnabled(String projectName, boolean blacklistingEnabled)
            throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
        ProjectManager.handleProjectExclusively(projectName, project -> {
            // checks that validation is already enabled
            if (!project.isValidationEnabled()) {
                throw new IllegalArgumentException(
                        "Cannot enable blacklisting on a project without validation: " + projectName);
            }
            if (project.isBlacklistingEnabled() == blacklistingEnabled)
                return; // nothing to do

            STLocalRepositoryManager prjRepMgr = new STLocalRepositoryManager(project.getProjectDirectory());
            prjRepMgr.init();
            try {
                prjRepMgr.operateOnUnfoldedManager(Project.CORE_REPOSITORY, (repMgr, repId) -> {
                    // updates the repository configuration

                    ValueFactory vf = SimpleValueFactory.getInstance();
                    Model configModel = repMgr.getRepositoryConfig(repId);
                    Resource changeTrackerSailHolder = Models
                            .subject(configModel.filter(null, SailConfigSchema.SAILTYPE,
                                    vf.createLiteral(ChangeTrackerFactory.SAIL_TYPE)))
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Unable to find the ChangeTracker sail in the repository configuration"));
                    Models.setProperty(configModel, changeTrackerSailHolder,
                            ChangeTrackerSchema.BLACKLISTING_ENABLED, vf.createLiteral(blacklistingEnabled));
                    if (blacklistingEnabled) {
                        if (CHANGELOG
                                .isNull(Models
                                        .getPropertyIRI(configModel, changeTrackerSailHolder,
                                                ChangeTrackerSchema.BLACKLIST_GRAPH)
                                        .orElse(CHANGELOG.NULL))) {
                            Models.setProperty(configModel, changeTrackerSailHolder,
                                    ChangeTrackerSchema.BLACKLIST_GRAPH, SUPPORT.BLACKLIST);
                        }
                    }

                    // writes the updated configuration
                    repMgr.addRepositoryConfig(configModel);
                });

                // udpates the project property
                project.setReservedProperty(Project.BLACKLISTING_ENABLED_PROP,
                        Boolean.toString(blacklistingEnabled));
            } catch (ProjectUpdateException e) {
                throw new IllegalStateException(
                        "Unable to update the project properties for setting blacklisting to "
                                + blacklistingEnabled);
            } finally {
                prjRepMgr.shutDown();
            }

        });
    }

    /**
     * Sets whether SHACL validation on commit is enabled in a <em>closed</em> project
     *
     * @param projectName
     * @param shaclValidationEnabled
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void setSHACLValidationEnabled(String projectName, boolean shaclValidationEnabled) throws ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        ProjectManager.handleProjectExclusively(projectName, project -> {
            STLocalRepositoryManager prjRepMgr = new STLocalRepositoryManager(project.getProjectDirectory());
            prjRepMgr.init();
            try {
                prjRepMgr.operateOnUnfoldedManager(Project.CORE_REPOSITORY, (modelBasedRepositoryManager, repId) -> {
                    Model repConfig = modelBasedRepositoryManager.getRepositoryConfig(repId);
                    Resource shaclSail = Models.subject(repConfig.filter(null, SailConfigSchema.SAILTYPE, Values.literal(ShaclSailFactory.SAIL_TYPE))).orElseThrow(() -> new IllegalArgumentException("the SHACL Sail is not configured for the repository"));
                    Models.setProperty(repConfig, shaclSail, ShaclSailSchema.VALIDATION_ENABLED, Values.literal(shaclValidationEnabled));
                    modelBasedRepositoryManager.addRepositoryConfig(repConfig);
                });
            } finally {
                prjRepMgr.shutDown();
            }
        });
    }

    /**
     * Tells whether SHACL validation on commit is enabled in a <em>closed</em> project.
     *
     * @param projectName
     * @return
     */
    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public Boolean isSHACLValidationEnabled(String projectName) throws ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        MutableBoolean validationEnabled = new MutableBoolean(ShaclSailConfig.VALIDATION_ENABLED_DEFAULT);
        ProjectManager.handleProjectExclusively(projectName, project -> {
            STLocalRepositoryManager prjRepMgr = new STLocalRepositoryManager(project.getProjectDirectory());
            prjRepMgr.init();
            try {
                prjRepMgr.operateOnUnfoldedManager(Project.CORE_REPOSITORY, (modelBasedRepositoryManager, repId) -> {
                    Model repConfig = modelBasedRepositoryManager.getRepositoryConfig(repId);
                    Resource shaclSail = Models.subject(repConfig.filter(null, SailConfigSchema.SAILTYPE, Values.literal(ShaclSailFactory.SAIL_TYPE))).orElseThrow(() -> new IllegalArgumentException("the SHACL Sail is not configured for the repository"));
                    boolean v = Models.getPropertyLiteral(repConfig, shaclSail, ShaclSailSchema.VALIDATION_ENABLED).map(l -> Literals.getBooleanValue(l, ShaclSailConfig.VALIDATION_ENABLED_DEFAULT)).orElse(ShaclSailConfig.VALIDATION_ENABLED_DEFAULT);
                    validationEnabled.setValue(v);
                });
            } finally {
                prjRepMgr.shutDown();
            }
        });
        return validationEnabled.toBoolean();
    }

    /**
     * Sets whether undo is enabled in a <em>closed</em> project. Undo can be enabled on projects with history or validation
     * and in any project in which the change tracker happens to be set up (see {@link #isChangeTrackerSetUp(String)}
     *
     * @param projectName
     * @param undoEnabled
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void setUndoEnabled(String projectName, boolean undoEnabled) throws ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        ProjectManager.handleProjectExclusively(projectName, project -> {
            STLocalRepositoryManager prjRepMgr = new STLocalRepositoryManager(project.getProjectDirectory());
            prjRepMgr.init();
            boolean undoEnabledOrig = project.isUndoEnabled();
            try {
                project.setReservedProperty(Project.UNDO_ENABLED_PROP, String.valueOf(undoEnabled));
            } catch (ProjectUpdateException e) {
                ExceptionUtils.rethrow(e);
            }

            try {
                prjRepMgr.operateOnUnfoldedManager(Project.CORE_REPOSITORY, (modelBasedRepositoryManager, repId) -> {
                    Model repConfig = modelBasedRepositoryManager.getRepositoryConfig(repId);
                    Resource changeTrackerSail = Models.subject(repConfig.filter(null, SailConfigSchema.SAILTYPE, Values.literal(ChangeTrackerFactory.SAIL_TYPE))).orElseThrow(() -> new IllegalArgumentException("the ChangeTracker Sail is not configured for the repository"));
                    Models.setProperty(repConfig, changeTrackerSail, ChangeTrackerSchema.UNDO_ENABLED, Values.literal(undoEnabled));
                    try {
                        modelBasedRepositoryManager.addRepositoryConfig(repConfig);
                    } catch (Exception e) {
                        try {
                            project.setReservedProperty(Project.UNDO_ENABLED_PROP, String.valueOf(undoEnabledOrig));
                        } catch (ProjectUpdateException e2) {
                            e.addSuppressed(e2);
                            ExceptionUtils.rethrow(e);
                        }
                    }
                });
            } finally {
                prjRepMgr.shutDown();
            }
        });
    }

    /**
     * Tells whether undo is enabled in a <em>closed</em> project.
     *
     * @param projectName
     * @return
     */
    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public Boolean isUndoEnabled(String projectName) throws ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        MutableBoolean undoEnabled = new MutableBoolean(false);
        ProjectManager.handleProjectExclusively(projectName, project -> {
            STLocalRepositoryManager prjRepMgr = new STLocalRepositoryManager(project.getProjectDirectory());
            prjRepMgr.init();
            try {
                prjRepMgr.operateOnUnfoldedManager(Project.CORE_REPOSITORY, (modelBasedRepositoryManager, repId) -> {
                    Model repConfig = modelBasedRepositoryManager.getRepositoryConfig(repId);
                    Resource changeTrackerSail = Models.subject(repConfig.filter(null, SailConfigSchema.SAILTYPE, Values.literal(ChangeTrackerFactory.SAIL_TYPE))).orElseThrow(() -> new IllegalArgumentException("the ChangeTracker Sail is not configured for the repository"));
                    boolean v = Models.getPropertyLiteral(repConfig, changeTrackerSail, ChangeTrackerSchema.UNDO_ENABLED).map(Literal::booleanValue).orElse(false);
                    undoEnabled.setValue(v);
                });
            } finally {
                prjRepMgr.shutDown();
            }
        });
        return undoEnabled.toBoolean();
    }

    /**
     * Tells whether the change tracker is set up for a <em>closed</em> project.
     *
     * @param projectName
     * @return
     */
    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public Boolean isChangeTrackerSetUp(String projectName) throws ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        MutableBoolean changeTrackerSetup = new MutableBoolean(false);
        ProjectManager.handleProjectExclusively(projectName, project -> {
            STLocalRepositoryManager prjRepMgr = new STLocalRepositoryManager(project.getProjectDirectory());
            prjRepMgr.init();
            try {
                prjRepMgr.operateOnUnfoldedManager(Project.CORE_REPOSITORY, (modelBasedRepositoryManager, repId) -> {
                    Model repConfig = modelBasedRepositoryManager.getRepositoryConfig(repId);
                    changeTrackerSetup.setValue(Models.subject(repConfig.filter(null, SailConfigSchema.SAILTYPE, Values.literal(ChangeTrackerFactory.SAIL_TYPE))).isPresent());
                });
            } finally {
                prjRepMgr.shutDown();
            }
        });
        return changeTrackerSetup.toBoolean();
    }


    /**
     * Enables/Disables the possibility to automatically open a project when SemanticTurkey is executed
     *
     * @param projectName
     * @param openAtStartup
     * @throws InvalidProjectNameException
     * @throws ProjectInexistentException
     * @throws ProjectAccessException
     * @throws ProjectUpdateException
     */
    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void setOpenAtStartup(String projectName, boolean openAtStartup)
            throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException,
            ProjectUpdateException {
        Project project = ProjectManager.getProject(projectName, true);
        project.setReservedProperty(Project.OPEN_AT_STARTUP_PROP, String.valueOf(openAtStartup));
    }

    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public Boolean getOpenAtStartup(String projectName)
            throws InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
        Project project = ProjectManager.getProject(projectName, true);
        return project.isOpenAtStartupEnabled();
    }

    /**
     * Returns the rendering engine associated with a project together with its (optional) configuration
     *
     * @param projectName
     * @return
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws ProjectAccessException
     */
    @PreAuthorize("@auth.isAdmin()")
    @STServiceOperation
    public org.apache.commons.lang3.tuple.Pair<String, STProperties> getRenderingEngineConfiguration(
            String projectName)
            throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
        MutableObject<org.apache.commons.lang3.tuple.Pair<String, STProperties>> rv = new MutableObject<>();
        ProjectManager.handleProjectExclusively(projectName, project -> {
            org.apache.commons.lang3.tuple.Pair<String, STProperties> pair = getBoundComponentConfiguration(
                    project, Project.RENDERING_ENGINE_FACTORY_ID_PROP,
                    Project.RENDERING_ENGINE_CONFIG_FILENAME);

            rv.setValue(pair);

        });
        return rv.getValue();
    }

    /**
     * Updates the configuration of the rendering engine associated with a project
     *
     * @param projectName
     * @param renderingEngineSpecification
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws ProjectAccessException
     */
    @PreAuthorize("@auth.isAdmin()")
    @STServiceOperation(method = RequestMethod.POST)
    public void updateRenderingEngineConfiguration(String projectName,
            PluginSpecification renderingEngineSpecification)
            throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
        ProjectManager.handleProjectExclusively(projectName, project -> {
            try {
                updateBoundComponentConfiguration(project, Project.RENDERING_ENGINE_FACTORY_ID_PROP,
                        Project.RENDERING_ENGINE_CONFIGURATION_TYPE_PROP,
                        Project.RENDERING_ENGINE_CONFIG_FILENAME, renderingEngineSpecification);
            } catch (IOException | ProjectUpdateException e) {
                ExceptionUtils.rethrow(e);
            }
        });
    }

    /**
     * Returns the uri generator associated with a project together with its (optional) configuration
     *
     * @param projectName
     * @return
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws ProjectAccessException
     */
    @PreAuthorize("@auth.isAdmin()")
    @STServiceOperation
    public org.apache.commons.lang3.tuple.Pair<String, STProperties> getURIGeneratorConfiguration(
            String projectName)
            throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
        MutableObject<org.apache.commons.lang3.tuple.Pair<String, STProperties>> rv = new MutableObject<>();
        ProjectManager.handleProjectExclusively(projectName, project -> {
            org.apache.commons.lang3.tuple.Pair<String, STProperties> pair = getBoundComponentConfiguration(
                    project, Project.URI_GENERATOR_FACTORY_ID_PROP, Project.URI_GENERATOR_CONFIG_FILENAME);

            rv.setValue(pair);

        });
        return rv.getValue();
    }

    /**
     * Updates the configuration of the uri generator associated with a project
     *
     * @param projectName
     * @param uriGeneratorSpecification
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws ProjectAccessException
     */
    @PreAuthorize("@auth.isAdmin()")
    @STServiceOperation(method = RequestMethod.POST)
    public void updateURIGeneratorConfiguration(String projectName,
            PluginSpecification uriGeneratorSpecification)
            throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
        ProjectManager.handleProjectExclusively(projectName, project -> {
            try {
                updateBoundComponentConfiguration(project, Project.URI_GENERATOR_FACTORY_ID_PROP,
                        Project.URI_GENERATOR_CONFIGURATION_TYPE_PROP, Project.URI_GENERATOR_CONFIG_FILENAME,
                        uriGeneratorSpecification);
            } catch (IOException | ProjectUpdateException e) {
                ExceptionUtils.rethrow(e);
            }
        });
    }

    protected org.apache.commons.lang3.tuple.Pair<String, STProperties> getBoundComponentConfiguration(
            Project project, String factoryIdProp, String configFilename) throws RuntimeException {
        try {
            String componentID = project.getProperty(factoryIdProp);

            @Nullable
            STProperties config;

            File configFile = new File(project.getProjectDirectory(), configFilename);

            if (configFile.exists()) {
                ConfigurationManager<?> cm = exptManager.getConfigurationManager(componentID);

                Class<? extends Configuration> configBaseClass = ReflectionUtilities
                        .getInterfaceArgumentTypeAsClass(cm.getClass(), ConfigurationManager.class, 0);
                config = STPropertiesManager.loadSTPropertiesFromYAMLFiles(configBaseClass, true, configFile);
            } else {
                config = null;
            }

            return ImmutablePair.of(componentID, config);
        } catch (NoSuchConfigurationManager | STPropertyAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateBoundComponentConfiguration(Project project, String factoryIdProp,
            String configTypeProp, String configFilename, PluginSpecification componentSpec)
            throws IOException, ProjectUpdateException {
        project.setReservedProperty(factoryIdProp, componentSpec.getFactoryId());
        File componentConfigurationFile = new File(project.getProjectDirectory(), configFilename);
        if (componentSpec.getConfiguration() != null) {
            try (FileWriter fw = new FileWriter(componentConfigurationFile)) {
                ObjectNode configuration = componentSpec.getConfiguration();
                if (StringUtils.isNoneBlank(componentSpec.getConfigType())) {
                    configuration = configuration.deepCopy();
                    configuration.put(STPropertiesManager.SETTINGS_TYPE_PROPERTY, componentSpec.getConfigType());
                }
                STPropertiesManager.storeObjectNodeInYAML(configuration,
                        componentConfigurationFile);
            }
        } else {
            componentConfigurationFile.delete();
        }
    }
}