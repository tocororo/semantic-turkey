package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services for handling configurations.
 */
@STService
public class Configurations extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Configurations.class);

	@Autowired
	private ExtensionPointManager exptManager;

	/**
	 * Returns the available configuration managers
	 * 
	 * @return
	 */
	@STServiceOperation
	public Collection<ConfigurationManager<?>> getConfigurationManagers() {
		return exptManager.getConfigurationManagers();
	}

	/**
	 * Returns a specific configuration manager
	 * 
	 * @param componentID
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	public ConfigurationManager<?> getConfigurationManager(String componentID)
			throws NoSuchConfigurationManager {
		return exptManager.getConfigurationManager(componentID);
	}

	/**
	 * Returns the stored configurations associated with the given component
	 * 
	 * @param componentID
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	public Collection<Reference> getConfigurationReferences(String componentID)
			throws NoSuchConfigurationManager {
		return exptManager.getConfigurationReferences(getProject(), UsersManager.getLoggedUser(),
				componentID);
	}

	/**
	 * Returns a stored configuration given its relative reference
	 * 
	 * @param componentID
	 * @param relativeReference
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 */
	@STServiceOperation
	public Configuration getConfiguration(String componentID, String relativeReference)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException {
		return exptManager.getConfiguration(componentID, parseReference(relativeReference));
	}

	/**
	 * Stores a configurations
	 * 
	 * @param componentID
	 * @param relativeReference
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void storeConfiguration(String componentID, String relativeReference,
			Map<String, Object> configuration) throws NoSuchConfigurationManager, IOException,
			WrongPropertiesException, STPropertyUpdateException {
		exptManager.storeConfiguration(componentID, parseReference(relativeReference), configuration);
	}

	/**
	 * Deletes a previously stored configuration
	 * 
	 * @param componentID
	 * @param relativeReference
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void deleteConfiguration(String componentID, String relativeReference)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException {
		exptManager.deleteConfiguraton(componentID, parseReference(relativeReference));
	}

	private Reference parseReference(String relativeReference) {
		int colonPos = relativeReference.indexOf(":");

		if (colonPos == -1)
			throw new IllegalArgumentException("Invalid reference: " + relativeReference);

		Scope scope = Scope.deserializeScope(relativeReference.substring(0, colonPos));
		String identifier = relativeReference.substring(colonPos + 1);

		switch (scope) {
		case SYSTEM:
			return new Reference(null, null, identifier);
		case PROJECT:
			return new Reference(getProject(), null, identifier);
		case USER:
			return new Reference(null, UsersManager.getLoggedUser(), identifier);
		case PROJECT_USER:
			return new Reference(getProject(), UsersManager.getLoggedUser(), identifier);
		default:
			throw new IllegalArgumentException("Unsupported scope: " + scope);
		}
	}

}