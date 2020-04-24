package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.config.customservice.CustomService;
import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.customservice.CustomServiceException;
import it.uniroma2.art.semanticturkey.customservice.CustomServiceHandlerMapping;
import it.uniroma2.art.semanticturkey.customservice.DuplicateIdException;
import it.uniroma2.art.semanticturkey.customservice.DuplicateName;
import it.uniroma2.art.semanticturkey.customservice.SchemaException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

@STService
public class CustomServices extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(CustomServices.class);

	@Autowired
	private CustomServiceHandlerMapping customServiceMapping;

	/**
	 * Returns the <em>identifiers</em> of the configurations stored at system-level that define custom
	 * services.
	 * 
	 * @see CustomServiceDefinitionStore
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service)', 'R')")
	@STServiceOperation
	public Collection<String> getCustomServiceIdentifiers() throws NoSuchConfigurationManager {
		return customServiceMapping.getCustomServiceIdentifiers();
	}

	/**
	 * Creates a custom service given its <em>definition</em> and the <em>id</em> of the configuration file
	 * used for persisting the definition.
	 * <p>
	 * The definition shall be the JSON serialization of a {@link CustomService} configuration object.
	 * Currently, the sole mandatory part of the definition is the service name.
	 * </p>
	 * 
	 * @param id
	 * @param definition
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 * @throws STPropertyAccessException
	 * @throws STPropertyUpdateException
	 * @throws WrongPropertiesException
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws SchemaException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws CustomServiceException
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service)', 'C')")
	@STServiceOperation(method = RequestMethod.POST)
	public void createCustomService(String id, ObjectNode definition)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException,
			InstantiationException, IllegalAccessException, SchemaException, IllegalArgumentException,
			NoSuchExtensionException, InvalidConfigurationException, CustomServiceException {
		customServiceMapping.registerCustomService(id, definition, false);
	}

	/**
	 * Returns the definition of a custom service given the <em>id</em> of the associated configuration file
	 * 
	 * @param id
	 * @return
	 * @throws NoSuchConfigurationManager
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service)', 'R')")
	@STServiceOperation
	public CustomService getCustomService(String id) throws NoSuchConfigurationManager, IOException,
			ConfigurationNotFoundException, WrongPropertiesException, STPropertyAccessException {
		return customServiceMapping.getCustomService(id);
	}

	/**
	 * Updates a custom service given its <em>definition</em> and the <em>id</em> of the configuration file
	 * used for persisting the definition.
	 * <p>
	 * The definition shall be the JSON serialization of a {@link CustomService} configuration object.
	 * </p>
	 * 
	 * @param id
	 * @param definition
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 * @throws STPropertyAccessException
	 * @throws STPropertyUpdateException
	 * @throws WrongPropertiesException
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws SchemaException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws CustomServiceException
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service)', 'U')")
	@STServiceOperation(method = RequestMethod.POST)
	public void updateCustomService(String id, ObjectNode definition)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException,
			InstantiationException, IllegalAccessException, SchemaException, IllegalArgumentException,
			NoSuchExtensionException, InvalidConfigurationException, CustomServiceException {
		customServiceMapping.registerCustomService(id, definition, true);
	}

	/**
	 * Deletes a custom service given the <em>id</em> of its associated configuration file
	 * 
	 * @param id
	 * @throws ConfigurationNotFoundException
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service)', 'D')")
	@STServiceOperation(method = RequestMethod.POST)
	public void deleteCustomService(String id) throws ConfigurationNotFoundException {
		customServiceMapping.unregisterCustomService(id);
	}

	@PreAuthorize("@auth.isAuthorized('customService(service)', 'R')")
	@SuppressWarnings("unchecked")
	@STServiceOperation
	public List<Configuration> getOperationForms()
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException {
		return (List<Configuration>) exptManager.getExtensions(CustomServiceBackend.class.getName()).stream()
				.filter(ConfigurableExtensionFactory.class::isInstance)
				.map(ConfigurableExtensionFactory.class::cast).flatMap(f -> f.getConfigurations().stream())
				.collect(Collectors.toList());
	}

	/**
	 * Adds an operation to a given custom service (identified by the <em>id</em> of its associated
	 * configuration). The operation definition is a JSON object containing the data obtained by filling an
	 * <em>operation form</em>. Note that the object must include an @type property holding the conifiguration
	 * type.
	 * 
	 * @param id
	 * @param operationDefinition
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 * @throws InvalidConfigurationException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws SchemaException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws CustomServiceException
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service, operation)', 'C')")
	@STServiceOperation(method = RequestMethod.POST)
	public void addOperationToCustomService(String id, ObjectNode operationDefinition)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException,
			InstantiationException, IllegalAccessException, SchemaException, IllegalArgumentException,
			NoSuchExtensionException, InvalidConfigurationException, CustomServiceException {

		customServiceMapping.addOperationToCustomeService(id, operationDefinition);
	}

	/**
	 * Update an operation associated with a given custom service (identified by the <em>id</em> of its
	 * associated configuration). The operation definition is a JSON object containing the data obtained by
	 * filling an <em>operation form</em>. Note that the object must include an @type property holding the
	 * configuration type. The optional <em>oldOperationName</em> shouldbe passed when the new operation uses
	 * a different name.
	 * 
	 * @param id
	 * @param operationDefinition
	 * @param oldOperationName
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 * @throws STPropertyAccessException
	 * @throws InvalidConfigurationException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws SchemaException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws CustomServiceException
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service, operation)', 'U')")
	@STServiceOperation(method = RequestMethod.POST)
	public void updateOperationInCustomService(String id, ObjectNode operationDefinition,
			@Optional String oldOperationName)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException,
			InstantiationException, IllegalAccessException, SchemaException, IllegalArgumentException,
			NoSuchExtensionException, InvalidConfigurationException, CustomServiceException {
		customServiceMapping.udpateOperationInCustomeService(id, operationDefinition, oldOperationName);
	}

	/**
	 * Removes an operation from a custom service
	 * 
	 * @param id
	 * @param operationName
	 * @throws NoSuchConfigurationManager
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws ConfigurationNotFoundException
	 * @throws IOException
	 * @throws STPropertyUpdateException
	 * @throws InvalidConfigurationException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws SchemaException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws CustomServiceException
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service, operation)', 'D')")
	@STServiceOperation(method = RequestMethod.POST)
	public void removeOperationFromCustomService(String id, String operationName)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, STPropertyAccessException, STPropertyUpdateException,
			InstantiationException, IllegalAccessException, SchemaException, IllegalArgumentException,
			NoSuchExtensionException, InvalidConfigurationException, CustomServiceException {
		customServiceMapping.removeOperationFromCustomeService(id, operationName);
	}

	/**
	 * Reloads the custom service defined by the configuration identified by the given <em>id</em>.
	 * 
	 * @param id
	 * @throws ConfigurationNotFoundException
	 * @throws InvalidConfigurationException
	 * @throws STPropertyUpdateException
	 * @throws WrongPropertiesException
	 * @throws IOException
	 * @throws STPropertyAccessException
	 * @throws DuplicateIdException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 * @throws SchemaException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws DuplicateName 
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service)', 'R')")
	@STServiceOperation(method = RequestMethod.POST)
	public void reloadCustomService(String id) throws InstantiationException, IllegalAccessException,
			SchemaException, IllegalArgumentException, NoSuchExtensionException, DuplicateIdException,
			STPropertyAccessException, IOException, WrongPropertiesException, STPropertyUpdateException,
			InvalidConfigurationException, ConfigurationNotFoundException, DuplicateName {
		customServiceMapping.registerCustomService(id);
	}

	/**
	 * Reloads all custom services.
	 * 
	 * @throws NoSuchConfigurationManager
	 * 
	 */
	@PreAuthorize("@auth.isAuthorized('customService(service)', 'R')")
	@STServiceOperation(method = RequestMethod.POST)
	public void reloadCustomServices() throws NoSuchConfigurationManager {
		customServiceMapping.initializeFromStoredCustomServices();
	}
}
