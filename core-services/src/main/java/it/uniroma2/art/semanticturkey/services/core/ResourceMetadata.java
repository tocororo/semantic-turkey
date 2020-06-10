package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.resourceMetadata.ResourceMetadataAssociation;
import it.uniroma2.art.semanticturkey.config.resourceMetadata.ResourceMetadataAssociationStore;
import it.uniroma2.art.semanticturkey.config.resourceMetadata.ResourceMetadataPattern;
import it.uniroma2.art.semanticturkey.config.resourceMetadata.ResourceMetadataPatternStore;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@STService
public class ResourceMetadata extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(ResourceMetadata.class);

	@Autowired
	protected ExtensionPointManager exptManager;

	protected ResourceMetadataPatternStore getResourceMetadataPatternStore() throws NoSuchConfigurationManager {
		return (ResourceMetadataPatternStore) exptManager
				.getConfigurationManager(ResourceMetadataPatternStore.class.getName());
	}

	protected ResourceMetadataAssociationStore getResourceMetadataAssociationStore() throws NoSuchConfigurationManager {
		return (ResourceMetadataAssociationStore) exptManager
				.getConfigurationManager(ResourceMetadataAssociationStore.class.getName());
	}

	/* ====== Mappings ====== */

	@STServiceOperation()
	public List<ResourceMetadataAssociation> listAssociations() throws NoSuchConfigurationManager, STPropertyAccessException,
			WrongPropertiesException, ConfigurationNotFoundException, IOException {
		ResourceMetadataAssociationStore cm = getResourceMetadataAssociationStore();
		//this service could be invoked also when no project is accessed (e.g. when creating a project)
		Project proj = (stServiceContext.hasContextParameter("project")) ? getProject() : null;
		Collection<Reference> references = cm.getConfigurationReferences(proj, UsersManager.getLoggedUser());
		List<ResourceMetadataAssociation> associations = new ArrayList<>();
		for (Reference ref : references) {
			associations.add(cm.getConfiguration(ref));
		}
		return associations;
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void addAssociation(RDFResourceRole role, String patternReference)
			throws IOException, NoSuchConfigurationManager, WrongPropertiesException, STPropertyUpdateException,
			STPropertyAccessException, ConfigurationNotFoundException {
		//retrieve the referenced pattern just to make sure that it exists
		getResourceMetadataPatternStore().getConfiguration(parseReference(patternReference));

		//create the association
		ResourceMetadataAssociation association = new ResourceMetadataAssociation();
		association.role = role;
		association.patternReference = patternReference;

		//generate a randomic ID for the configuration to be stored
		String id = UUID.randomUUID().toString();
		Reference ref = new Reference(getProject(), null, id);

		//store the association configuration
		getResourceMetadataAssociationStore().storeConfiguration(ref, association);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deleteAssociation(String reference) throws NoSuchConfigurationManager, ConfigurationNotFoundException {
		Reference ref = parseReference(reference);
		getResourceMetadataAssociationStore().deleteConfiguration(ref);
	}

	/* ====== Updaters ====== */

	@STServiceOperation
	public Collection<Reference> getPatternIdentifiers() throws NoSuchConfigurationManager {
		ResourceMetadataPatternStore cm = getResourceMetadataPatternStore();
		//this service could be invoked also when no project is accessed (e.g. when creating a project)
		Project proj = (stServiceContext.hasContextParameter("project")) ? getProject() : null;
		return cm.getConfigurationReferences(proj, UsersManager.getLoggedUser());
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void createPattern(String reference, @JsonSerialized ObjectNode definition)
			throws IOException, NoSuchConfigurationManager, WrongPropertiesException, STPropertyUpdateException {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		ResourceMetadataPattern defObj = mapper.treeToValue(definition, ResourceMetadataPattern.class);
		Reference ref = parseReference(reference);
		getResourceMetadataPatternStore().storeConfiguration(ref, defObj);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void updatePattern(String reference, @JsonSerialized ObjectNode definition)
			throws IOException, NoSuchConfigurationManager, WrongPropertiesException, STPropertyUpdateException {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		ResourceMetadataPattern defObj = mapper.treeToValue(definition, ResourceMetadataPattern.class);
		Reference ref = parseReference(reference);
		getResourceMetadataPatternStore().storeConfiguration(ref, defObj);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deletePattern(String reference) throws NoSuchConfigurationManager,
			ConfigurationNotFoundException, WrongPropertiesException, IOException, STPropertyAccessException {
		Reference patternRef = parseReference(reference);
		//first delete the associations referencing this pattern
		ResourceMetadataAssociationStore associationStore = getResourceMetadataAssociationStore();
		Collection<Reference> associationReferences = associationStore.getConfigurationReferences(getProject(), UsersManager.getLoggedUser());
		for (Reference ref : associationReferences) {
			ResourceMetadataAssociation association = associationStore.getConfiguration(ref);
			if (association.patternReference.equals(reference)) {
				associationStore.deleteConfiguration(ref);
			}
		}
		//delete the pattern
		getResourceMetadataPatternStore().deleteConfiguration(patternRef);
	}

}
