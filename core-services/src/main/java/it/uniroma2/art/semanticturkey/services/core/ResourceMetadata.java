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
import it.uniroma2.art.semanticturkey.versioning.ResourceMetadataManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
	@Autowired
	private ResourceMetadataManager resourceMetadataManager;

	/* ====== Associations ====== */

	@STServiceOperation()
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, association)', 'R')")
	public JsonNode listAssociations() throws STPropertyAccessException,
			ConfigurationNotFoundException, NoSuchConfigurationManager {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode associationJsonArray = jsonFactory.arrayNode();

		ResourceMetadataPatternStore patternStore = resourceMetadataManager.getResourceMetadataPatternStore();
		ResourceMetadataAssociationStore associationStore = resourceMetadataManager.getResourceMetadataAssociationStore();
		//this service could be invoked also when no project is accessed (e.g. when creating a project)
		Project proj = (stServiceContext.hasContextParameter("project")) ? getProject() : null;
		Collection<Reference> references = associationStore.getConfigurationReferences(proj, UsersManager.getLoggedUser());
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
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, association)', 'C')")
	public void addAssociation(RDFResourceRole role, String patternReference)
			throws IOException, WrongPropertiesException, STPropertyUpdateException,
			STPropertyAccessException, ConfigurationNotFoundException, NoSuchConfigurationManager {
		//make sure that the same association does not exists
		ResourceMetadataAssociationStore associationStore = resourceMetadataManager.getResourceMetadataAssociationStore();
		if (associationExists(associationStore, role, patternReference)) {
			throw new IllegalArgumentException("An association between the same role and pattern already exists");
		}

		//make sure that the pattern exists
		if (!patternExists(resourceMetadataManager.getResourceMetadataPatternStore(), patternReference)) {
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
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, association)', 'D')")
	public void deleteAssociation(String reference) throws ConfigurationNotFoundException, NoSuchConfigurationManager {
		Reference ref = parseReference(reference);
		resourceMetadataManager.getResourceMetadataAssociationStore().deleteConfiguration(ref);
	}

	/* ====== Patterns ====== */

	/**
	 * Returns the identifiers (configuration relative reference) of the pattern stored
	 * at project level and those factory provided
	 *
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'R')")
	public Collection<String> getPatternIdentifiers() throws NoSuchConfigurationManager {
		List<String> patternIDs = new ArrayList<>();
		ResourceMetadataPatternStore cm = resourceMetadataManager.getResourceMetadataPatternStore();
		//collects project-level patterns
		for (Reference ref : cm.getConfigurationReferences(getProject(), UsersManager.getLoggedUser())) {
			if (ref.getProject().isPresent()) {
				patternIDs.add(ref.getRelativeReference());
			}
		}
		//collects factory provided patterns
		patternIDs.addAll(cm.getFactoryConfigurationReferences());
		return patternIDs;
	}

	/**
	 * Returns the reference of the only factory-provided patterns.
	 * This service can be used to have the list of only the factory-provided patterns from outside a project.
	 * In other cases (in the context of a project) it can be used getPatternIdentifiers which returns all of them
	 * (factory-provided and non)
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isSuperUser(false)")
	public Collection<String> getFactoryPatternIdentifiers() throws NoSuchConfigurationManager {
		return resourceMetadataManager.getResourceMetadataPatternStore().getFactoryConfigurationReferences();
	}

	/**
	 * Shared patterns are those stored as system level
	 *
	 * @return
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'R')")
	public Collection<String> getLibraryPatternIdentifiers() throws NoSuchConfigurationManager {
		List<String> patternIDs = new ArrayList<>();
		ResourceMetadataPatternStore store = resourceMetadataManager.getResourceMetadataPatternStore();
		for (Reference ref : store.getConfigurationReferences(null, UsersManager.getLoggedUser())) {
			patternIDs.add(ref.getRelativeReference());
		}
		return patternIDs;
	}

	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'R')")
	public ResourceMetadataPattern getPattern(String reference) throws STPropertyAccessException, IOException, NoSuchConfigurationManager {
		//this service could be invoked also when no project is accessed (e.g. during project creation)
		Project project = null;
		if (stServiceContext.hasContextParameter("project")) {
			project = getProject();
		}
		return resourceMetadataManager.getResourceMetadataPattern(project, reference);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'C')")
	public void createPattern(String reference, @JsonSerialized ObjectNode definition)
			throws IOException, WrongPropertiesException, STPropertyUpdateException, NoSuchConfigurationManager {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		ResourceMetadataPattern defObj = mapper.treeToValue(definition, ResourceMetadataPattern.class);
		Reference ref = parseReference(reference);
		resourceMetadataManager.getResourceMetadataPatternStore().storeConfiguration(ref, defObj);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'U')")
	public void updatePattern(String reference, @JsonSerialized ObjectNode definition)
			throws IOException, WrongPropertiesException, STPropertyUpdateException, NoSuchConfigurationManager {
		ObjectMapper mapper = STPropertiesManager.createObjectMapper(exptManager);
		ResourceMetadataPattern defObj = mapper.treeToValue(definition, ResourceMetadataPattern.class);
		Reference ref = parseReference(reference);
		resourceMetadataManager.getResourceMetadataPatternStore().storeConfiguration(ref, defObj);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'D')")
	public void deletePattern(String reference) throws ConfigurationNotFoundException, STPropertyAccessException, NoSuchConfigurationManager {
		Reference patternRef = parseReference(reference);
		//first delete the associations referencing this pattern
		ResourceMetadataAssociationStore associationStore = resourceMetadataManager.getResourceMetadataAssociationStore();
		Collection<Reference> associationReferences = associationStore.getConfigurationReferences(getProject(), UsersManager.getLoggedUser());
		for (Reference ref : associationReferences) {
			ResourceMetadataAssociation association = associationStore.getConfiguration(ref);
			if (association.patternReference.equals(reference)) {
				associationStore.deleteConfiguration(ref);
			}
		}
		//delete the pattern
		resourceMetadataManager.getResourceMetadataPatternStore().deleteConfiguration(patternRef);
	}

	/**
	 * Clone a pattern stored at system level (shared) to project level
	 *
	 * @param reference the reference of the system level pattern to import
	 * @param name      the name to assign at the imported pattern (at project level)
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'C')")
	public void importPatternFromLibrary(String reference, String name) throws STPropertyAccessException,
			WrongPropertiesException, IOException, STPropertyUpdateException, NoSuchConfigurationManager {
		Reference patternRef = parseReference(reference);
		if (patternRef.getProject().isPresent()) {
			throw new IllegalArgumentException("Cannot import a pattern already stored at project level");
		}
		ResourceMetadataPattern pattern = resourceMetadataManager.getResourceMetadataPattern(getProject(), reference);
		//store the same pattern at project level
		Reference newPatterRef = new Reference(getProject(), null, patternRef.getIdentifier());
		resourceMetadataManager.getResourceMetadataPatternStore().storeConfiguration(newPatterRef, pattern);
	}

	/**
	 * Clone (share) a pattern stored at project level to system level
	 *
	 * @param reference the reference of the project level pattern to share
	 * @param name      the name to assign at the shared pattern (at system level)
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'C')")
	public void storePatternInLibrary(String reference, String name) throws STPropertyAccessException,
			WrongPropertiesException, IOException, STPropertyUpdateException, NoSuchConfigurationManager {
		Reference patternRef = parseReference(reference);
		if (!patternRef.getProject().isPresent()) {
			throw new IllegalArgumentException("Cannot share a pattern not stored at project level");
		}
		//retrieve project level pattern
		ResourceMetadataPattern pattern = resourceMetadataManager.getResourceMetadataPattern(getProject(), reference);
		//store the same pattern at system level
		Reference newPatterRef = new Reference(null, null, patternRef.getIdentifier());
		resourceMetadataManager.getResourceMetadataPatternStore().storeConfiguration(newPatterRef, pattern);
	}

	/**
	 * @param reference relativeReference of the pattern to clone
	 * @param name      name of the new pattern to create
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws IOException
	 * @throws NoSuchConfigurationManager
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'C')")
	public void clonePattern(String reference, String name) throws STPropertyAccessException,
			WrongPropertiesException, IOException, STPropertyUpdateException, NoSuchConfigurationManager {
		ResourceMetadataPattern pattern = resourceMetadataManager.getResourceMetadataPattern(getProject(), reference);
		resourceMetadataManager.getResourceMetadataPatternStore().storeConfiguration(new Reference(getProject(), null, name), pattern);
	}

	/**
	 * Import and add a new pattern at project level from a loaded file
	 *
	 * @param inputFile
	 * @param name
	 * @throws IOException
	 * @throws NoSuchConfigurationManager
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws STPropertyUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'C')")
	public void importPattern(MultipartFile inputFile, String name) throws IOException, STPropertyAccessException,
			WrongPropertiesException, STPropertyUpdateException, NoSuchConfigurationManager {
		File tempServerFile = File.createTempFile("cfImport", inputFile.getOriginalFilename());
		try {
			//convert multipart in file
			inputFile.transferTo(tempServerFile);
			//load the configuration of the given file (it eventually throws an exception if the content is not valid)
			ResourceMetadataPatternStore store = resourceMetadataManager.getResourceMetadataPatternStore();
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
	 *
	 * @param oRes
	 * @param reference
	 * @throws CustomFormException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('pm(resourceMetadata, pattern)', 'R')")
	public void exportPattern(HttpServletResponse oRes, String reference) throws IOException,
			STPropertyAccessException, NoSuchConfigurationManager {
		ResourceMetadataPattern pattern = resourceMetadataManager.getResourceMetadataPattern(getProject(), reference);
		File tempServerFile = File.createTempFile("patternExport", ".cfg");
		try {
			STPropertiesManager.storeSTPropertiesInYAML(pattern, tempServerFile, true);
			oRes.setHeader("Content-Disposition", "attachment; filename=" + reference.substring(reference.indexOf(":") + 1) + ".cfg");
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

	private boolean associationExists(ResourceMetadataAssociationStore store, RDFResourceRole role, String patternRef)
			throws STPropertyAccessException {
		for (Reference associationRef : store.getConfigurationReferences(getProject(), null)) {
			ResourceMetadataAssociation association = store.getConfiguration(associationRef);
			if (association.role.equals(role) && association.patternReference.equals(patternRef)) {
				return true;
			}
		}
		return false;
	}

	private boolean patternExists(ResourceMetadataPatternStore store, String patternRef) {
		if (patternRef.startsWith("factory")) { //factory provided => check if it is among those stored
			return store.getFactoryConfigurationReferences().contains(patternRef);
		} else { //project level => check if exists the reference
			for (Reference ref : store.getConfigurationReferences(getProject(), null)) {
				if (ref.getRelativeReference().equals(patternRef)) {
					return true;
				}
			}
			return false;
		}
	}

}
