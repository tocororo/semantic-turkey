package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;

/**
 * This class provides services for obtain information on the available services.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Services extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Services.class);

	@Autowired
	private STServiceTracker stServiceTracker;
	
	/**
	 * Returns the extension paths associated with active extension bundles. Usually, they are in the form
	 * <code>groupId/artifactId</code>.
	 * 
	 * @return
	 */
	@STServiceOperation
	public Collection<String> getExtensionPaths() {
		return stServiceTracker.getExtensionPaths();
	}

	/**
	 * Returns the services classes utilizing a given extension path.
	 * 
	 * @return
	 */
	@STServiceOperation
	public Collection<String> getServiceClasses(String extensionPath) {
		return stServiceTracker.getServiceClasses(extensionPath);
	}

	/**
	 * Returns the operations of a service class bound to an extension path.
	 * 
	 * @param extensionPath
	 * @param serviceClass
	 * @return
	 */
	@STServiceOperation
	public Collection<String> getServiceOperations(String extensionPath, String serviceClass) {
		return stServiceTracker.getServiceOperations(extensionPath, serviceClass);
	}

};