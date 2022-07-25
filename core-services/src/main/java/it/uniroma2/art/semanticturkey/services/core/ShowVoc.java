package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.contribution.ContributionStore;
import it.uniroma2.art.semanticturkey.config.contribution.StoredContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredDevResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredMetadataContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredStableResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.email.ShowVocEmailService;
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
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DatasetSearchResult;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.DownloadDescription;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.FacetAggregation;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.FacetAggregation.Bucket;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SearchResultsPage;
import it.uniroma2.art.semanticturkey.extension.extpts.datasetcatalog.SelectionMode;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LiftingException;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.mdr.core.Distribution;
import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.mdr.core.vocabulary.METADATAREGISTRY;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STPropertiesSerializer;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.properties.dynamic.STPropertiesSchema;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACManager.DefaultRole;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationPipeline;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationStep;
import it.uniroma2.art.semanticturkey.settings.facets.CustomProjectFacetsSchemaStore;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacets;
import it.uniroma2.art.semanticturkey.settings.facets.ProjectFacetsIndexUtils;
import it.uniroma2.art.semanticturkey.showvoc.PendingContribution;
import it.uniroma2.art.semanticturkey.showvoc.PendingContributionStore;
import it.uniroma2.art.semanticturkey.showvoc.RemoteVBConnector;
import it.uniroma2.art.semanticturkey.showvoc.ShowVocConstants;
import it.uniroma2.art.semanticturkey.showvoc.ShowVocConstants.ShowVocRole;
import it.uniroma2.art.semanticturkey.showvoc.ShowVocConversionFormat;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@STService
public class ShowVoc extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(ShowVoc.class);

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.services.core.ShowVoc";
		public static final String model$displayName = keyBase + ".model.displayName";
		public static final String lexicalizationModel$displayName = keyBase
				+ ".lexicalizationModel.displayName";
	}

	@Autowired
	private ExtensionPointManager exptManager;
	@Autowired
	private STMetadataRegistryBackend metadataRegistryBackend;
	@Autowired
	private InputOutput inputOutputService;
	@Autowired
	private Projects projectsService;

	/* ADMINISTRATION */

	/**
	 * Initializes all that stuff needed in ShowVoc.
	 * - The roles: showvoc-public, showvoc-pristine, showvoc-staging
	 * - The visitor user
	 *
	 * @throws IOException
	 * @throws UserException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void initShowVoc() throws IOException, UserException, RoleCreationException, ProjectAccessException {
		Collection<Role> roles = Arrays.asList(ShowVocRole.PRISTINE, ShowVocRole.PUBLIC, ShowVocRole.STAGING);
		File rolesDir = RBACManager.getRolesDir(null);
		for (Role r : roles) {
			if (!RBACManager.roleExists(null, r.getName())) {
				File targetRoleFile = new File(rolesDir, "role_" + r.getName() + ".pl");
				Utilities.copy(
						Resources.class.getClassLoader().getResourceAsStream(
								"/it/uniroma2/art/semanticturkey/rbac/roles/role_" + r.getName() + ".pl"),
						targetRoleFile);
				RBACManager.addSystemRole(r.getName(), targetRoleFile);
			}
		}
		STUser visitor = new STUser(ShowVocConstants.SHOWVOC_VISITOR_EMAIL, ShowVocConstants.SHOWVOC_VISITOR_PWD,
				"Visitor", "ShowVoc");
		UsersManager.registerUser(visitor);
		UsersManager.updateUserStatus(visitor, UserStatus.ACTIVE);
	}

	/**
	 * @throws IOException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public void testVocbenchConfiguration()
			throws IOException, STPropertyAccessException, URISyntaxException {
		RemoteVBConnector vbConnector = new RemoteVBConnector();
		ObjectNode respJson = vbConnector.loginAdmin();
		/*
		 * I noticed that in a particular case, even if the stHost is not correct, the login request completes
		 * successfully. E.g. if the stHost is http://mypath.it/semanticturkey/ it means that the complete
		 * login url will be http://mypath.it/semanticturkey/ +
		 * semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Auth/login that is not correct
		 * (notice the double semanticturkey/semanticturkey/. Anyway, the login is performed. I guess this is
		 * a problem linked with spring-security that processes in a "strange" way the following:
		 * login-processing-url="/it.uniroma2.art.semanticturkey/st-core-services/Auth/login" in
		 * spring-security.xml In order to avoid such case, if the login goes ok, a further GET request is
		 * performed
		 */
		vbConnector.getRemoteAccessConfigurations();
		// if also getRemoteAccessConfigurations complete successfully, check if the logged-in user was an
		// admin
		boolean isAdmin = respJson.findValue("admin").asBoolean();
		if (!isAdmin) {
			throw new IllegalArgumentException(
					"Configuration is correct, but the provided credentials don't belong to an administrator user");
		}
	}

	/* CONTRIBUTION MANAGEMENT */

	/**
	 * Returns the available configuration in the ContributionStore
	 *
	 * @return the references of the contribution configurations
	 */
	@STServiceOperation
	public Collection<Reference> getContributionReferences() throws NoSuchConfigurationManager {
		return exptManager.getConfigurationReferences(null, UsersManager.getLoggedUser(),
				ContributionStore.class.getName());
	}

	/**
	 * @param configuration
	 *            a json object node (key-value) representing the configuration to store
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void submitContribution(ObjectNode configuration) throws NoSuchConfigurationManager, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException,
			MessagingException, ConfigurationNotFoundException {
		Reference contribRef = new Reference(null, null, String.valueOf(System.currentTimeMillis()));
		exptManager.storeConfiguration(ContributionStore.class.getName(), contribRef, configuration);
		StoredContributionConfiguration contribution = (StoredContributionConfiguration) exptManager
				.getConfiguration(ContributionStore.class.getName(), contribRef);
		new ShowVocEmailService().sendContributionSubmittedMail(contribution);
	}

	/**
	 * @param relativeReference
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void rejectContribution(String relativeReference)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, WrongPropertiesException,
			IOException, STPropertyAccessException, MessagingException {
		Reference reference = parseReference(relativeReference);
		StoredContributionConfiguration config = (StoredContributionConfiguration) exptManager
				.getConfiguration(ContributionStore.class.getName(), reference);
		exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
		new ShowVocEmailService().sendRejectedContributionMail(reference, config);
	}

	/**
	 * Approves a stable-resource contribution request
	 *
	 * @param projectName
	 * @param model
	 * @param lexicalizationModel
	 * @param baseURI
	 * @param repositoryAccess
	 * @param coreRepoSailConfigurerSpecification
	 * @param configurationReference
	 * @param showvocHostAddress
	 * @throws IOException
	 * @throws RBACException
	 * @throws WrongPropertiesException
	 * @throws ProjectBindingException
	 * @throws ProjectInconsistentException
	 * @throws ClassNotFoundException
	 * @throws ForbiddenProjectAccessException
	 * @throws UnsupportedModelException
	 * @throws ProjectUpdateException
	 * @throws InvalidConfigurationException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 * @throws UnsupportedLexicalizationModelException
	 * @throws ProjectInexistentException
	 * @throws ProjectCreationException
	 * @throws ReservedPropertyUpdateException
	 * @throws DuplicatedResourceException
	 * @throws STPropertyAccessException
	 * @throws ConfigurationNotFoundException
	 * @throws NoSuchConfigurationManager
	 * @throws MessagingException
	 * @throws ProjectDeletionException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveStableContribution(String projectName, IRI model, IRI lexicalizationModel,
			String baseURI, RepositoryAccess repositoryAccess,
			PluginSpecification coreRepoSailConfigurerSpecification, String configurationReference,
			String showvocHostAddress)
			throws IOException, RBACException, WrongPropertiesException, ProjectBindingException,
			ProjectInconsistentException, ClassNotFoundException, ForbiddenProjectAccessException,
			UnsupportedModelException, ProjectUpdateException,
			InvalidConfigurationException, InvalidProjectNameException, ProjectAccessException, UnsupportedLexicalizationModelException,
			ProjectInexistentException, ProjectCreationException, ReservedPropertyUpdateException,
			DuplicatedResourceException, STPropertyAccessException, ConfigurationNotFoundException,
			NoSuchConfigurationManager, MessagingException, ProjectDeletionException,
			STPropertyUpdateException, MetadataRegistryWritingException, UserException {
		try {
			/* create project for hosting the stable resource */
			ProjectConsumer consumer = ProjectConsumer.SYSTEM;

			boolean historyEnabled = false;
			boolean validationEnabled = false;
			boolean blacklistingEnabled = false;

			String coreRepoID = projectName + "_core";
			String coreBackendType = null;

			String supportRepoID = null;
			PluginSpecification supportRepoSailConfigurerSpecification = null;
			String supportBackendType = null;

			String extensionID = "it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGenerator";
			String configType = "it.uniroma2.art.semanticturkey.extension.impl.urigen.template.NativeTemplateBasedURIGeneratorConfiguration";
			ObjectNode configObj = JsonNodeFactory.instance.objectNode();
			configObj.put("@type", configType);
			PluginSpecification uriGeneratorSpecification = new PluginSpecification(extensionID, null, null, configObj);

			PluginSpecification renderingEngineSpecification = BaseRenderingEngine
					.getRenderingEngineSpecificationForLexicalModel(lexicalizationModel)
					.orElseThrow(() -> new IllegalArgumentException(
							"Unsupported lexicalization model: " + lexicalizationModel));

			List<Pair<RDFResourceRole, String>> resourceMetadataAssociations = null;
			File preloadedDataFile = null;
			RDFFormat preloadedDataFormat = null;
			TransitiveImportMethodAllowance transitiveImportAllowance = null;
			Set<IRI> failedImports = new HashSet<>();
			String leftDataset = null;
			String rightDataset = null;

			Project newProject = ProjectManager.createProject(consumer, projectName, null, model,
					lexicalizationModel, baseURI.trim(), historyEnabled, validationEnabled,
					blacklistingEnabled, repositoryAccess, coreRepoID, coreRepoSailConfigurerSpecification,
					coreBackendType, supportRepoID, supportRepoSailConfigurerSpecification,
					supportBackendType, uriGeneratorSpecification, renderingEngineSpecification,
					resourceMetadataAssociations, preloadedDataFile, preloadedDataFormat,
					transitiveImportAllowance, failedImports, leftDataset, rightDataset, false, null, false,
					false, null, false);

			Reference reference = parseReference(configurationReference);

			StoredStableResourceContributionConfiguration contribution = (StoredStableResourceContributionConfiguration) exptManager
					.getConfiguration(ContributionStore.class.getName(), reference);

			newProject.setProperty(Project.DESCRIPTION_PROP, contribution.description);

			// write also metadata get from the contribution
			writeMetadataInRegistry(contribution);

			/* Set the project status to "pristine" (by assigning the pristine role to the visitor) */

			STUser visitor = UsersManager.getUser(ShowVocConstants.SHOWVOC_VISITOR_EMAIL);
			ProjectUserBindingsManager.addRoleToPUBinding(visitor, newProject, ShowVocRole.PRISTINE);

			/* send email notification to the contributor */
			// generate a random token
			String token = new BigInteger(130, new SecureRandom()).toString(32);
			// add the pair token-project to the pending contribution
			new PendingContributionStore().addPendingContribution(token, projectName,
					contribution.contributorEmail, contribution.contributorName,
					contribution.contributorLastName);

			new ShowVocEmailService().sendAcceptedStableResourceContributionMail(reference, contribution,
					projectName, showvocHostAddress, token);

			// contribution approved => delete it from the store
			exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
		} catch (MessagingException e) {
			// failed to send email, it will not be possible to complete the contribution => delete the
			// project just created
			ProjectManager.disconnectFromProject(ProjectConsumer.SYSTEM, projectName);
			ProjectManager.deleteProject(projectName);
			throw new MessagingException(
					"Failed to send an email notification to the contributor, please verify the email configuration");
		}
	}

	/**
	 * Approves a development-resource contribution request
	 *
	 * @param projectName
	 * @param model
	 * @param lexicalizationModel
	 * @param baseURI
	 * @param coreRepoSailConfigurerSpecification
	 * @param configurationReference
	 * @param showvocHostAddress
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyAccessException
	 * @throws ConfigurationNotFoundException
	 * @throws NoSuchConfigurationManager
	 * @throws URISyntaxException
	 * @throws STPropertyUpdateException
	 * @throws MessagingException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveDevelopmentContribution(String projectName, IRI model, IRI lexicalizationModel,
			String baseURI, PluginSpecification coreRepoSailConfigurerSpecification,
			String configurationReference, String showvocHostAddress) throws IOException,
			WrongPropertiesException, STPropertyAccessException, ConfigurationNotFoundException,
			NoSuchConfigurationManager, URISyntaxException, MessagingException {

		Reference reference = parseReference(configurationReference);
		StoredDevResourceContributionConfiguration contribution = (StoredDevResourceContributionConfiguration) exptManager
				.getConfiguration(ContributionStore.class.getName(), reference);

		/*
		 * with http requests to the SemanticTurkey for staging project: - login as admin - create project
		 * According the kind of conversion required by the load - In case of spreadsheet conversion - create
		 * a user (email the one of the user and a random password) - activate the user - assign a role to the
		 * user in the project - send an email that redirect directly to the VB instance including the
		 * credentials - Otherwise (Zthes or Tbx conversion, or not conversion at all): - send an email with
		 * the link to a load page in ShowVoc - (only after the load, the user will be created, enabled, assigned
		 * to the project, and the email with the VB instance will be send)
		 */
		RemoteVBConnector vbConnector = new RemoteVBConnector();
		vbConnector.loginAdmin();
		vbConnector.createProject(projectName, baseURI, model, lexicalizationModel,
				coreRepoSailConfigurerSpecification);

		ShowVocEmailService svEmailService = new ShowVocEmailService();
		if (contribution.format == ShowVocConversionFormat.EXCEL) {
			/*
			 * The load data of a contribution that requires the conversion from an excel file is performed
			 * directly from the VB instance.
			 */
			String userPassword = createRemoteUser(vbConnector, contribution.contributorEmail,
					contribution.contributorName, contribution.contributorLastName,
					contribution.contributorOrganization);
			//this shouldn't be needed since from v11.0, createUser automatically enable the new user, anyway I leave it in case remote VB uses an old version
			vbConnector.enableUser(contribution.contributorEmail);
			vbConnector.addRolesToUser(projectName, contribution.contributorEmail,
					Collections.singletonList(DefaultRole.RDF_GEEK)); // rdf geek required for sheet2rdf
			svEmailService.sendAcceptedDevExcelResourceContributionMail(reference, contribution,
					projectName, vbConnector.getVocbenchUrl(), userPassword);
		} else {
			/*
			 * The load data of a contribution that requires no conversion at all (data already in rdf), or
			 * conversion from zthes or tbx, is performed on ShowVoc
			 */
			// generate a random token
			String token = new BigInteger(130, new SecureRandom()).toString(32);
			svEmailService.sendAcceptedDevGenericResourceContributionMail(reference, contribution,
					projectName, showvocHostAddress, token);
			// add the pair token-project to the pending contribution
			new PendingContributionStore().addPendingContribution(token, projectName,
					contribution.contributorEmail, contribution.contributorName,
					contribution.contributorLastName);
		}

		// contribution approved => delete it from the store
		exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
	}

	/**
	 * Approves a metadata contribution request
	 *
	 * @param configurationReference
	 * @throws MetadataRegistryWritingException
	 * @throws NoSuchConfigurationManager
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws STPropertyAccessException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveMetadataContribution(String configurationReference)
			throws MetadataRegistryWritingException, NoSuchConfigurationManager, WrongPropertiesException,
			ConfigurationNotFoundException, STPropertyAccessException, IOException, MessagingException {
		Reference reference = parseReference(configurationReference);

		StoredMetadataContributionConfiguration config = (StoredMetadataContributionConfiguration) exptManager
				.getConfiguration(ContributionStore.class.getName(), reference);

		writeMetadataInRegistry(config);

		new ShowVocEmailService().sendAcceptedMetadataContributionMail(reference, config);
		// contribution approved => delete it from the store
		exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
	}

	/**
	 * @param token
	 * @param projectName
	 * @param inputFile
	 * @param format
	 * @throws STPropertyAccessException
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 * @throws WrongPropertiesException
	 * @throws LiftingException
	 * @throws ProjectBindingException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public void loadStableContributionData(String token, String projectName, String contributorEmail,
			MultipartFile inputFile, String format, PluginSpecification rdfLifterSpec,
			TransitiveImportMethodAllowance transitiveImportAllowance) throws STPropertyAccessException,
			IOException, InvalidConfigurationException, WrongPropertiesException, LiftingException,
			ProjectBindingException, MessagingException, UserException {
		Project project = ProjectManager.getProject(projectName);
		if (project == null) {
			throw new IllegalArgumentException("Invalid project name '" + projectName
					+ "'. It is not an open project or it does not exist");
		}

		PendingContributionStore pendingContributionStore = new PendingContributionStore();
		PendingContribution pendingContrib = pendingContributionStore.getPendingContribution(token);

		if (pendingContrib == null) { // wrong token
			throw new IllegalArgumentException(
					"The contribution you're trying to complete does not exist or it might be expired");
		} else if (!projectName.equals(pendingContrib.getProjectName())) { // token ok, wrong project name
			throw new IllegalArgumentException(
					"The provided project name does not correspond to the contribution you're trying to complete");
		} else if (!contributorEmail.equals(pendingContrib.getContributorEmail())) { // token ok, wrong
																						// contributor email
			throw new IllegalArgumentException(
					"The provided email does not correspond to the contribution you're trying to complete");
		} else { // token+projectName+contributorEmail are ok
			// Load data exploiting the InputOutput service class
			TransformationPipeline pipeline = new TransformationPipeline(new TransformationStep[0]);
			inputOutputService.loadRDFInternal(inputFile, project.getBaseURI(), format,
					transitiveImportAllowance, getManagedConnection(), null, rdfLifterSpec, pipeline, false);

			// Update the status of the project from pristine to staging
			STUser visitor = UsersManager.getUser(ShowVocConstants.SHOWVOC_VISITOR_EMAIL);
			ProjectUserBindingsManager.removeAllRoleFromPUBinding(visitor, project);
			ProjectUserBindingsManager.addRoleToPUBinding(visitor, project, ShowVocRole.STAGING);

			// remove the stored pending contribution
			pendingContributionStore.removePendingContribution(token);

			new ShowVocEmailService().sendLoadedStableResourceContributionMail(projectName,
					pendingContrib.getContributorName(), pendingContrib.getContributorLastName(),
					pendingContrib.getContributorEmail());
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void loadDevContributionData(String token, String projectName, String contributorEmail,
			MultipartFile inputFile, String format, PluginSpecification rdfLifterSpec,
			TransitiveImportMethodAllowance transitiveImportAllowance) throws STPropertyAccessException,
			IOException, URISyntaxException, MessagingException {

		PendingContributionStore pendingContributionStore = new PendingContributionStore();
		PendingContribution pendingContrib = pendingContributionStore.getPendingContribution(token);

		if (pendingContrib == null) { // wrong token
			throw new IllegalArgumentException(
					"The contribution you're trying to complete does not exist or it might be expired");
		} else if (!projectName.equals(pendingContrib.getProjectName())) { // token ok, wrong project name
			throw new IllegalArgumentException(
					"The provided project name does not correspond to the contribution you're trying to complete");
		} else if (!contributorEmail.equals(pendingContrib.getContributorEmail())) { // token ok, wrong
																						// contributor email
			throw new IllegalArgumentException(
					"The provided email does not correspond to the contribution you're trying to complete");
		} else { // token+projectName+contributorEmail are ok
			/*
			 * On the remote VB instance - Login as admin - load the data - create the user, enable it and
			 * assign to the project
			 */
			RemoteVBConnector vbConnector = new RemoteVBConnector();
			vbConnector.loginAdmin();

			ObjectNode projInfoJson = vbConnector.getProjectInfo(projectName);
			String baseURI = projInfoJson.findValue("baseURI").asText();

			File tempServerFile = File.createTempFile("loadRdf", inputFile.getOriginalFilename());
			inputFile.transferTo(tempServerFile);

			vbConnector.loadRDF(projectName, baseURI, tempServerFile, format, rdfLifterSpec,
					transitiveImportAllowance, false);

			String userPassword = createRemoteUser(vbConnector, contributorEmail,
					pendingContrib.getContributorName(), pendingContrib.getContributorLastName(), null);
			//this shouldn't be needed since from v11.0, createUser automatically enable the new user, anyway I leave it in case remote VB uses an old version
			vbConnector.enableUser(contributorEmail);
			vbConnector.addRolesToUser(projectName, contributorEmail,
					Collections.singletonList(DefaultRole.RDF_GEEK));
			new ShowVocEmailService().sendLoadedDevGenericResourceContributionMail(projectName,
					vbConnector.getVocbenchUrl(), contributorEmail, userPassword);

			// remove the stored pending contribution
			pendingContributionStore.removePendingContribution(token);
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setProjectStatus(String projectName, String status) throws ProjectBindingException,
			InvalidProjectNameException, ProjectInexistentException, ProjectAccessException, UserException {
		STUser visitor = UsersManager.getUser(ShowVocConstants.SHOWVOC_VISITOR_EMAIL);
		Project project = ProjectManager.getProjectDescription(projectName);

		ProjectUserBindingsManager.removeAllRoleFromPUBinding(visitor, project);
		Role role = RBACManager.getRole(null, status);
		if (role == null) {
			throw new IllegalArgumentException("'" + status + "' is not a valid role");
		}
		ProjectUserBindingsManager.addRoleToPUBinding(visitor, project, role);
	}

	/**
	 * Registers and enables a user on the remove ST of the VB instance. Returns the password of the user if
	 * it is created, null if the user already existed
	 * 
	 * @param vbConnector
	 * @param email
	 * @param givenName
	 * @param familyName
	 * @param organization
	 * @return
	 */
	private String createRemoteUser(RemoteVBConnector vbConnector, String email, String givenName,
			String familyName, String organization) {
		String userPassword = null;
		try { // surrounded in a try catch since the user email could be already used => user already
				// registered
			String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*?";
			String tempUserPwd = RandomStringUtils.random(15, characters);
			//this shouldn't be needed since from v11.0, createUser automatically enable the new user, anyway I leave it in case remote VB uses an old version
			vbConnector.createUser(email, tempUserPwd, givenName, familyName, organization);
			vbConnector.enableUser(email);
			userPassword = tempUserPwd;
		} catch (IOException e) {
			// user already registered
		}
		return userPassword;
	}

	private void writeMetadataInRegistry(StoredMetadataContributionConfiguration config)
			throws MetadataRegistryWritingException {
		Boolean dereferenciability = null;
		if (config.dereferenciationSystem != null) {
			if (config.dereferenciationSystem.equals(METADATAREGISTRY.STANDARD_DEREFERENCIATION)) {
				dereferenciability = true;
			} else if (config.dereferenciationSystem.equals(METADATAREGISTRY.NO_DEREFERENCIATION)) {
				dereferenciability = false;
			}
		}
		String uriSpace = config.uriSpace != null ? config.uriSpace : config.baseURI.stringValue();
		Distribution datasetDistribution = new Distribution(config.identity, METADATAREGISTRY.SPARQL_ENDPOINT, config.sparqlEndpoint, null);
		IRI datasetIRI = metadataRegistryBackend.createConcreteDataset(null, uriSpace, Values.literal(config.resourceName), null,
				dereferenciability, datasetDistribution, null, false);
		try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
			if (config.sparqlLimitations != null && !config.sparqlLimitations.isEmpty()) {
				// if it's not empty, set just the first since at the moment we have just a limitation
				// (aggregation)
				metadataRegistryBackend.setSPARQLEndpointLimitation(datasetIRI,
						config.sparqlLimitations.iterator().next());
			}
		}
	}

	public static final int PAGE_SIZE = 10;

	@STServiceOperation /* No @PreAuthorize("...") since ST doesn't support project-less capabilities */
	public SearchResultsPage<DatasetSearchResult> searchDataset(String query,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "{}") @JsonSerialized Map<String, List<String>> facets,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "1") int page)
			throws IOException, InvalidProjectNameException, ProjectInexistentException,
			ProjectAccessException, PropertyNotFoundException, IllegalStateException,
			STPropertyAccessException, NoSuchSettingsManager, UserException {
		query = query.trim();
		if (StringUtils.isAllBlank(query)) {
			return new SearchResultsPage<>(0, PAGE_SIZE, page + 1, Collections.emptyList(),
					Collections.emptyList());
		}

		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Builder tokenQueryBuilder = new BooleanQuery.Builder(); // (0, BooleanClause.Occur.SHOULD);

			String[] queryTokens = query.split(" ");
			for (String tok : queryTokens) {
				tokenQueryBuilder.add(new TermQuery(new Term(ProjectFacetsIndexUtils.PROJECT_NAME, tok)),
						Occur.SHOULD);
				tokenQueryBuilder.add(
						new TermQuery(new Term(ProjectFacetsIndexUtils.PROJECT_DESCRIPTION, tok)),
						Occur.SHOULD);
			}

			BooleanQuery tokenQuery = tokenQueryBuilder.build();
			Builder overallQueryBuilder = new BooleanQuery.Builder(); // (0, BooleanClause.Occur.SHOULD);
			overallQueryBuilder.add(tokenQuery, Occur.MUST);

			for (String facet : facets.keySet()) {
				Builder facetQueryBuilder = new BooleanQuery.Builder(); // (0, BooleanClause.Occur.SHOULD);
				for (String fv : facets.get(facet)) {
					facetQueryBuilder.add(new TermQuery(new Term(facet, fv)), Occur.SHOULD);
				}
				BooleanClause facetClause = new BooleanClause(facetQueryBuilder.build(), Occur.MUST);
				overallQueryBuilder.add(facetClause);
			}

			BooleanQuery overallQuery = overallQueryBuilder.build();

			projectsService.createFacetIndexIfNeeded();

			int maxResults = ProjectFacetsIndexUtils.MAX_RESULT_QUERY_FACETS;
			IndexSearcher searcher = ProjectFacetsIndexUtils.createSearcher();
			TopDocs topDocs = searcher.search(overallQuery, maxResults);

			List<DatasetSearchResult> searchResults = new ArrayList<>();

			ValueFactory vf = SimpleValueFactory.getInstance();

			Map<String, FacetAggregation> facetAggregations = new HashMap<>();

			for (ScoreDoc sd : topDocs.scoreDocs) {
				Document doc = searcher.doc(sd.doc);

				String id = doc.get(ProjectFacetsIndexUtils.PROJECT_NAME);

				Project proj = ProjectManager.getProject(id);

				if (proj == null) continue; // skip closed projects

				boolean isPublic = ProjectUserBindingsManager
						.getPUBinding(UsersManager.getUser(ShowVocConstants.SHOWVOC_VISITOR_EMAIL), proj).getRoles()
						.contains(ShowVocRole.PUBLIC);

				if (!isPublic)
					continue; // skip non public projects

				Map<String, List<String>> facetValues = new HashMap<>();
				extractFacetsValues(facetAggregations, facetValues, doc);

				IRI ontologyIRI = vf.createIRI(proj.getBaseURI());
				double score = sd.score;
				URL datasetPage = null;
				List<Literal> titles = Collections.singletonList(vf.createLiteral(proj.getName()));
				List<Literal> descriptions = StringUtils.isNoneBlank(proj.getDescription())
						? Collections.singletonList(vf.createLiteral(proj.getDescription()))
						: Collections.emptyList();
				DatasetSearchResult result = new DatasetSearchResult(id, ontologyIRI, score, datasetPage,
						titles, descriptions, facetValues);

				searchResults.add(result);
			}

			int totalResults = searchResults.size();
			List<DatasetSearchResult> searchResultsPage = searchResults.stream().skip(page * PAGE_SIZE)
					.limit(PAGE_SIZE).collect(Collectors.toList());
			return new SearchResultsPage<>(totalResults, PAGE_SIZE, page + 1, searchResultsPage,
					facetAggregations.values().stream().collect(Collectors.toList()));
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	private void extractFacetsValues(Map<String, FacetAggregation> facetAggregations,
			Map<String, List<String>> facetValues, Document doc)
			throws PropertyNotFoundException, STPropertyAccessException, NoSuchSettingsManager {
		STPropertiesSchema customFacetsSchema = (STPropertiesSchema) exptManager.getSettings(null,
				UsersManager.getLoggedUser(), null, CustomProjectFacetsSchemaStore.class.getName(), Scope.SYSTEM);
		STProperties customProjectsFacetsForm = customFacetsSchema.toSTProperties();
		STProperties stdProjectFacetsForm = new ProjectFacets();

		extractFacetsValues(facetAggregations, facetValues, doc, stdProjectFacetsForm,
				customProjectsFacetsForm);
	}

	private void extractFacetsValues(Map<String, FacetAggregation> facetAggregations,
			Map<String, List<String>> facetValues, Document doc, STProperties stdProjectFactsForm,
			STProperties customProjectFacetsForm) throws PropertyNotFoundException {

		Collection<String> stdFacetNames = stdProjectFactsForm.getProperties();
		Collection<String> customFacetNames = customProjectFacetsForm.getProperties();

		for (IndexableField field : doc.getFields()) {
			@Nullable
			String fieldValue = field.stringValue();
			if (StringUtils.isAllBlank(fieldValue))
				continue; // skip blank field

			String fieldName = field.name();

			String display;

			if (Objects.equals(fieldName, ProjectFacetsIndexUtils.PROJECT_MODEL)) {
				display = STMessageSource.getMessage(MessageKeys.model$displayName);
			} else if (Objects.equals(fieldName, ProjectFacetsIndexUtils.PROJECT_LEX_MODEL)) {
				display = STMessageSource.getMessage(MessageKeys.lexicalizationModel$displayName);
			} else if (stdFacetNames.contains(fieldName)) {
				display = STPropertiesSerializer
						.interpolate(stdProjectFactsForm.getPropertyDisplayName(fieldName));
			} else if (customFacetNames.contains(fieldName)) {
				display = STPropertiesSerializer
						.interpolate(customProjectFacetsForm.getPropertyDisplayName(fieldName));
			} else {
				continue; // skip unrecognized facet
			}

			FacetAggregation aggregation = facetAggregations.computeIfAbsent(fieldName, n -> {
				try {
					FacetAggregation aggr = new FacetAggregation(n, display, SelectionMode.single,
							new ArrayList<>(), false);
					return aggr;
				} catch (Exception e) {
					ExceptionUtils.rethrow(e);
					return null;
				}
			});
			List<Bucket> buckets = aggregation.getBuckets();
			Bucket bucket = buckets.stream().filter(b -> Objects.equals(b.getName(), fieldValue)).findAny()
					.orElseGet(() -> {
						Bucket newB = new Bucket(fieldValue, fieldValue, 0);
						buckets.add(newB);
						return newB;
					});
			bucket.setCount(bucket.getCount() + 1);

			facetValues.put(aggregation.getDisplayName(), Collections.singletonList(fieldValue));

		}
	}

	@STServiceOperation
	@PreAuthorize("@auth.isProjectPublic(#id)") /*
												 * No capability check in @PreAuthorize("...") since ST
												 * doesn't support project-less capabilities
												 */
	public DatasetDescription describeDataset(String id,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional String apiBaseURL,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional String frontendBaseURL)
			throws STPropertyAccessException, NoSuchSettingsManager, PropertyNotFoundException,
			IllegalArgumentException, IOException {
		Project proj = ProjectManager.getProject(id);
		if (proj == null) {
			throw new IllegalArgumentException("Invalid id: " + id);
		}

		ValueFactory vf = SimpleValueFactory.getInstance();

		IRI ontologyIRI = vf.createIRI(proj.getBaseURI());
		URL datasetPage = StringUtils.isNoneBlank(frontendBaseURL, proj.getName())
				? new URL(frontendBaseURL + "#/datasets/" + proj.getName())
				: null;
		List<Literal> titles = Collections.singletonList(vf.createLiteral(proj.getName()));
		List<Literal> descriptions = StringUtils.isNoneBlank(proj.getDescription())
				? Collections.singletonList(vf.createLiteral(proj.getDescription()))
				: Collections.emptyList();
		Map<String, List<String>> facetValues = new HashMap<>();

		Document doc = ProjectFacetsIndexUtils.getDocumentForProject(id)
				.orElseThrow(() -> new IllegalArgumentException("Dataset not indexed: " + id));

		extractFacetsValues(new HashMap<>(), facetValues, doc);
		String uriPrefix = proj.getDefaultNamespace();
		List<DownloadDescription> dataDumps;
		if (apiBaseURL != null) {
			dataDumps = Collections.singletonList(new DownloadDescription(
					UriComponentsBuilder
							.fromHttpUrl(apiBaseURL + "it.uniroma2.art.semanticturkey/st-core-services/"
									+ "ShowVoc/dataDump")
							.queryParam("ctx_project", proj.getName()).queryParam("format", "Turtle")
							.build(false).encode().toUri().toURL(),
					Collections.emptyList(), Collections.emptyList(), RDFFormat.TURTLE.getDefaultMIMEType()));
		} else {
			dataDumps = Collections.emptyList();
		}
		URL sparqlEndpoint = null;
		IRI model = proj.getModel();
		IRI lexicalizationModel = proj.getLexicalizationModel();
		DatasetDescription datasetDescription = new DatasetDescription(id, ontologyIRI, datasetPage, titles,
				descriptions, facetValues, uriPrefix, dataDumps, sparqlEndpoint, model, lexicalizationModel);

		return datasetDescription;
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isCtxProjectPublic()")
	public void dataDump(HttpServletResponse oRes,
			@it.uniroma2.art.semanticturkey.services.annotations.Optional(defaultValue = "Turtle") RDFFormat format)
			throws Exception {
		Export.exportHelper(exptManager, stServiceContext, oRes, getManagedConnection(),
				new IRI[] { (IRI) getWorkingGraph() }, new TransformationPipeline(new TransformationStep[0]),
				false, format.getName(), true, null, null);
	}
}
