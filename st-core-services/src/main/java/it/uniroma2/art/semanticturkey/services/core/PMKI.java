package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.contribution.ContributionStore;
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
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
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
	 * @return
	 */
	@STServiceOperation
	public Collection<Reference> getContributionReferences() throws NoSuchConfigurationManager {
		return exptManager.getConfigurationReferences(null, UsersManager.getLoggedUser(), ContributionStore.class.getName());
	}

	/**
	 *
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
	 *
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
	 * Approves a stable-resource or a development-resource contribution request
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void approveResourceContribution() {
		/**
		 * TODO
		 * params:
		 * - all the params in Projects.createProject()?
		 */
		/**
		 * TODO
		 * wrap the Projects.createProject() service
		 * - in case of stable resource:
		 * 		- invoke the service, assign the pmki-pristine to the project (give to the pre-defined user the pmki-pristine role)
		 * - in case of development resource with http requests to the SemanticTurkey for staging project:
		 * 		- do the login as admin
		 * 		- create project
		 * 		- create a user, activate it and assign a role (which one???) to the project
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
		 */
	}

}
