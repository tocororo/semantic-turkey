package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.customservice.CustomService;
import it.uniroma2.art.semanticturkey.config.customservice.CustomServiceDefinitionStore;
import it.uniroma2.art.semanticturkey.config.customservice.Operation;
import it.uniroma2.art.semanticturkey.customservice.CustomServiceHandlerMapping;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.extpts.customservice.CustomServiceBackend;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
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
	@STServiceOperation
	public Collection<String> getCustomServiceIdentifiers() throws NoSuchConfigurationManager {
		CustomServiceDefinitionStore cm = (CustomServiceDefinitionStore) exptManager
				.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
		return cm.getSystemConfigurationIdentifiers();
	}

	/**
	 * Deletes a custom service given the <em>id</em> of its associated configuration file
	 * 
	 * @param id
	 * @throws NoSuchConfigurationManager
	 * @throws ConfigurationNotFoundException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void deleteCustomService(String id)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException {
		CustomServiceDefinitionStore cm = (CustomServiceDefinitionStore) exptManager
				.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
		cm.deleteSystemConfiguration(id);
		customServiceMapping.unregisterCustomService(id);
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
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void createCustomService(String id, ObjectNode definition)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException {
		CustomServiceDefinitionStore cm = (CustomServiceDefinitionStore) exptManager
				.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
		exptManager.storeConfiguration(CustomServiceDefinitionStore.class.getName(),
				new Reference(null, null, id), definition);
		customServiceMapping.registerCustomService(id);
	}

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
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void addOperationToCustomService(String id, ObjectNode operationDefinition)
			throws NoSuchConfigurationManager, ConfigurationNotFoundException, IOException,
			WrongPropertiesException, STPropertyUpdateException, STPropertyAccessException {
		Operation op = STPropertiesManager.loadSTPropertiesFromObjectNode(Operation.class, true,
				operationDefinition);

		CustomServiceDefinitionStore cm = (CustomServiceDefinitionStore) exptManager
				.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
		CustomService customService = cm.getSystemConfiguration(id);

		List<Operation> oldOps = customService.operations;
		if (oldOps != null) {
			customService.operations = new ArrayList<>(oldOps.size() + 1);
			customService.operations.addAll(oldOps);
			customService.operations.add(op);
		} else {
			customService.operations = Lists.newArrayList(op);
		}

		cm.storeSystemConfiguration(id, customService);
		customServiceMapping.registerCustomService(id);
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
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void removeOperationFromCustomService(String id, String operationName)
			throws NoSuchConfigurationManager, IOException, ConfigurationNotFoundException,
			WrongPropertiesException, STPropertyAccessException, STPropertyUpdateException {
		CustomServiceDefinitionStore cm = (CustomServiceDefinitionStore) exptManager
				.getConfigurationManager(CustomServiceDefinitionStore.class.getName());
		CustomService customService = cm.getSystemConfiguration(id);

		List<Operation> oldOps = customService.operations;
		if (oldOps != null) {
			customService.operations = oldOps.stream().filter(op -> !Objects.equals(operationName, op.name))
					.collect(Collectors.toList());

			cm.storeSystemConfiguration(id, customService);
			customServiceMapping.registerCustomService(id);
		}

	}

	/**
	 * Reloads the custom service defined by the configuration identified by the given <em>id</em>.
	 * 
	 * @param id
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void reloadCustomService(String id) {
		customServiceMapping.registerCustomService(id);
	}
}
