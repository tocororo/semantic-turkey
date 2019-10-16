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
import it.uniroma2.art.semanticturkey.pmki.PendingContributions;
import it.uniroma2.art.semanticturkey.pmki.PmkiConstants;
import it.uniroma2.art.semanticturkey.pmki.PmkiConstants.PmkiRole;
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
import it.uniroma2.art.semanticturkey.services.core.export.TransformationPipeline;
import it.uniroma2.art.semanticturkey.services.core.export.TransformationStep;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
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
	 * - The roles: pmki-public, pmki-pristine, pmki-staging
	 * - The visitor user
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void initPmki() throws IOException, UserException {
		Collection<Role> roles = Arrays.asList(PmkiRole.PRISTINE, PmkiRole.PUBLIC, PmkiRole.STAGING);
		File rolesDir = RBACManager.getRolesDir(null);
		for (Role r : roles) {
			Utilities.copy(Resources.class.getClassLoader()
							.getResourceAsStream("/it/uniroma2/art/semanticturkey/rbac/roles/role_" + r.getName() + ".pl"),
					new File(rolesDir, "role_" + r.getName() + ".pl")
			);
		}
		STUser visitor = new STUser(PmkiConstants.PMKI_VISITOR_EMAIL, PmkiConstants.PMKI_VISITOR_PWD, "Visitor", "PMKI");
		UsersManager.registerUser(visitor);
		UsersManager.updateUserStatus(visitor, UserStatus.ACTIVE);
	}

	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public void testVocbenchConfiguration() throws IOException, STPropertyAccessException {
		RemoteVBConnector vbConnector = new RemoteVBConnector();
		ObjectNode respJson = vbConnector.login();
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
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException, MessagingException {
		exptManager.storeConfiguration(ContributionStore.class.getName(), new Reference(null, null, String.valueOf(System.currentTimeMillis())), configuration);
		PmkiEmailSender.sendContributionSubmittedMail();
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
		exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
		StoredContributionConfiguration config = (StoredContributionConfiguration) exptManager.getConfiguration(
				ContributionStore.class.getName(), reference);
		PmkiEmailSender.sendRejectedContributionMail(reference, config);
	}

	/**
	 * Approves a stable-resource contribution request
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
			ConfigurationNotFoundException, NoSuchConfigurationManager, MessagingException, ProjectDeletionException, STPropertyUpdateException {
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
					failedImports, leftDataset, rightDataset);

			Reference reference = parseReference(configurationReference);

			StoredStableResourceContributionConfiguration contribution =
					(StoredStableResourceContributionConfiguration) exptManager.getConfiguration(
							ContributionStore.class.getName(), reference);

			//TODO write also metadata get from the contribution

			/* Set the project status to "pristine" (by assigning the pristine role to the visitor) */

			STUser visitor = UsersManager.getUserByEmail(PmkiConstants.PMKI_VISITOR_EMAIL);
			ProjectUserBindingsManager.addRoleToPUBinding(visitor, newProject, PmkiRole.PRISTINE);

			/* send email notification to the contributor */
			//generate a random token
			String token = new BigInteger(130, new SecureRandom()).toString(32);
			//add the pair token-project to the pending contribution
			new PendingContributions().addPendingContribution(token, projectName);

			String loadPageLink = pmkiHostAddress + "#/load/" + token;
			PmkiEmailSender.sendAcceptedStableResourceContributionMail(reference, contribution, projectName, loadPageLink);

			//In order to support the testing, the deletion of the configuration is temporarily skipped TODO restore
			//exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
		} catch (MessagingException e) {
			//failed to send email, it will not be possible to complete the contribution => delete the project just created
			ProjectManager.disconnectFromProject(ProjectConsumer.SYSTEM, projectName);
			ProjectManager.deleteProject(projectName);
			throw new MessagingException("Failed to send an email notification to the contributor, please verify the email configuration");
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void loadStableContributionData(String token, String projectName, MultipartFile file, String format)
			throws STPropertyAccessException, IOException, InvalidConfigurationException, WrongPropertiesException,
			LiftingException, ProjectBindingException, STPropertyUpdateException {
		Project project = ProjectManager.getProject(projectName);
		if (project == null) {
			throw new IllegalArgumentException("Invalid project name '" + projectName + "'. It is not an open project or it does not exist");
		}

		PendingContributions pendingContributions = new PendingContributions();
		String contribProjName = pendingContributions.getPendingContributionProject(token);

		if (contribProjName == null) { //wrong token
			throw new IllegalArgumentException("The contribution you're trying to complete does not exist. It might be expired");
		} else if (!projectName.equals(contribProjName)) { //token ok, wrong project name
			throw new IllegalArgumentException("The provided project name does not correspond to the contribution you're trying to complete");
		} else { //token+projectName are ok
			//Load data exploiting the InputOutput service class
			TransformationPipeline pipeline = new TransformationPipeline(new TransformationStep[0]);
			inputOutputService.loadRDF(file, project.getBaseURI(), format, TransitiveImportMethodAllowance.web, null, null, pipeline, false);

			//Update the status of the project from pristine to staging
			STUser visitor = UsersManager.getUserByEmail(PmkiConstants.PMKI_VISITOR_EMAIL);
			ProjectUserBindingsManager.removeAllRoleFromPUBinding(visitor, project);
			ProjectUserBindingsManager.addRoleToPUBinding(visitor, project, PmkiRole.STAGING);

			//remove the stored pending contribution
			pendingContributions.removePendingContribution(token);
		}
	}

	/**
	 * Approves a development-resource contribution request
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveDevelopmentContribution(String projectName, IRI model, IRI lexicalizationModel, String baseURI,
			PluginSpecification coreRepoSailConfigurerSpecification, String configurationReference, String pmkiHostAddress)
			throws IOException, WrongPropertiesException, STPropertyAccessException, ConfigurationNotFoundException, NoSuchConfigurationManager, URISyntaxException {

		Reference reference = parseReference(configurationReference);
		StoredDevResourceContributionConfiguration contribution =
				(StoredDevResourceContributionConfiguration) exptManager.getConfiguration(
						ContributionStore.class.getName(), reference);

		/*
		* with http requests to the SemanticTurkey for staging project:
		* - login as admin
		* - create project
		* - create a user (email the one of the user and a random password)
		* - activate the user
		* - assign a role to the user in the project
		*/
		RemoteVBConnector vbConnector = new RemoteVBConnector();
		vbConnector.login();
		vbConnector.createProject(projectName, baseURI, model, lexicalizationModel, coreRepoSailConfigurerSpecification);
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?";
		String tempUserPwd = RandomStringUtils.random(15, characters);
		//TODO check if user already registered
		vbConnector.registerUser(contribution.contributorEmail, tempUserPwd, contribution.contributorName, contribution.contributorLastName, contribution.contributorOrganization);
		vbConnector.enableUser(contribution.contributorEmail);
		vbConnector.addRolesToUser(projectName, contribution.contributorEmail, Collections.singletonList(DefaultRole.ONTOLOGIST)); //TODO is ok ontologist?

		/* send email notification to the contributor */
//		//generate a random token
//		String token = new BigInteger(130, new SecureRandom()).toString(32);
//		//add the pair token-project to the pending contribution
//		new PendingContributions().addPendingContribution(token, projectName);
//
//		String loadPageLink = pmkiHostAddress + "#/load/" + token;
//		PmkiEmailSender.sendAcceptedDevResourceContributionMail(reference, contribution, projectName, loadPageLink, null, tempUserPwd);

		//In order to support the testing, the deletion of the configuration is temporarily skipped
		//exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);


	}

	/**
	 * Approves a metadata contribution request
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveMetadataContribution(String configurationReference) throws MetadataRegistryWritingException, NoSuchConfigurationManager, WrongPropertiesException, ConfigurationNotFoundException, STPropertyAccessException, IOException {

		Reference reference = parseReference(configurationReference);

		StoredMetadataContributionConfiguration config =
				(StoredMetadataContributionConfiguration) exptManager.getConfiguration(
						ContributionStore.class.getName(), reference);

		Boolean dereferenciability = null;
		if (config.dereferenciationSystem != null) {
			if (config.dereferenciationSystem.equals(METADATAREGISTRY.STANDARD_DEREFERENCIATION)) {
				dereferenciability = true;
			} else if (config.dereferenciationSystem.equals(METADATAREGISTRY.NO_DEREFERENCIATION)) {
				dereferenciability = false;
			}
		}
		IRI record = metadataRegistryBackend.addDataset(null, config.uriSpace, config.resourceName, dereferenciability, config.sparqlEndpoint);
		try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
			Model model = QueryResults.asModel(conn.getStatements(record, FOAF.PRIMARY_TOPIC, null));
			IRI datasetIRI = Models.objectIRI(model).orElse(null);
			if (!config.sparqlLimitations.isEmpty()) { //if it's not empty, set just the first since at the moment we have just a limitation (aggregation)
				metadataRegistryBackend.setSPARQLEndpointLimitation(datasetIRI, config.sparqlLimitations.iterator().next());
			}

		}

		//In order to support the testing, the deletion of the configuration is temporarily skipped TODO restore
		//exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);

	}

}
