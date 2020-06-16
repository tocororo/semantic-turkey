package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataAssociation;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataAssociationStore;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataPattern;
import it.uniroma2.art.semanticturkey.config.resourcemetadata.ResourceMetadataPatternStore;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	public JsonNode listAssociations() throws NoSuchConfigurationManager, STPropertyAccessException,
			ConfigurationNotFoundException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode associationJsonArray = jsonFactory.arrayNode();

		ResourceMetadataPatternStore patternStore = getResourceMetadataPatternStore();
		ResourceMetadataAssociationStore associationStore = getResourceMetadataAssociationStore();
		//this service could be invoked also when no project is accessed (e.g. when creating a project)
		Project proj = (stServiceContext.hasContextParameter("project")) ? getProject() : null;
		Collection<Reference> references = associationStore.getConfigurationReferences(proj, UsersManager.getLoggedUser());
		List<ResourceMetadataAssociation> associations = new ArrayList<>();
		for (Reference ref : references) {
			ResourceMetadataAssociation association = associationStore.getConfiguration(ref);
			RDFResourceRole role = association.role;
			String patternRef = association.patternReference;
			if (patternExists(patternStore, patternRef)) {
				ObjectNode associationNode = jsonFactory.objectNode();
				associationNode.put("ref", ref.getRelativeReference());
				associationNode.put("role", association.role.toString());
				associationNode.put("patternRef", association.patternReference);
				associationJsonArray.add(associationNode);
			} else { //pattern doesn't exist (in might have been deleted manually) => delete the association
				associationStore.deleteConfiguration(ref);
			}
		}
		return associationJsonArray;
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void addAssociation(RDFResourceRole role, String patternReference)
			throws IOException, NoSuchConfigurationManager, WrongPropertiesException, STPropertyUpdateException,
			STPropertyAccessException, ConfigurationNotFoundException {
		//make sure that the same association does not exists
		ResourceMetadataAssociationStore associationStore = getResourceMetadataAssociationStore();
		if (associationExists(associationStore, role, patternReference)) {
			throw new IllegalArgumentException("An association between the same role and pattern already exists");
		}

		//make sure that the pattern exists
		if (!patternExists(getResourceMetadataPatternStore(), patternReference)) {
			throw new ConfigurationNotFoundException("Pattern " + patternReference + " not found");
		}

		//create the association
		ResourceMetadataAssociation association = new ResourceMetadataAssociation();
		association.role = role;
		association.patternReference = patternReference;

		//generate a randomic ID for the configuration to be stored
		String id = UUID.randomUUID().toString();
		Reference ref = new Reference(getProject(), null, id);

		//store the association configuration
		associationStore.storeConfiguration(ref, association);
	}

	@STServiceOperation(method = RequestMethod.POST)
	public void deleteAssociation(String reference) throws NoSuchConfigurationManager, ConfigurationNotFoundException {
		Reference ref = parseReference(reference);
		getResourceMetadataAssociationStore().deleteConfiguration(ref);
	}

	/* ====== Patterns ====== */

	/**
	 * Returns the identifiers (configuration relative reference) of the pattern stored
	 * at project level and those factory provided
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	public Collection<String> getPatternIdentifiers() throws NoSuchConfigurationManager {
		List<String> patternIDs = new ArrayList<>();
		ResourceMetadataPatternStore cm = getResourceMetadataPatternStore();
		//collects project-level patterns
		for (Reference ref: cm.getConfigurationReferences(getProject(), UsersManager.getLoggedUser())) {
			if (ref.getProject().isPresent()) {
				patternIDs.add(ref.getRelativeReference());
			}
		}
		//collects factory provided patterns
		for (String factId: cm.getFactoryConfigurations()) {
			patternIDs.add("factory:" + factId);
		}
		return patternIDs;
	}

	/**
	 * Shared patterns are those stored as system level
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	public Collection<String> getLibraryPatternIdentifiers() throws NoSuchConfigurationManager {
		List<String> patternIDs = new ArrayList<>();
		ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
		for (Reference ref: store.getConfigurationReferences(null, UsersManager.getLoggedUser())) {
			patternIDs.add(ref.getRelativeReference());
		}
		return patternIDs;
	}

	@STServiceOperation
	public ResourceMetadataPattern getPattern(String reference) throws NoSuchConfigurationManager,
			STPropertyAccessException, IOException {
		ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
		return getPatternFactoryIncluded(store, reference);
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
			ConfigurationNotFoundException, STPropertyAccessException {
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

	/**
	 * Clone a pattern stored at system level (shared) to project level
	 * @param reference the reference of the system level pattern to import
	 * @param name the name to assign at the imported pattern (at project level)
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void importPatternFromLibrary(String reference, String name) throws NoSuchConfigurationManager, STPropertyAccessException,
			WrongPropertiesException, IOException, STPropertyUpdateException {
		Reference patternRef = parseReference(reference);
		if (patternRef.getProject().isPresent()) {
			throw new IllegalArgumentException("Cannot import a pattern already stored at project level");
		}
		ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
		//retrieve system level pattern
		ResourceMetadataPattern pattern = store.getConfiguration(patternRef);
		//store the same pattern at project level
		Reference newPatterRef = new Reference(getProject(), null, patternRef.getIdentifier());
		store.storeConfiguration(newPatterRef, pattern);
	}

	/**
	 * Clone (share) a pattern stored at project level to system level
	 * @param reference the reference of the project level pattern to share
	 * @param name the name to assign at the shared pattern (at system level)
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void storePatternInLibrary(String reference, String name) throws NoSuchConfigurationManager,
			STPropertyAccessException, WrongPropertiesException, IOException, STPropertyUpdateException {
		Reference patternRef = parseReference(reference);
		if (!patternRef.getProject().isPresent()) {
			throw new IllegalArgumentException("Cannot share a pattern not stored at project level");
		}
		ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
		//retrieve project level pattern
		ResourceMetadataPattern pattern = store.getConfiguration(patternRef);
		//store the same pattern at system level
		Reference newPatterRef = new Reference(null, null, patternRef.getIdentifier());
		store.storeConfiguration(newPatterRef, pattern);
	}

	/**
	 *
	 * @param reference relativeReference of the pattern to clone
	 * @param name name of the new pattern to create
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws IOException
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void clonePattern(String reference, String name) throws STPropertyAccessException,
			WrongPropertiesException, IOException, NoSuchConfigurationManager,
			STPropertyUpdateException {
		ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
		ResourceMetadataPattern pattern = getPatternFactoryIncluded(store, reference);
		store.storeConfiguration(new Reference(getProject(), null, name), pattern);
	}

	/**
	 * Import and add a new pattern at project level from a loaded file
	 * @param inputFile
	 * @param name
	 * @throws IOException
	 * @throws NoSuchConfigurationManager
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void importPattern(MultipartFile inputFile, String name) throws IOException,
			NoSuchConfigurationManager, STPropertyAccessException, WrongPropertiesException, STPropertyUpdateException {
		File tempServerFile = File.createTempFile("cfImport", inputFile.getOriginalFilename());
		try {
			//convert multipart in file
			inputFile.transferTo(tempServerFile);
			//load the configuration of the given file (it eventually throws an exception if the content is not valid)
			ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
			ResourceMetadataPattern pattern = STPropertiesManager.loadSTPropertiesFromYAMLFiles(
					ResourceMetadataPattern.class, true, tempServerFile);
			//store the configuration at project level with the provided name
			store.storeConfiguration(new Reference(getProject(), null, name), pattern);
		} finally {
			tempServerFile.delete();
		}
	}

	/**
	 * Export/Download the pattern file
	 * @param oRes
	 * @param reference
	 * @throws CustomFormException
	 * @throws IOException
	 */
	@STServiceOperation
	public void exportPattern(HttpServletResponse oRes, String reference) throws IOException, NoSuchConfigurationManager,
			STPropertyAccessException {
		ResourceMetadataPatternStore store = getResourceMetadataPatternStore();
		ResourceMetadataPattern pattern = getPatternFactoryIncluded(store, reference);
		File tempServerFile = File.createTempFile("patternExport", ".cfg");
		try {
			STPropertiesManager.storeSTPropertiesInYAML(pattern, tempServerFile, true);
			oRes.setHeader("Content-Disposition", "attachment; filename=" + reference.substring(reference.indexOf(":")+1) + ".cfg");
			oRes.setContentType("text/plain");
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}

	/**
	 * Returns a pattern looking in pattern store and among those factory-provided
	 * @param store
	 * @param reference
	 * @return
	 * @throws IOException
	 * @throws STPropertyAccessException
	 */
	private ResourceMetadataPattern getPatternFactoryIncluded(ResourceMetadataPatternStore store, String reference)
			throws IOException, STPropertyAccessException {
		if (reference.startsWith("factory")) {
			String fileName = reference.substring(reference.indexOf(":")+1) + ".cfg";
			File factConfFile = store.getFactoryConfigurationFile(fileName);
			return STPropertiesManager.loadSTPropertiesFromYAMLFiles(ResourceMetadataPattern.class, true, factConfFile);
		} else {
			Reference ref = parseReference(reference);
			return store.getConfiguration(ref);
		}
	}

	private boolean associationExists(ResourceMetadataAssociationStore store, RDFResourceRole role, String patternRef)
			throws STPropertyAccessException {
		for (Reference associationRef: store.getConfigurationReferences(getProject(), null)) {
			ResourceMetadataAssociation association = store.getConfiguration(associationRef);
			if (association.role.equals(role) && association.patternReference.equals(patternRef)) {
				return true;
			}
		}
		return false;
	}

	private boolean patternExists(ResourceMetadataPatternStore store, String patternRef) {
		if (patternRef.startsWith("factory")) { //factory provided => check if it is among those stored
			return store.getFactoryConfigurations().contains(patternRef.substring(patternRef.indexOf(":")+1));
		} else { //project level => check if exists the reference
			for (Reference ref: store.getConfigurationReferences(getProject(), null)) {
				if (ref.getRelativeReference().equals(patternRef)) {
					return true;
				}
			}
			return false;
		}
	}

}
