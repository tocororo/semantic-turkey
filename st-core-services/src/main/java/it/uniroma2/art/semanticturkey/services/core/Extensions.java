package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services for handling extensions.
 */
@STService
public class Extensions extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Extensions.class);

	@Autowired
	private ExtensionPointManager exptManager;

	/**
	 * Returns known extension points.
	 * 
	 * @param scopes
	 *            if not empty, indicates the scopes we are interested in. Otherwise, every scope is
	 *            considered.
	 * @return
	 */
	@STServiceOperation
	public Collection<ExtensionPoint> getExtensionPoints(@Optional(defaultValue = "") Scope[] scopes) {
		return exptManager.getExtensionPoints(scopes);
	}

	/**
	 * Returns an extension point given its identifier
	 * 
	 * @param identifier
	 * @return
	 */
	@STServiceOperation
	public ExtensionPoint getExtensionPoint(String identifier) {
		return exptManager.getExtensionPoint(identifier);
	}

	/**
	 * Returns the stored configurations associated with the given component
	 * 
	 * @param componentIdentifier
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	public Collection<Reference> getConfigurationReferences(String componentIdentifier)
			throws NoSuchConfigurationManager {
		return exptManager.getConfigurationReferences(getProject(), UsersManager.getLoggedUser(),
				componentIdentifier);
	}

	/**
	 * Returns a stored configurations given its reference
	 * 
	 * @param componentIdentifier
	 * @param reference
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 */
	@STServiceOperation
	public Configuration getConfiguration(String componentIdentifier, String reference)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException {
		return exptManager.getConfiguration(componentIdentifier, parseReference(reference));
	}

	/**
	 * Stores a configurations
	 * 
	 * @param componentIdentifier
	 * @param reference
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void storeConfiguration(String componentIdentifier, String reference,
			Map<String, Object> configuration) throws NoSuchConfigurationManager, IOException,
			ConfigurationNotFoundException, WrongPropertiesException {
		exptManager.storeConfiguration(componentIdentifier, parseReference(reference), configuration);
	}

	private Reference parseReference(String reference) {
		int colonPos = reference.indexOf(":");

		if (colonPos == -1)
			throw new IllegalArgumentException("Invalid reference: " + reference);

		String refType = reference.substring(0, colonPos);
		String identifier = reference.substring(colonPos + 1);

		switch (refType) {
		case "system":
			return new Reference(null, null, identifier);
		case "project":
			return new Reference(getProject(), null, identifier);
		case "user":
			return new Reference(null, UsersManager.getLoggedUser(), identifier);
		case "pu":
			return new Reference(getProject(), UsersManager.getLoggedUser(), identifier);
		default:
			throw new IllegalArgumentException("Invalid reference: " + reference);
		}
	}
}