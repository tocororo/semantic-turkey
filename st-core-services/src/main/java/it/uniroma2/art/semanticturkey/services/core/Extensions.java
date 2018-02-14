package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.extension.settings.Settings;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
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
	 * Returns known extensions for a given extension point
	 * 
	 * @param extensionPointID
	 * @return
	 */
	@STServiceOperation
	public Collection<ExtensionFactory<?>> getExtensions(String extensionPointID) {
		return exptManager.getExtensions(extensionPointID);
	}

}