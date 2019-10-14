package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.contribution.ContributionStore;
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
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.rbac.RBACManager;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryWritingException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.Role.RoleLevel;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UserStatus;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.EmailSender;
import it.uniroma2.art.semanticturkey.utilities.Utilities;
import it.uniroma2.art.semanticturkey.vocabulary.METADATAREGISTRY;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
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

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

@STService
public class PMKI extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(PMKI.class);

	@Autowired
	private ExtensionPointManager exptManager;
	@Autowired
	private MetadataRegistryBackend metadataRegistryBackend;

	private static final String PMKI_VISITOR_EMAIL = "pmki@pmki.eu";
	private static final String PMKI_VISITOR_PWD = "pmki";

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
		STUser visitor = new STUser(PMKI_VISITOR_EMAIL, PMKI_VISITOR_PWD, "Visitor", "PMKI");
		UsersManager.registerUser(visitor);
		UsersManager.updateUserStatus(visitor, UserStatus.ACTIVE);
	}

	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public void testVocbenchConfiguration() throws IOException, STPropertyAccessException {
		String vbConfValue = STPropertiesManager.getSystemSetting(STPropertiesManager.SETTING_VB_CONFIG_FOR_PMKI);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode confJson = mapper.readTree(vbConfValue);
		String stHost = confJson.get("stHost").textValue();
		String adminEmail = confJson.get("adminEmail").textValue();
		String adminPwd = confJson.get("adminPassword").textValue();

		String loginUrl = stHost;
		if (!stHost.endsWith("/")) {
			loginUrl += "/";
		}
		loginUrl += "semanticturkey/it.uniroma2.art.semanticturkey/st-core-services/Auth/login";

		HttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(loginUrl);
		//Request parameters and other properties
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("email", adminEmail));
		params.add(new BasicNameValuePair("password", adminPwd));
		httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		//Execute and get the response.
		HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			String responseAsString = EntityUtils.toString(entity);
			Header contentType = entity.getContentType();
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
				if (responseAsString.isEmpty()) { //probably sub-path is wrong (ST responds but there is nothing under that path) => empty response
					throw new IllegalArgumentException("Invalid SemanticTurkey host URL");
				} else { //wrong credentials
					throw new IllegalArgumentException(responseAsString);
				}
			} else if (statusCode == HttpStatus.SC_OK) {
				if (contentType.getValue().equals("application/json")) {
					JsonNode respJson = mapper.readTree(responseAsString);
					boolean isAdmin = respJson.findValue("admin").asBoolean();
					if (!isAdmin) {
						throw new IllegalArgumentException(
								"Configuration is correct, but the provided credentials don't belong to an administrator user");
					}
				}
			} else { //other => wrong ST host (this should never happen, if the host is wrong a RuntimeException is thrown by httpClient.execute())
				throw new IllegalArgumentException("Invalid SemanticTurkey host URL");
			}
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
	//TODO auth with capability of the pmki-public user (just rdf(r)?)
	public void submitContribution(ObjectNode configuration) throws NoSuchConfigurationManager, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException {
		exptManager.storeConfiguration(ContributionStore.class.getName(), new Reference(null, null, String.valueOf(System.currentTimeMillis())), configuration);
		//TODO send an email to the administrator?
	}

	/**
	 * @param relativeReference
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void deleteContribution(String relativeReference)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException {
		exptManager.deleteConfiguraton(ContributionStore.class.getName(), parseReference(relativeReference));
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
			ProjectCreationException, ReservedPropertyUpdateException, DuplicatedResourceException, STPropertyAccessException, ConfigurationNotFoundException, NoSuchConfigurationManager, MessagingException, ProjectDeletionException {

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

			StoredStableResourceContributionConfiguration config =
					(StoredStableResourceContributionConfiguration) exptManager.getConfiguration(
							ContributionStore.class.getName(), reference);

			//TODO write also metadata get from the contribution

			/* send email notification to the contributor */

			String emailTo = config.contributorEmail;
			String emailContent = "Dear " + config.contributorName + " " + config.contributorLastName + ",\n" +
					"your request to contribute to PMKI with the resource '" + config.resourceName + "' has been approved"; //TODO continue
			System.out.println("EmailTo: " + emailTo);
			System.out.println("Email content: " + emailContent);

			STUser visitor = UsersManager.getUserByEmail(PMKI_VISITOR_EMAIL);
			ProjectUserBindingsManager.addRoleToPUBinding(visitor, newProject, PmkiRole.PRISTINE);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			String formattedDate = sdf.format(new Date(Long.parseLong(reference.getIdentifier())));
			//generate a random token
			SecureRandom random = new SecureRandom();
			String token = new BigInteger(130, random).toString(32); //TODO store the pair projectName-token somewhere
			String loadPageLink = pmkiHostAddress + "/#/load/" + token;

			String mailContent = "Dear " + config.contributorName + " " + config.contributorLastName + ",\n" +
					"Your contribution request submitted at " + formattedDate + " has been accepted. " +
					"You can now upload the RDF resource at the following link " + loadPageLink;
			EmailSender.sendMail(config.contributorEmail, "Contribution approved", mailContent);

			/*
			 * TODO (IN A DEDICATED SERVICE) once the resource is uploaded,
			 *  remove the "pmki-pristine" role to the visitor and give him the "pmki-staging"
			 *  remove the stored pair project-token
			 */

			//In order to support the testing, the deletion of the configuration is temporarily skipped TODO restore
			//exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);
		} catch (MessagingException e) {
			//failed to send email, it will not be possible to complete the contribution => delete the project just created
			ProjectManager.disconnectFromProject(ProjectConsumer.SYSTEM, projectName);
			ProjectManager.deleteProject(projectName);
			throw new MessagingException("Failed to send an email notification to the contributor, please verify the email configuration");
		}
	}

	/**
	 * Approves a development-resource contribution request
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveDevelopmentContribution(String projectName, IRI model, IRI lexicalizationModel, String baseURI,
											   RepositoryAccess repositoryAccess, PluginSpecification coreRepoSailConfigurerSpecification, String configurationReference)
			throws IOException, WrongPropertiesException, STPropertyAccessException, ConfigurationNotFoundException, NoSuchConfigurationManager {

		/*
		 * TODO
		 * 	- with http requests to the SemanticTurkey for staging project:
		 * 		- do the login as admin
		 * 		- create project
		 * 		- create a user (email the one of the user and random password), activate it and assign a role (which one???) to the project
		 * 	- send email to the user with...
		 * 		According PMKI-108
		 * 		"an email is sent to the contributor with the token and the upload page (which includes the conversion options).
		 * 		Once the upload is done, the page provides the URL of Vocbench and the access credentials for it."
		 * 		I don't agree with this, I think it is better to simply send the credentials and
		 * 		let the user upload/convert the resource directly in VB
		 */

		Reference reference = parseReference(configurationReference);

		StoredDevResourceContributionConfiguration config =
				(StoredDevResourceContributionConfiguration) exptManager.getConfiguration(
						ContributionStore.class.getName(), reference);
		String emailTo = config.contributorEmail;
		String emailContent = "Dear " + config.contributorName + " " + config.contributorLastName + ",\n" +
				"your request to contribute to PMKI with the resource '" + config.resourceName + "' has been approved"; //TODO continue
		System.out.println("EmailTo: " + emailTo);
		System.out.println("Email content: " + emailContent);

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
			//TODO ask Manuel: sparqlLimitations is a Set, but the setter accept an IRI, why?
//			metadataRegistryBackend.setSPARQLEndpointLimitation(datasetIRI, config.sparqlLimitations);
		}

		//In order to support the testing, the deletion of the configuration is temporarily skipped
		//exptManager.deleteConfiguraton(ContributionStore.class.getName(), reference);

	}


	private static final class PmkiRole {
		public static final Role PUBLIC = new Role("pmki_public", RoleLevel.system);
		public static final Role PRISTINE = new Role("pmki_pristine", RoleLevel.system);
		public static final Role STAGING = new Role("pmki_staging", RoleLevel.system);
	}

}
