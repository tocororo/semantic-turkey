package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.project.Project;

public class CustomFormModel {
	
	private static Logger logger = LoggerFactory.getLogger(CustomFormModel.class);
	
	private CustomFormsConfig cfConfig;
	private Collection<FormCollection> formCollections;
	private Collection<CustomForm> customForms;
	
	private Project project;
	
	/**
	 * Creates CustomFormModel at system level
	 * @throws CustomFormParseException 
	 */
	public CustomFormModel() throws CustomFormParseException {
		logger.debug("Loading Custom Form architecture at system level");
		//CustomForm: load files from forms/ folder and store them in the forms map
		customForms = CustomFormXMLHelper.loadSystemCustomForms();
		//FormCollection: load files from formCollections/ folder
		formCollections = CustomFormXMLHelper.loadSystemFormCollections(customForms);
		//CustomFormConfig: create config with mappings loaded from system customForms/customFormsConfig.xml file
		cfConfig = new CustomFormsConfig(CustomFormXMLHelper.loadSysyemFormsMapping(formCollections));
	}
	
	/**
	 * Creates CustomFormModel at project level
	 * @throws CustomFormParseException
	 * @throws CustomFormInitializationException 
	 */
	public CustomFormModel(Project project, CustomFormModel systemCFModel) throws CustomFormParseException, CustomFormInitializationException {
		
		this.project = project;
		
		File cfFolder = CustomFormManager.getCustomFormsFolder(project);
		if (!cfFolder.exists()) {
			logger.debug("CustomForm folders architecture for project " + project.getName() + " doesn't exists. Initializing it.");
			initializeProjectCFBasicHierarchy(systemCFModel);
		} else {
			logger.debug("CustomForm folders architecture for project " + project.getName() + " exists. Loading it.");
			//CustomForm: load files from forms/ folder and store them in the forms map
			customForms = CustomFormXMLHelper.loadProjectCustomForms(project, systemCFModel.getCustomForms());
			//FormCollection: load files from formCollections/ folder
			formCollections = CustomFormXMLHelper.loadProjectFormCollections(project, customForms, systemCFModel.getCustomForms());
			//CustomFormConfig: create config with mappings loaded from project customForms/customFormsConfig.xml file
			cfConfig = new CustomFormsConfig(CustomFormXMLHelper.loadProjectFormsMapping(project, formCollections, systemCFModel.getFormCollections()));
		}
	}
	
	/* ##################
	 * ###### READ ######
	 * ################## */
	
	// FORM MAPPING
	
	/**
	 * Returns the {@link FormsMapping}s in the current project.
	 * @return
	 */
	public Collection<FormsMapping> getFormMappings() {
		return cfConfig.getFormsMappings();
	}
	
	/**
	 * Returns the {@link FormsMapping} linked to the given resource. <code>null</code> if no FormsMapping is linked. 
	 * @param resource
	 * @return
	 */
	public FormsMapping getFormMapping(IRI resource) {
		return cfConfig.getFormsMapping(resource);
	}
	
	// FORM COLLECTION
	
	/**
	 * Returns all the {@link FormCollection}
	 * @return
	 */
	public Collection<FormCollection> getFormCollections() {
		return formCollections;
	}
	
	/**
	 * Given a resource URI (property or class) returns the {@link FormCollection} linked to that resource in the configuration.
	 * <code>null</code> if no FormCollection is linked to the resource.
	 * @param resource
	 * @return
	 */
	public FormCollection getFormCollectionForResource(IRI resource) {
		return cfConfig.getFormCollectionForResource(resource);
	}
	
	/**
	 * Returns the {@link FormCollection} with the given ID. <code>null</code> if there is no FC with that id.
	 * @param formCollId
	 * @return
	 */
	public FormCollection getFormCollectionById(String formCollId){
		for (FormCollection fc : formCollections) {
			if (fc.getId().equals(formCollId)) {
				return fc;
			}
		}
		return null;
	}
	
	// CUSTOM FORM
	
	/**
	 * Returns all the {@link CustomForm}s
	 * @return
	 */
	public Collection<CustomForm> getCustomForms() {
		return customForms;
	}
	
	/**
	 * Returns the {@link CustomForm} with the given ID of the given project
	 * @param customFormId
	 * @return
	 */
	public CustomForm getCustomFormById(String customFormId){
		for (CustomForm cf : customForms) {
			if (cf.getId().equals(customFormId)) {
				return cf;
			}
		}
		return null;
	}
	
	/**
	 * Returns all the {@link CustomForm} for the given resource.
	 * If the resource has not a {@link FormCollection} linked, then returns an empty collection.
	 * @param resource
	 * @return
	 */
	public Collection<CustomForm> getCustomFormForResource(IRI resource){
		Collection<CustomForm> cForms = new ArrayList<CustomForm>();
		FormCollection formColl = cfConfig.getFormCollectionForResource(resource);
		if (formColl != null) {
			cForms.addAll(formColl.getForms());
		}
		return cForms;
	}
	
	/**
	 * Returns all the {@link CustomFormGraph} for the given resource
	 * @param resource
	 * @return
	 */
	public Collection<CustomFormGraph> getCustomFormGraphForResource(IRI resource){
		Collection<CustomFormGraph> cFormsGraph = new ArrayList<>();
		FormCollection formColl = cfConfig.getFormCollectionForResource(resource);
		if (formColl != null) {
			for (CustomForm form : formColl.getForms()) {
				if (form.isTypeGraph()) {
					cFormsGraph.add(form.asCustomFormGraph());
				}
			}
		}
		return cFormsGraph;
	}
	
	/* ##################
	 * ##### CREATE #####
	 * ################## */
	
	// FORM MAPPING
	
	/**
	 * Adds a {@link FormsMapping} (mapping between resource and {@link FormCollection}) to the configuration.
	 * If a {@link FormCollection} is already assigned to the given resource, throws a {@link CustomFormException}.
	 * @param resource
	 * @param formColl
	 * @param replace
	 * @return 
	 * @throws CustomFormException
	 */
	public FormsMapping addFormsMapping(IRI resource, FormCollection formColl, boolean replace) throws CustomFormException {
		//check if the resource has already a FormCollection linked
		if (cfConfig.getFormCollectionForResource(resource) != null) {
			throw new CustomFormException(resource.stringValue() + " has already a FormCollection assigned ("
					+ formColl.getId() + ")");
		}
		FormsMapping formMapping = new FormsMapping(resource.stringValue(), formColl, replace);
		cfConfig.addFormsMapping(formMapping);
		cfConfig.save(new File(CustomFormManager.getCustomFormsFolder(project), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME));
		return formMapping;
	}
	
	// FORM COLLECTION
	
	/**
	 * Creates and adds a FormCollection. If a {@link FormCollection} with the same ID exists,
	 * a {@link DuplicateIdException} is thrown
	 * @param id
	 * @return
	 * @throws DuplicateIdException 
	 */
	public FormCollection createFormCollection(String id) throws DuplicateIdException {
		if (getFormCollectionById(id) != null) {
			throw new DuplicateIdException("A FormCollection with id '" + id + "' already exists in project " + project.getName());
		}
		FormCollection formColl = new FormCollection(id);
		formCollections.add(formColl);
		// serialize the FormCollection
		formColl.save(new File(CustomFormManager.getFormCollectionsFolder(project), id + ".xml"));
		return formColl;
	}
	
	// CUSTOM FORM
	
	/**
	 * Creates and adds a CustomForm. If a {@link CustomForm} with the same ID exists,
	 * a {@link DuplicateIdException} is thrown
	 * @param type
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @param showPropChain
	 * @return
	 * @throws DuplicateIdException 
	 */
	public CustomForm createCustomForm(String type, String id, String name, String description, String ref, List<IRI> showPropChain)
			throws DuplicateIdException {
		if (getCustomFormById(id) != null) {
			throw new DuplicateIdException("A FormCollection with id '" + id + "' already exists in project " + project.getName());
		}
		CustomForm form = null;
		if (type.equalsIgnoreCase(CustomForm.Types.node.toString())) {
			form = new CustomFormNode(id, name, description, ref);
		} else {
			form = new CustomFormGraph(id, name, description, ref, showPropChain);
		}
		customForms.add(form);
		// serialize the CustomForm (only if add() doesn't throw a DuplicateIdException)
		form.save(new File(CustomFormManager.getFormsFolder(project), id + ".xml"));
		return form;
	}
	
	/* ##################
	 * ##### UPDATE #####
	 * ################## */
	
	// FORM MAPPING
	
	public void setReplace(IRI resource, boolean replace) throws CustomFormException {
		FormsMapping formsMapping = cfConfig.getFormsMapping(resource);
		if (formsMapping == null) {
			throw new CustomFormException("No FormsMapping found for " + resource.stringValue() + " in project " + project.getName());
		}
		formsMapping.setReplace(replace);
		cfConfig.save(new File(CustomFormManager.getCustomFormsFolder(project), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME));
	}
	
	/**
	 * Removes a {@link FormsMapping} (mapping between resource and {@link FormCollection}) from the configuration.
	 * @param resource
	 */
	public void removeFormsMapping(IRI resource) {
		cfConfig.removeMappingOfResource(resource);
		cfConfig.save(new File(CustomFormManager.getCustomFormsFolder(project), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME));
	}
	
	// FORM COLLECTION
	
	/**
	 * Removes a {@link FormCollection} from the configuration and its file from file-system
	 * @param formCollId
	 */
	public void deleteFormCollection(FormCollection formColl) {
		Iterator<FormCollection> itFc = formCollections.iterator();
		while (itFc.hasNext()) {
			FormCollection fc = itFc.next();
			if (fc.getId().equals(formColl.getId())) {
				itFc.remove();
			}
		}
		// delete the file
		File[] formCollFiles = CustomFormManager.getFormCollectionsFolder(project).listFiles();
		for (File f : formCollFiles) {// search for the formCollection file with the given id/name
			if (f.getName().equals(formColl.getId() + ".xml")) {
				f.delete();
				break;
			}
		}
		// remove FormsMappings about the given FormCollection
		cfConfig.removeMappingOfFormCollection(formColl);
		cfConfig.save(new File(CustomFormManager.getCustomFormsFolder(project), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME));
	}
	
	// CUSTOM FORM
	
	public void updateCustomForm(CustomForm customForm, String name, String description, String ref, List<IRI> showPropChain) {
		customForm.setName(name);
		customForm.setDescription(description);
		customForm.setRef(ref);
		if (showPropChain != null) {
			customForm.asCustomFormGraph().setShowPropertyChain(showPropChain);
		}
		customForm.save(new File(CustomFormManager.getFormsFolder(project), customForm.getId() + ".xml"));
	}
	
	/**
	 * Removes a CustomForm from the form collection of a project
	 * @param customForm
	 * @param deleteEmptyColl if true deletes FormCollection that are left empty after the deletion
	 */
	public void deleteCustomForm(CustomForm customForm, boolean deleteEmptyColl) {
		
		Iterator<CustomForm> itCf = customForms.iterator();
		while (itCf.hasNext()) {
			CustomForm cf = itCf.next();
			if (cf.getId().equals(customForm.getId())) {
				itCf.remove();
			}
		}
		
		// delete the CF file
		File[] cfFiles = CustomFormManager.getFormsFolder(project).listFiles();
		for (File f : cfFiles) {// search for the custom form file with the given id/name
			if (f.getName().equals(customForm + ".xml")) {
				f.delete();
				break;
			}
		}
		// remove the entry from the FormCollection(s) that use it
		Collection<FormCollection> formCollections = getFormCollections();
		for (FormCollection formColl : formCollections) {
			if (formColl.containsForm(customForm.getId())) {
				formColl.removeForm(customForm);
				// if deleteEmptyColl is true and collection is left empty, delete it
				if (deleteEmptyColl && formColl.getForms().isEmpty()) {
					deleteFormCollection(formColl);
				} else { // otherwise save it
					formColl.save(new File(CustomFormManager.getFormCollectionsFolder(project), formColl.getId() + ".xml"));
				}
			}
		}
	}
	
	public void addFormToCollection(FormCollection formColl, CustomForm customForm) {
		formColl.addForm(customForm);
		formColl.save(new File(CustomFormManager.getFormCollectionsFolder(project), formColl.getId() + ".xml"));
	}
	
	public void addFormsToCollection(FormCollection formColl, Collection<CustomForm> customForms) {
		for (CustomForm cf : customForms) {
			formColl.addForm(cf);
		}
		formColl.save(new File(CustomFormManager.getFormCollectionsFolder(project), formColl.getId() + ".xml"));
	}
	
	public void removeFormFromCollection(FormCollection formColl, CustomForm customForm) {
		formColl.removeForm(customForm.getId());
		formColl.save(new File(CustomFormManager.getFormCollectionsFolder(project), formColl.getId() + ".xml"));
	}
	
	
	/* #############################################
	 * ################# Utility ###################
	 * ############################################*/
	
	/**
	 * Creates the following hierarchy under the project's folder:
	 *   <ul>
	 *    <li>customForms</li>
	 *     <ul>
	 *      <li>customFormConfig.xml</li>
	 *      <li>forms/</li>
	 *      <li>formCollections</li>
	 *     </ul>
	 *   </ul>
	 * Where <code>customFormConfig.xml</code> is copied from the same at system level.
	 * This is invoked when a project is accessed and this structure doesn't exist.
	 * @param systemCFModel 
	 */
	private void initializeProjectCFBasicHierarchy(CustomFormModel systemCFModel) {
		//creates folders
		CustomFormManager.getCustomFormsFolder(project).mkdir();
		CustomFormManager.getFormCollectionsFolder(project).mkdir();
		CustomFormManager.getFormsFolder(project).mkdir();

		//initialize customForms, formCollections and cfConfig
		customForms = new ArrayList<>();
		formCollections = new ArrayList<>();
		// create a configuration that is a clone of the one at system level
		cfConfig = new CustomFormsConfig(systemCFModel.getFormMappings());
		// ...and save it
		cfConfig.save(new File(CustomFormManager.getCustomFormsFolder(project), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME));
	}
	
}
