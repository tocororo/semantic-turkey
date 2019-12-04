package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.contribution.ContributionStore;
import it.uniroma2.art.semanticturkey.config.contribution.StoredContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredDevResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredMetadataContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredStableResourceContributionConfiguration;
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
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LiftingException;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.pmki.PendingContribution;
import it.uniroma2.art.semanticturkey.pmki.PendingContributionStore;
import it.uniroma2.art.semanticturkey.pmki.PmkiConstants;
import it.uniroma2.art.semanticturkey.pmki.PmkiConstants.PmkiRole;
import it.uniroma2.art.semanticturkey.pmki.PmkiConversionFormat;
import it.uniroma2.art.semanticturkey.pmki.PmkiEmailSender;
import it.uniroma2.art.semanticturkey.pmki.RemoteVBConnector;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.rbac.RBACManager.DefaultRole;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationPipeline;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationStep;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.RoleCreationException;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.vocabulary.METADATAREGISTRY;
import org.apache.commons.lang.RandomStringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@STService
public class PMKI extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(PMKI.class);

	@Autowired
	private ExtensionPointManager exptManager;
	@Autowired
	private MetadataRegistryBackend metadataRegistryBackend;
	@Autowired
	private InputOutput inputOutputService;



	/* ADMINISTRATION */

	/**
	 * Initializes all that stuff needed in PMKI.
	 * * - The roles: pmki-public, pmki-pristine, pmki-staging
	 * * - The visitor user
	 *
	 * @throws IOException
	 * @throws UserException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void initPmki() throws IOException, UserException, RoleCreationException, ProjectAccessException, ProjectBindingException {
		Collection<Role> roles = Arrays.asList(PmkiRole.PRISTINE, PmkiRole.PUBLIC, PmkiRole.STAGING);
		File rolesDir = RBACManager.getRolesDir(null);
		for (Role r : roles) {
			File targetRoleFile = new File(rolesDir, "role_" + r.getName() + ".pl");
			Utilities.copy(Resources.class.getClassLoader()
							.getResourceAsStream("/it/uniroma2/art/semanticturkey/rbac/roles/role_" + r.getName() + ".pl"),
					targetRoleFile
			);
			RBACManager.addSystemRole(r.getName(), targetRoleFile);
		}
		STUser visitor = new STUser(PmkiConstants.PMKI_VISITOR_EMAIL, PmkiConstants.PMKI_VISITOR_PWD, "Visitor", "PMKI");
		UsersManager.registerUser(visitor);
		UsersManager.updateUserStatus(visitor, UserStatus.ACTIVE);
	}

	/**
	 * @throws IOException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public void testVocbenchConfiguration() throws IOException, STPropertyAccessException, URISyntaxException {
		RemoteVBConnector vbConnector = new RemoteVBConnector();
		ObjectNode respJson = vbConnector.loginAdmin();
		/*
		 * I noticed that in a particular case, even if the stHost is not correct, the login request completes successfully.
		 * E.g. if the stHost is http://mypath.it/semanticturkey/ it means that the complete login url will be
		 * http://mypath.it/semanticturkey/ + semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Auth/login
		 * that is not correct (notice the double semanticturkey/semanticturkey/. Anyway, the login is performed.
		 * I guess this is a problem linked with spring-security that processes in a "strange" way the following:
		 * login-processing-url="/it.uniroma2.art.semanticturkey/st-core-services/Auth/login"
		 * in spring-security.xml
		 * In order to avoid such case, if the login goes ok, a further GET request is performed
		 */
		vbConnector.getRemoteAccessConfigurations();
		//if also getRemoteAccessConfigurations complete successfully, check if the logged-in user was an admin
		boolean isAdmin = respJson.findValue("admin").asBoolean();
		if (!isAdmin) {
			throw new IllegalArgumentException(
					"Configuration is correct, but the provided credentials don't belong to an administrator user");
		}
	}

	/**
	 * @param mailTo
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public void testEmailConfig(String mailTo) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		PmkiEmailSender.sendTestMailConfiguration(mailTo);
	}

	/* CONTRIBUTION MANAGEMENT */

	/**
	 * Returns the available configuration in the ContributionStore
	 *
	 * @return the references of the contribution configurations
	 */
	@STServiceOperation
	public Collection<Reference> getContributionReferences() throws NoSuchConfigurationManager {
		return exptManager.getConfigurationReferences(null, UsersManager.getLoggedUser(), ContributionStore.class.getName());
	}

	/**
	 * @param configuration a json object node (key-value) representing the configuration to store
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void submitContribution(ObjectNode configuration) throws NoSuchConfigurationManager, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException, MessagingException,
			ConfigurationNotFoundException {
		Reference contribRef = new Reference(null, null, String.valueOf(System.currentTimeMillis()));
		exptManager.storeConfiguration(ContributionStore.class.getName(), contribRef, configuration);
		StoredContributionConfiguration contribution = (StoredContributionConfiguration)
				exptManager.getConfiguration(ContributionStore.class.getName(), contribRef);
		PmkiEmailSender.sendContributionSubmittedMail(contribution);
	}

	/**
	 * @param relativeReference
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void rejectContribution(String relativeReference)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, WrongPropertiesException, IOException, STPropertyAccessException, MessagingException {
		Reference reference = parseReference(relativeReference);
		StoredContributionConfiguration config = (StoredContributionConfiguration) exptManager.getConfiguration(
				ContributionStore.class.getName(), reference);
		exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
		PmkiEmailSender.sendRejectedContributionMail(reference, config);
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
	 * @param pmkiHostAddress
	 * @throws IOException
	 * @throws RBACException
	 * @throws WrongPropertiesException
	 * @throws ProjectBindingException
	 * @throws ProjectInconsistentException
	 * @throws ClassNotFoundException
	 * @throws ForbiddenProjectAccessException
	 * @throws UnsupportedModelException
	 * @throws UnsupportedPluginConfigurationException
	 * @throws ProjectUpdateException
	 * @throws InvalidConfigurationException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 * @throws UnloadablePluginConfigurationException
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
	public void approveStableContribution(String projectName, IRI model, IRI lexicalizationModel, String baseURI,
			RepositoryAccess repositoryAccess, PluginSpecification coreRepoSailConfigurerSpecification,
			String configurationReference, String pmkiHostAddress)
			throws IOException, RBACException, WrongPropertiesException, ProjectBindingException,
			ProjectInconsistentException, ClassNotFoundException, ForbiddenProjectAccessException,
			UnsupportedModelException, UnsupportedPluginConfigurationException, ProjectUpdateException,
			InvalidConfigurationException, InvalidProjectNameException, ProjectAccessException,
			UnloadablePluginConfigurationException, UnsupportedLexicalizationModelException, ProjectInexistentException,
			ProjectCreationException, ReservedPropertyUpdateException, DuplicatedResourceException, STPropertyAccessException,
			ConfigurationNotFoundException, NoSuchConfigurationManager, MessagingException, ProjectDeletionException,
			STPropertyUpdateException, MetadataRegistryWritingException {
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

			PluginSpecification uriGeneratorSpecification = new PluginSpecification(
					"it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory",
					null, new Properties(), null);
			uriGeneratorSpecification.expandDefaults();

			String renderingEngineFactoryID = Project.determineBestRenderingEngine(lexicalizationModel);
			PluginSpecification renderingEngineSpecification = new PluginSpecification(renderingEngineFactoryID, null, new Properties(), null);
			renderingEngineSpecification.expandDefaults();

			IRI creationDateProperty = null;
			IRI modificationDateProperty = null;
			String[] updateForRoles = new String[]{"resource"};
			File preloadedDataFile = null;
			RDFFormat preloadedDataFormat = null;
			TransitiveImportMethodAllowance transitiveImportAllowance = null;
			Set<IRI> failedImports = new HashSet<>();
			String leftDataset = null;
			String rightDataset = null;

			Project newProject = ProjectManager.createProject(consumer, projectName, model, lexicalizationModel, baseURI.trim(),
					historyEnabled, validationEnabled, blacklistingEnabled, repositoryAccess, coreRepoID,
					coreRepoSailConfigurerSpecification, coreBackendType, supportRepoID,
					supportRepoSailConfigurerSpecification, supportBackendType, uriGeneratorSpecification,
					renderingEngineSpecification, creationDateProperty, modificationDateProperty,
					updateForRoles, preloadedDataFile, preloadedDataFormat, transitiveImportAllowance,
					failedImports, leftDataset, rightDataset, false);

			Reference reference = parseReference(configurationReference);

			StoredStableResourceContributionConfiguration contribution =
					(StoredStableResourceContributionConfiguration) exptManager.getConfiguration(
							ContributionStore.class.getName(), reference);

			//write also metadata get from the contribution
			writeMetadataInRegistry(contribution);

			/* Set the project status to "pristine" (by assigning the pristine role to the visitor) */

			STUser visitor = UsersManager.getUserByEmail(PmkiConstants.PMKI_VISITOR_EMAIL);
			ProjectUserBindingsManager.addRoleToPUBinding(visitor, newProject, PmkiRole.PRISTINE);

			/* send email notification to the contributor */
			//generate a random token
			String token = new BigInteger(130, new SecureRandom()).toString(32);
			//add the pair token-project to the pending contribution
			new PendingContributionStore().addPendingContribution(token, projectName, contribution.contributorEmail,
					contribution.contributorName, contribution.contributorLastName);

			PmkiEmailSender.sendAcceptedStableResourceContributionMail(reference, contribution, projectName, pmkiHostAddress, token);

			//contribution approved => delete it from the store
			exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
		} catch (MessagingException e) {
			//failed to send email, it will not be possible to complete the contribution => delete the project just created
			ProjectManager.disconnectFromProject(ProjectConsumer.SYSTEM, projectName);
			ProjectManager.deleteProject(projectName);
			throw new MessagingException("Failed to send an email notification to the contributor, please verify the email configuration");
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
	 * @param pmkiHostAddress
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
	public void approveDevelopmentContribution(String projectName, IRI model, IRI lexicalizationModel, String baseURI,
			PluginSpecification coreRepoSailConfigurerSpecification, String configurationReference, String pmkiHostAddress)
			throws IOException, WrongPropertiesException, STPropertyAccessException, ConfigurationNotFoundException, NoSuchConfigurationManager, URISyntaxException, STPropertyUpdateException, MessagingException {

		Reference reference = parseReference(configurationReference);
		StoredDevResourceContributionConfiguration contribution =
				(StoredDevResourceContributionConfiguration) exptManager.getConfiguration(
						ContributionStore.class.getName(), reference);

		/*
		 * with http requests to the SemanticTurkey for staging project:
		 * - login as admin
		 * - create project
		 * According the kind of conversion required by the load
		 * - In case of spreadsheet conversion
		 * 		- create a user (email the one of the user and a random password)
		 * 		- activate the user
		 * 		- assign a role to the user in the project
		 * 		- send an email that redirect directly to the VB instance including the credentials
		 * - Otherwise (Zthes or Tbx conversion, or not conversion at all):
		 * 		- send an email with the link to a load page in PMKI
		 * 		- (only after the load, the user will be created, enabled, assigned to the project,
		 * 			and the email with the VB instance will be send)
		 */
		RemoteVBConnector vbConnector = new RemoteVBConnector();
		vbConnector.loginAdmin();
		vbConnector.createProject(projectName, baseURI, model, lexicalizationModel, coreRepoSailConfigurerSpecification);

		if (contribution.format == PmkiConversionFormat.EXCEL) {
			/*
			 * The load data of a contribution that requires the conversion from an excel file is performed
			 * directly from the VB instance.
			 */
			String userPassword = createRemoteUser(vbConnector, contribution.contributorEmail,
					contribution.contributorName, contribution.contributorLastName, contribution.contributorOrganization);
			vbConnector.enableUser(contribution.contributorEmail);
			vbConnector.addRolesToUser(projectName, contribution.contributorEmail, Collections.singletonList(DefaultRole.RDF_GEEK)); //rdf geek required for sheet2rdf
			PmkiEmailSender.sendAcceptedDevExcelResourceContributionMail(reference, contribution, projectName,
					vbConnector.getVocbenchUrl(), userPassword);
		} else {
			/*
			 * The load data of a contribution that requires no conversion at all (data already in rdf),
			 * or conversion from zthes or tbx, is performed on PMKI
			 */
			//generate a random token
			String token = new BigInteger(130, new SecureRandom()).toString(32);
			PmkiEmailSender.sendAcceptedDevGenericResourceContributionMail(reference, contribution, projectName,
					pmkiHostAddress, token);
			//add the pair token-project to the pending contribution
			new PendingContributionStore().addPendingContribution(token, projectName, contribution.contributorEmail,
					contribution.contributorName, contribution.contributorLastName);
		}

		//contribution approved => delete it from the store
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

		StoredMetadataContributionConfiguration config =
				(StoredMetadataContributionConfiguration) exptManager.getConfiguration(
						ContributionStore.class.getName(), reference);

		writeMetadataInRegistry(config);

		PmkiEmailSender.sendAcceptedMetadataContributionMail(reference, config);
		//contribution approved => delete it from the store
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
			ProjectBindingException, STPropertyUpdateException, MessagingException {
		Project project = ProjectManager.getProject(projectName);
		if (project == null) {
			throw new IllegalArgumentException("Invalid project name '" + projectName + "'. It is not an open project or it does not exist");
		}

		PendingContributionStore pendingContributionStore = new PendingContributionStore();
		PendingContribution pendingContrib = pendingContributionStore.getPendingContribution(token);

		if (pendingContrib == null) { //wrong token
			throw new IllegalArgumentException("The contribution you're trying to complete does not exist or it might be expired");
		} else if (!projectName.equals(pendingContrib.getProjectName())) { //token ok, wrong project name
			throw new IllegalArgumentException("The provided project name does not correspond to the contribution you're trying to complete");
		} else if (!contributorEmail.equals(pendingContrib.getContributorEmail())) { //token ok, wrong contributor email
			throw new IllegalArgumentException("The provided email does not correspond to the contribution you're trying to complete");
		} else { //token+projectName+contributorEmail are ok
			//Load data exploiting the InputOutput service class
			TransformationPipeline pipeline = new TransformationPipeline(new TransformationStep[0]);
			inputOutputService.loadRDFInternal(inputFile, project.getBaseURI(), format, transitiveImportAllowance,
					getManagedConnection(), null, rdfLifterSpec, pipeline);

			//Update the status of the project from pristine to staging
			STUser visitor = UsersManager.getUserByEmail(PmkiConstants.PMKI_VISITOR_EMAIL);
			ProjectUserBindingsManager.removeAllRoleFromPUBinding(visitor, project);
			ProjectUserBindingsManager.addRoleToPUBinding(visitor, project, PmkiRole.STAGING);

			//remove the stored pending contribution
			pendingContributionStore.removePendingContribution(token);

			PmkiEmailSender.sendLoadedStableResourceContributionMail(projectName, pendingContrib.getContributorName(),
					pendingContrib.getContributorLastName(), pendingContrib.getContributorEmail());
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void loadDevContributionData(String token, String projectName, String contributorEmail, MultipartFile inputFile,
			String format, PluginSpecification rdfLifterSpec, TransitiveImportMethodAllowance transitiveImportAllowance)
			throws STPropertyAccessException, IOException, STPropertyUpdateException, URISyntaxException, MessagingException {

		PendingContributionStore pendingContributionStore = new PendingContributionStore();
		PendingContribution pendingContrib = pendingContributionStore.getPendingContribution(token);

		if (pendingContrib == null) { //wrong token
			throw new IllegalArgumentException("The contribution you're trying to complete does not exist or it might be expired");
		} else if (!projectName.equals(pendingContrib.getProjectName())) { //token ok, wrong project name
			throw new IllegalArgumentException("The provided project name does not correspond to the contribution you're trying to complete");
		} else if (!contributorEmail.equals(pendingContrib.getContributorEmail())) { //token ok, wrong contributor email
			throw new IllegalArgumentException("The provided email does not correspond to the contribution you're trying to complete");
		} else { //token+projectName+contributorEmail are ok
			/*
			* On the remove VB instance
			* - Login as admin
			* - load the data
			* - create the user, enable it and assign to the project
			*/
			RemoteVBConnector vbConnector = new RemoteVBConnector();
			vbConnector.loginAdmin();

			ObjectNode projInfoJson = vbConnector.getProjectInfo(projectName);
			String baseURI = projInfoJson.findValue("baseURI").asText();

			File tempServerFile = File.createTempFile("loadRdf", inputFile.getOriginalFilename());
			inputFile.transferTo(tempServerFile);

			vbConnector.loadRDF(projectName, baseURI, tempServerFile, format, rdfLifterSpec, transitiveImportAllowance);

			String userPassword = createRemoteUser(vbConnector, contributorEmail,
					pendingContrib.getContributorName(), pendingContrib.getContributorLastName(), null);
			vbConnector.enableUser(contributorEmail);
			vbConnector.addRolesToUser(projectName, contributorEmail, Collections.singletonList(DefaultRole.RDF_GEEK));
			PmkiEmailSender.sendLoadedDevGenericResourceContributionMail(projectName, vbConnector.getVocbenchUrl(), contributorEmail, userPassword);

			//remove the stored pending contribution
			pendingContributionStore.removePendingContribution(token);
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void setProjectStatus(String projectName, String status) throws ProjectBindingException, InvalidProjectNameException, ProjectInexistentException, ProjectAccessException {
		STUser visitor = UsersManager.getUserByEmail(PmkiConstants.PMKI_VISITOR_EMAIL);
		Project project = ProjectManager.getProjectDescription(projectName);

		ProjectUserBindingsManager.removeAllRoleFromPUBinding(visitor, project);
		Role role = RBACManager.getRole(null, status);
		if (role == null) {
			throw new IllegalArgumentException("'" + status + "' is not a valid role");
		}
		ProjectUserBindingsManager.addRoleToPUBinding(visitor, project, role);
	}


	/**
	 * Registers and enables a user on the remove ST of the VB instance.
	 * Returns the password of the user if it is created, null if the user already existed
	 * @param vbConnector
	 * @param email
	 * @param givenName
	 * @param familyName
	 * @param organization
	 * @return
	 */
	private String createRemoteUser(RemoteVBConnector vbConnector, String email, String givenName, String familyName, String organization) {
		String userPassword = null;
		try { //surrounded in a try catch since the user email could be already used => user already registered
			String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*?";
			String tempUserPwd = RandomStringUtils.random(15, characters);
			vbConnector.registerUser(email, tempUserPwd, givenName, familyName, organization);
			vbConnector.enableUser(email);
			userPassword = tempUserPwd;
		} catch (IOException e) {
			//user already registered
		}
		return userPassword;
	}

	private void writeMetadataInRegistry(StoredMetadataContributionConfiguration config) throws MetadataRegistryWritingException {
		Boolean dereferenciability = null;
		if (config.dereferenciationSystem != null) {
			if (config.dereferenciationSystem.equals(METADATAREGISTRY.STANDARD_DEREFERENCIATION)) {
				dereferenciability = true;
			} else if (config.dereferenciationSystem.equals(METADATAREGISTRY.NO_DEREFERENCIATION)) {
				dereferenciability = false;
			}
		}
		String uriSpace = config.uriSpace != null ? config.uriSpace : config.baseURI.stringValue();
		IRI record = metadataRegistryBackend.addDataset(config.identity, uriSpace, config.resourceName, dereferenciability, config.sparqlEndpoint);
		try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
			Model model = QueryResults.asModel(conn.getStatements(record, FOAF.PRIMARY_TOPIC, null));
			IRI datasetIRI = Models.objectIRI(model).orElse(null);
			if (config.sparqlLimitations != null && !config.sparqlLimitations.isEmpty()) {
				//if it's not empty, set just the first since at the moment we have just a limitation (aggregation)
				metadataRegistryBackend.setSPARQLEndpointLimitation(datasetIRI, config.sparqlLimitations.iterator().next());
			}
		}
	}

}
