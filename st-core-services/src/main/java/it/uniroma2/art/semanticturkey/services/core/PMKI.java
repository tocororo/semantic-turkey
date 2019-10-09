package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.contribution.ContributionStore;
import it.uniroma2.art.semanticturkey.config.contribution.StoredContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredDevResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredStableResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedModelException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JNativeSailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.RepositoryAccess;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.ProjectBindingException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.apache.poi.util.SystemOutLogger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@STService
public class PMKI extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(PMKI.class);

	@Autowired
	private ExtensionPointManager exptManager;

	/**
	 * Returns the available configuration in the ContributionStore
	 *
	 * @return
	 */
	@STServiceOperation
	public Collection<Reference> getContributionReferences() throws NoSuchConfigurationManager {
		return exptManager.getConfigurationReferences(null, UsersManager.getLoggedUser(), ContributionStore.class.getName());
	}

	/**
	 * @param configuration
	 * @throws NoSuchConfigurationManager
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
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
			RepositoryAccess repositoryAccess, PluginSpecification coreRepoSailConfigurerSpecification, String configurationReference) //TODO also parameters about metadata
			throws IOException, RBACException, WrongPropertiesException, ProjectBindingException,
			ProjectInconsistentException, ClassNotFoundException, ForbiddenProjectAccessException,
			UnsupportedModelException, UnsupportedPluginConfigurationException, ProjectUpdateException,
			InvalidConfigurationException, InvalidProjectNameException, ProjectAccessException,
			UnloadablePluginConfigurationException, UnsupportedLexicalizationModelException, ProjectInexistentException,
			ProjectCreationException, ReservedPropertyUpdateException, DuplicatedResourceException, STPropertyAccessException, ConfigurationNotFoundException, NoSuchConfigurationManager {

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

		ProjectManager.createProject(consumer, projectName, model, lexicalizationModel, baseURI.trim(),
				historyEnabled, validationEnabled, blacklistingEnabled, repositoryAccess, coreRepoID,
				coreRepoSailConfigurerSpecification, coreBackendType, supportRepoID,
				supportRepoSailConfigurerSpecification, supportBackendType, uriGeneratorSpecification,
				renderingEngineSpecification, creationDateProperty, modificationDateProperty,
				updateForRoles, preloadedDataFile, preloadedDataFormat, transitiveImportAllowance,
				failedImports, leftDataset, rightDataset);

		StoredStableResourceContributionConfiguration config = (StoredStableResourceContributionConfiguration) exptManager.getConfiguration(
				ContributionStore.class.getName(), parseReference(configurationReference));
		String emailTo = config.contributorEmail;
		String emailContent = "Dear " + config.contributorName + " " + config.contributorLastName + ",\n" +
					"your request to contribute to PMKI with the resource '" + config.resourceName + "' has been approved"; //TODO continue
		System.out.println("EmailTo: " + emailTo);
		System.out.println("Email content: " + emailContent);

		//In order to support the testing, the deletion of the configuration is temporarily skipped
		//exptManager.deleteConfiguraton(ContributionStore.class.getName(), parseReference(configurationReference));

		/**
		 * TODO
		 * 	- bind the pmki visitor user to the project with the "pmki-pristine" role
		 * 	- send email to the user with the link for uploading the resource and the auth token
		 * 	- (IN A DEDICATED SERVICE) once the resource is uploaded, remove the "pmki-pristine" role to the visitor and give him the "pmki-staging"
		 */
	}

	/**
	 * Approves a development-resource contribution request
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveDevelopmentContribution(String projectName, IRI model, IRI lexicalizationModel, String baseURI,
		  	RepositoryAccess repositoryAccess, PluginSpecification coreRepoSailConfigurerSpecification, String configurationReference)
			throws IOException, RBACException, WrongPropertiesException, ProjectBindingException,
			ProjectInconsistentException, ClassNotFoundException, ForbiddenProjectAccessException,
			UnsupportedModelException, UnsupportedPluginConfigurationException, ProjectUpdateException,
			InvalidConfigurationException, InvalidProjectNameException, ProjectAccessException,
			UnloadablePluginConfigurationException, UnsupportedLexicalizationModelException, ProjectInexistentException,
			ProjectCreationException, ReservedPropertyUpdateException, DuplicatedResourceException, STPropertyAccessException, ConfigurationNotFoundException, NoSuchConfigurationManager {

		StoredDevResourceContributionConfiguration config = (StoredDevResourceContributionConfiguration) exptManager.getConfiguration(
				ContributionStore.class.getName(), parseReference(configurationReference));
		String emailTo = config.contributorEmail;
		String emailContent = "Dear " + config.contributorName + " " + config.contributorLastName + ",\n" +
				"your request to contribute to PMKI with the resource '" + config.resourceName + "' has been approved"; //TODO continue
		System.out.println("EmailTo: " + emailTo);
		System.out.println("Email content: " + emailContent);

		//In order to support the testing, the deletion of the configuration is temporarily skipped
		//exptManager.deleteConfiguraton(ContributionStore.class.getName(), parseReference(configurationReference));

		/**
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
	}

	/**
	 * Approves a metadata contribution request
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveMetadataContribution() {
		/**
		 * TODO
		 * See the services in MetadataRegistry, like .addDataset() and the setter (.setSPARQLEndpoint(), ...)
		 *
		 * - send mail to the user about the approval
		 */
	}

}
