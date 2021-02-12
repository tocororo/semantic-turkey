package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Resources;

@Component
public class CustomFormManager {
	
	private static final String SYSTEM_LEVEL_ID = "SYSTEM";
	private static final String CUSTOM_FORMS_FOLDER_NAME = "customForms";
	private static final String FORM_COLLECTIONS_FOLDER_NAME = "formCollections";
	private static final String FORMS_FOLDER_NAME = "forms";
	
	//map projectName-CustomFormModel
	private Map<String, CustomFormModel> cfModelMap;
	
	//Workaround see getInstance() method
	private static CustomFormManager staticInstance; 
	
	@PostConstruct
	public void init() {
		/* at ST startup, initialize the structure only at system level. At project level will be initialized
		 * only when a project is opened and it will be terminated when the project is closed */
		cfModelMap = new HashMap<>();
		//init CustomFormModel at system level
		try {
			CustomFormModel cfModel = new CustomFormModel();
			cfModelMap.put(SYSTEM_LEVEL_ID, cfModel);
		} catch (CustomFormInitializationException e) {
			System.out.println("Custom Form mechanism has not been initialized due to an error in its system configuration file");
			e.printStackTrace();
		}
		staticInstance = this;
	}
	
	/**
	 * Return an instance of CustomFormManager.
	 * This is a workaround to use this Component as class with static methods in ProjectManager
	 * (ProjectManager is not a Component so CustomFormManager cannot be Autowired in it)
	 * @return
	 */
	public static CustomFormManager getInstance() {
		if (staticInstance == null) {
			throw new IllegalStateException("CustomFormManager is not yet initialized");
		}
		return staticInstance;
	}
	
	/**
	 * Initializes and registers the CustomForm structure for the given project (<code>projectName SYSTEM</code> to
	 * initialize at system level)
	 * @param project
	 */
	public void registerCustomFormModelOfProject(Project project) {
		try {
			CustomFormModel cfModel = new CustomFormModel(project, cfModelMap.get(SYSTEM_LEVEL_ID));
			cfModelMap.put(project.getName(), cfModel);
		} catch (CustomFormInitializationException e) {
			System.out.println("Custom Form model for " + project.getName() 
					+ " has not been initialized due to an error in its configuration file");
			e.printStackTrace();
		}
	}
	
	/**
	 * Deregisters the CustomForm structure for the given project
	 * @param project
	 */
	public void unregisterCustomFormModelOfProject(Project project) {
		cfModelMap.remove(project.getName());
	}
	
	
	/* ##################
	 * ###### READ ######
	 * ################## */
	
	/**
	 * Returns all the broken CF structures, both at system and project level.
	 * @param project
	 * @return
	 */
	public Collection<BrokenCFStructure> getBrokenCustomForms(Project project) {
		Collection<BrokenCFStructure> brokenCFs = new ArrayList<>();
		brokenCFs.addAll(cfModelMap.get(project.getName()).getBrokenCustomForms());
		brokenCFs.addAll(cfModelMap.get(SYSTEM_LEVEL_ID).getBrokenCustomForms());
		return brokenCFs;
	}
	
	// FORM MAPPING
	
	/**
	 * Returns the {@link FormsMapping} of the given project.
	 * @param project
	 * @return
	 */
	public Collection<FormsMapping> getProjectFormMappings(Project project) {
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel == null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return Collections.emptyList();
		} else {
			return cfModel.getFormMappings();
		}
	}
	
	/**
	 * Returns the {@link FormsMapping} at system level
	 * @return
	 */
	public Collection<FormsMapping> getSystemFormMappings() {
		CustomFormModel cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel == null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return Collections.emptyList();
		} else {
			return cfModel.getFormMappings();
		}
	}
	
	/**
	 * Returns the replace attribute of the {@link FormsMapping} of the given resource in the given project.
	 * If no FormsMapping is defined for the resource, and the fallback parameter is true, looks for FormsMapping at 
	 * system level.
	 * If no FormsMapping is defined at all for the resource return <code>false</code>.
	 * @param resource
	 * @param project
	 * @param fallback
	 * @return
	 */
	public boolean getReplace(Project project, IRI resource, boolean fallback) {
		boolean replace = false;
		
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			FormsMapping mapping = cfModel.getFormMapping(resource);
			if (mapping != null) {
				replace = mapping.getReplace();
			} else { //mapping not defined (null) at project level
				if (fallback) { //look at system level
					cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
					if (cfModel != null) {
						mapping = cfModel.getFormMapping(resource);
						if (mapping != null) {
							replace = mapping.getReplace();
						}
					}
				}
			}
		}
		return replace;
	}
	
	// FORM COLLECTION
	
	/**
	 * Returns all the {@link FormCollection}s at system and project level
	 * @return
	 */
	public Collection<FormCollection> getFormCollections(Project project) {
		Collection<FormCollection> formCollections = new ArrayList<>();
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			formCollections.addAll(cfModel.getFormCollections());
		}
		cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			formCollections.addAll(cfModel.getFormCollections());
		}
		return formCollections;
	}
	
	/**
	 * Given a resource (property or class) returns the {@link FormCollection} linked to that resource.
	 * <code>null</code> if no FormCollection is linked to the resource.
	 * @param resource
	 * @return
	 */
	public FormCollection getFormCollection(Project project, IRI resource) {
		FormCollection fc = null;
		//look only at project level since the mappings between resource and FC at system level are just
		//default/suggestion that are copied at project level in order to be editable
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			fc = cfModel.getFormCollectionForResource(resource);
		}
		return fc;
	}
	
	/**
	 * Returns the {@link FormCollection} with the given ID. It looks first at project level, then at system level.
	 * <code>null</code> if there is no FC with that id.
	 * @param formCollId
	 * @return
	 */
	public FormCollection getFormCollection(Project project, String formCollId){
		FormCollection fc = null;
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			fc = cfModel.getFormCollectionById(formCollId);
		}
		if (fc == null) { //FormCollection not found at project level, check a system level
			cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
			if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
				fc = cfModel.getFormCollectionById(formCollId);
			}
		}
		return fc;
	}
	
	/**
	 * Returns all the {@link FormCollection}s at system level
	 * @return
	 */
	public Collection<FormCollection> getSystemFormCollections() {
		CustomFormModel cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return cfModel.getFormCollections();
		} else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * Given a resource (property or class) returns the {@link FormCollection} linked to that resource at system level.
	 * <code>null</code> if no FormCollection is linked to the resource.
	 * @param resource
	 * @return
	 */
	public FormCollection getSystemFormCollection(IRI resource) {
		FormCollection fc = null;
		CustomFormModel cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			fc = cfModel.getFormCollectionForResource(resource);
		}
		return fc;
	}
	
	/**
	 * Returns the {@link FormCollection} with the given ID at system level. <code>null</code> if there is no FC with that id.
	 * @param formCollId
	 * @return
	 */
	public FormCollection getSystemFormCollection(String formCollId){
		FormCollection fc = null;
		CustomFormModel cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			fc = cfModel.getFormCollectionById(formCollId);
		}
		return fc;
	}
	
	/**
	 * Returns all the {@link FormCollection}s in the given project
	 * @return
	 */
	public Collection<FormCollection> getProjectFormCollections(Project project) {
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return cfModel.getFormCollections();
		} else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * Given a resource (property or class) returns the {@link FormCollection} linked to that resource in the given project.
	 * <code>null</code> if no FormCollection is linked to the resource.
	 * @param resource
	 * @return
	 */
	public FormCollection getProjectFormCollection(Project project, IRI resource) {
		FormCollection fc = null;
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			fc = cfModel.getFormCollectionForResource(resource);
		}
		return fc;
	}
	
	/**
	 * Returns the {@link FormCollection} with the given ID in the given project.
	 * <code>null</code> if there is no FC with that id.
	 * @param formCollId
	 * @return
	 */
	public FormCollection getProjectFormCollection(Project project, String formCollId){
		FormCollection fc = null;
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			fc = cfModel.getFormCollectionById(formCollId);
		}
		return fc;
	}
	
	// CUSTOM FORM
	
	/**
	 * Returns all the {@link CustomForm}s at system and project level
	 * @return
	 */
	public Collection<CustomForm> getCustomForms(Project project) {
		Collection<CustomForm> customForms = new ArrayList<>();
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			customForms.addAll(cfModel.getCustomForms());
		}
		cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			customForms.addAll(cfModel.getCustomForms());
		}
		return customForms;
	}
	
	/**
	 * Returns the {@link CustomForm} with the given ID. It looks first at project level, then at system level.
	 * @param project
	 * @param customFormId
	 * @return
	 */
	public CustomForm getCustomForm(Project project, String customFormId){
		CustomForm cf = null; 
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			cf = cfModel.getCustomFormById(customFormId);
			if (cf == null) { //cf not found at project level, look at system level
				cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
				if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
					cf = cfModel.getCustomFormById(customFormId);
				}
			}
		}
		return cf;
	}
	
	/**
	 * Returns the {@link CustomForm} for the given resource. It looks at project level and system level
	 * If the resource has not a {@link FormCollection} linked, then returns an empty collection.
	 * @param project 
	 * @param resource
	 * @return
	 */
	public Collection<CustomForm> getCustomForms(Project project, IRI resource){
		Collection<CustomForm> customForms = new ArrayList<>();
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			customForms.addAll(cfModel.getCustomFormForResource(resource));
		}
		cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			customForms.addAll(cfModel.getCustomFormForResource(resource));
		}
		return customForms;
	}
	
	/**
	 * Returns all the {@link CustomForm}s at system level
	 * @return
	 */
	public Collection<CustomForm> getSystemCustomForms() {
		CustomFormModel cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return cfModel.getCustomForms();
		} else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * Returns the {@link CustomForm} with the given ID at system level
	 * @param customFormId
	 * @return
	 */
	public CustomForm getSystemCustomForm(String customFormId){
		CustomForm cf = null;
		CustomFormModel cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			cf = cfModel.getCustomFormById(customFormId);
		}
		return cf;
	}
	
	/**
	 * Returns all the {@link CustomForm} for the given resource at system level.
	 * If the resource has not a {@link FormCollection} linked, then returns an empty collection 
	 * @param resource
	 * @return
	 */
	public Collection<CustomForm> getSystemCustomForms(IRI resource){
		CustomFormModel cfModel = cfModelMap.get(SYSTEM_LEVEL_ID);
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return cfModel.getCustomFormForResource(resource);
		} else {
			return Collections.emptyList();
		}
		
	}
	
	/**
	 * Returns all the {@link CustomForm}s for the given project
	 * @return
	 */
	public Collection<CustomForm> getProjectCustomForms(Project project) {
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return cfModel.getCustomForms();
		} else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * Returns all the {@link CustomForm} for the given resource of the given project.
	 * If the resource has not a {@link FormCollection} linked, then returns an empty collection.
	 * @param project 
	 * @param resource
	 * @return
	 */
	public Collection<CustomForm> getProjectCustomForms(Project project, IRI resource){
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			return cfModel.getCustomFormForResource(resource);
		} else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * Returns the {@link CustomForm} with the given ID of the given project
	 * @param project
	 * @param customFormId
	 * @return
	 */
	public CustomForm getProjectCustomForm(Project project, String customFormId){
		CustomForm cf = null;
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			cf = cfModel.getCustomFormById(customFormId);
		}
		return cf;
	}
	
	/**
	 * Returns all the {@link CustomFormGraph} for the given resource (at project and stystem level)
	 * @param resource
	 * @return
	 */
	public Collection<CustomFormGraph> getAllCustomFormGraphs(Project project, IRI resource){
		Collection<CustomFormGraph> cFormsGraph = new ArrayList<>();
		/* Here look for CF only at project level. Since I retrieve all CFGraph in two steps
		 * - FormCollection of the given resource
		 * - get CustomForms of the FormCollection found in the above step 
		 * The mappings resource-FormCollection at project level may contain already references of FormCollections
		 * at system level, so the CustomForm referenced in turn by these FormCollections are those at system level
		 */
		CustomFormModel cfModel = cfModelMap.get(project.getName());
		if (cfModel != null) { //check necessary to avoid Exception in case the initialization of the CustomFormModel failed
			cFormsGraph.addAll(cfModel.getCustomFormGraphForResource(resource));
		}
		return cFormsGraph;
	}
	
	/**
	 * Tells whether a resource has or not a {@link CustomForm} of type graph 
	 * @param resource
	 * @return
	 */
	public boolean existsCustomFormGraphForResource(Project project, IRI resource){
		return (!getAllCustomFormGraphs(project, resource).isEmpty());
	}




	/**
	 * Returns the CustomFormGraph that probably generated the reified resource. If for the given resource
	 * there is no CustomForm available returns null, if there's just one CustomForm then return it, otherwise
	 * if there are multiple CustomForm returns the one which its PEARL fits better the given reified resource
	 * description.
	 *
	 * @param project
	 * @param codaCore
	 * @param repoConnection
	 * @param resource
	 * @param predicateOrClasses
	 * @param includeInferred
	 * @return
	 * @throws RDFModelNotSetException
	 */
	public CustomFormGraph getCustomFormGraphSeed(Project project, CODACore codaCore, RepositoryConnection repoConnection,
			Resource resource, Collection<IRI> predicateOrClasses, boolean includeInferred) {

		if (predicateOrClasses.isEmpty()) { // edge case when no predicate or class is given
			return null;
		}

		Collection<CustomFormGraph> cForms = predicateOrClasses.stream().flatMap(
				aPredOrClass -> getAllCustomFormGraphs(project, aPredOrClass).stream())
				.collect(Collectors.toList());

		if (cForms.isEmpty())  { // no custom form --> just return null
			return null;
		} else if (cForms.size() == 1) { // only one custom form --> return it
			return cForms.iterator().next();
		} else { // cForms.size() > 1
			// return the CF whose graph section matches more triples in the model
			int maxStats = 0;
			CustomFormGraph bestCF = null;
			for (CustomFormGraph cf : cForms) {
				try {
					// creating the construct query
					StringBuilder queryBuilder = new StringBuilder();
					queryBuilder.append("construct { ");
					queryBuilder.append(cf.getGraphSectionAsString(codaCore, false));
					queryBuilder.append(" } where { ");
					queryBuilder.append(cf.getGraphSectionAsString(codaCore, true));
					queryBuilder.append(" }");
					String query = queryBuilder.toString();

					GraphQuery gq = repoConnection.prepareGraphQuery(query);
					gq.setBinding(cf.getEntryPointPlaceholder(codaCore).substring(1), resource);
					gq.setIncludeInferred(includeInferred);

					try (GraphQueryResult result = gq.evaluate()) {
						int nStats = QueryResults.asModel(result).size();
						if (nStats > maxStats) {
							maxStats = nStats;
							bestCF = cf;
						}
					}
				} catch (PRParserException e) {
					// if one of the CF contains an error, catch the exception and continue checking the other
					// CFs
					System.out.println("Parsing error in PEARL rule of CustomForm with ID " + cf.getId()
							+ ". " + "The CustomForm will be ignored, please fix its PEARL rule.");
				}
			}
			return bestCF;
		}
	}
	
	/* ##################
	 * ##### CREATE #####
	 * ################## */
	
	// FORM MAPPING
	
	/**
	 * Adds a {@link FormsMapping} (mapping between resource and {@link FormCollection}) to the configuration
	 * of the given project.
	 * If a {@link FormCollection} is already assigned to the given resource, throws a {@link CustomFormException}.
	 * @param resource
	 * @param formColl
	 * @param replace
	 * @param project
	 * @return 
	 * @throws CustomFormException
	 */
	public FormsMapping addFormsMapping(Project project, IRI resource, FormCollection formColl, boolean replace) throws CustomFormException {
		return cfModelMap.get(project.getName()).addFormsMapping(resource, formColl, replace);
	}
	
	// FORM COLLECTION
	
	/**
	 * Creates and adds a FormCollection in the given project. If in the project, a {@link FormCollection}
	 * with the same ID exists, a {@link DuplicateIdException} is thrown
	 * @param id
	 * @return
	 * @throws DuplicateIdException 
	 */
	public FormCollection createFormCollection(Project project, String id) throws DuplicateIdException {
		//check if a FormCollection with the same ID already exists at system level
		if (cfModelMap.get(SYSTEM_LEVEL_ID).getFormCollectionById(id) != null) {
			throw new DuplicateIdException("A FormCollection with id '" + id + "' already exists at system level");
		}
		return cfModelMap.get(project.getName()).createFormCollection(id);
	}
	
	// CUSTOM FORM
	
	/**
	 * Creates and adds a CustomFormGraph in the given project. If in the project, a {@link CustomForm}
	 * with the same ID exists, a {@link DuplicateIdException} is thrown
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @param showPropChain
	 * @return
	 * @throws DuplicateIdException 
	 */
	public CustomFormGraph createCustomFormGraph(Project project, String id, String name, String description, String ref,
		 	List<IRI> showPropChain, List<IRI> previewTableProps) throws DuplicateIdException {
		//check if a FormCollection with the same ID already exists at system level
		if (cfModelMap.get(SYSTEM_LEVEL_ID).getCustomFormById(id) != null) {
			throw new DuplicateIdException("A CustomForm with id '" + id + "' already exists at system level");
		}
		return cfModelMap.get(project.getName()).createCustomFormGraph(id, name, description, ref, showPropChain, previewTableProps);
	}

	/**
	 * Creates and adds a CustomFormNode in the given project. If in the project, a {@link CustomForm}
	 * with the same ID exists, a {@link DuplicateIdException} is thrown
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @return
	 * @throws DuplicateIdException
	 */
	public CustomFormNode createCustomFormNode(Project project, String id, String name, String description, String ref)
			throws DuplicateIdException {
		//check if a FormCollection with the same ID already exists at system level
		if (cfModelMap.get(SYSTEM_LEVEL_ID).getCustomFormById(id) != null) {
			throw new DuplicateIdException("A CustomForm with id '" + id + "' already exists at system level");
		}
		return cfModelMap.get(project.getName()).createCustomFormNode(id, name, description, ref);
	}
	
	/* ##################
	 * ##### UPDATE #####
	 * ################## */
	
	// FORM MAPPING
	
	public void setReplace(Project project, IRI resource, boolean replace) throws CustomFormException {
		cfModelMap.get(project.getName()).setReplace(resource, replace);
	}
	
	/**
	 * Removes a {@link FormsMapping} (mapping between resource and {@link FormCollection}) from the configuration
	 * of the given project.
	 * @param resource
	 * @param project
	 */
	public void removeFormsMapping(Project project, IRI resource) {
		cfModelMap.get(project.getName()).removeFormsMapping(resource);
	}
	
	// FORM COLLECTION
	
	public void updateFormCollection(Project project, FormCollection formColl, Collection<CustomForm> customForms, Collection<IRI> suggestions) {
		cfModelMap.get(project.getName()).updateFormCollection(formColl, customForms, suggestions);
	}
	
	/**
	 * Removes a {@link FormCollection} from the configuration of the given project and its file from file-system
	 * @param project
	 * @param formColl
	 */
	public void deleteFormCollection(Project project, FormCollection formColl) {
		cfModelMap.get(project.getName()).deleteFormCollection(formColl);
	}
	
	// CUSTOM FORM
	
	public void updateCustomFormNode(Project project, CustomFormNode customForm, String name, String description, String ref) {
		cfModelMap.get(project.getName()).updateCustomFormNode(customForm, name, description, ref);
	}

	public void updateCustomFormGraph(Project project, CustomFormGraph customForm, String name, String description,
		  String ref, List<IRI> showPropChain, List<IRI> previewTableProps) {
		cfModelMap.get(project.getName()).updateCustomFormGraph(customForm, name, description, ref, showPropChain, previewTableProps);
	}
	
	/**
	 * Removes a CustomForm from the form collection of a project
	 * @param customForm
	 * @param deleteEmptyColl if true deletes FormCollection that are left empty after the deletion
	 */
	public void deleteCustomForm(Project project, CustomForm customForm, boolean deleteEmptyColl)  {
		cfModelMap.get(project.getName()).deleteCustomForm(customForm, deleteEmptyColl);
	}
	
	/* #############################################
	 * ################# Utility ###################
	 * ############################################*/
	
	/**
	 * Returns the customForms folder for the given project, or at system level if projectName is <code>null</code>
	 * @return
	 */
	public static File getCustomFormsFolder(Project project){
		if (project == null) {
			return new File(Resources.getSystemDir() + File.separator + CUSTOM_FORMS_FOLDER_NAME); 
		} else {
			return new File(Resources.getProjectsDir() + File.separator + project.getName() + File.separator + CUSTOM_FORMS_FOLDER_NAME);
		}
	}
	
	/**
	 * Returns the formCollections folder for the given project, or at system level if projectName is <code>null</code>
	 * @return
	 */
	public static File getFormCollectionsFolder(Project project){
		return new File(getCustomFormsFolder(project) + File.separator + FORM_COLLECTIONS_FOLDER_NAME);
	}
	
	/**
	 * Returns the forms folder for the given project, or at system level if projectName is <code>null</code>
	 * @return
	 */
	public static File getFormsFolder(Project project){
		return new File(getCustomFormsFolder(project) + File.separator + FORMS_FOLDER_NAME);
	}
	
	//for debugging
	@SuppressWarnings("unused")
	private void printModels() {
		System.out.println("CustomForm model map:");
		for (Entry<String, CustomFormModel> entry : cfModelMap.entrySet()) {
			System.out.println("Project: " + entry.getKey());
			CustomFormModel cfModel = entry.getValue();
			System.out.println("Model: " + cfModel);
		}
	}
	
}
