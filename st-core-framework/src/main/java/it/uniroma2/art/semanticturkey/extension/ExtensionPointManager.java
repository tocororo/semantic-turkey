package it.uniroma2.art.semanticturkey.extension;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import it.uniroma2.art.semanticturkey.extension.config.Configuration;
import it.uniroma2.art.semanticturkey.extension.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.user.STUser;

public interface ExtensionPointManager {

	/**
	 * Returns known extension points.
	 * 
	 * @param scopes
	 *            if not empty, indicates the scopes we are interested in. Otherwise, every scope is
	 *            considered.
	 * @return
	 */
	Collection<ExtensionPoint> getExtensionPoints(Scope... scopes);

	/**
	 * Returns an extension point given its identifier
	 * 
	 * @param identifier
	 * @return
	 * @throws NoSuchExtensionPointException
	 */
	ExtensionPoint getExtensionPoint(String identifier) throws NoSuchExtensionPointException;

	/**
	 * Returns the stored configurations associated with a given component
	 * 
	 * @param project
	 * @param user
	 * @param componentIdentifier
	 * 
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	Collection<Reference> getConfigurationReferences(Project project, STUser user, String componentIdentifier)
			throws NoSuchConfigurationManager;

	/**
	 * Returns a stored configuration located with the supplied identifier
	 * 
	 * @param componentIdentifier
	 * @param reference
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 */
	Configuration getConfiguration(String componentIdentifier, Reference reference) throws IOException,
			ConfigurationNotFoundException, WrongPropertiesException, NoSuchConfigurationManager;

	void storeConfiguration(String componentIdentifier, Reference reference,
			Map<String, Object> configuration) throws IOException, WrongPropertiesException, NoSuchConfigurationManager;
}
