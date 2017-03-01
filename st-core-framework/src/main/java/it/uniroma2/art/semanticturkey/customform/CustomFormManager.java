package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.resources.Resources;

@Component
public class CustomFormManager {
	
	private static final String CUSTOM_FORMS_FOLDER_NAME = "customForms";
	private static final String FORM_COLLECTIONS_FOLDER_NAME = "formCollections";
	private static final String FORMS_FOLDER_NAME = "forms";
	
	private CustomFormsConfig cfConfig;
	
	//Map of id-FormCollection object pairs (contains all the {@link FormCollection}, also the ones that are not
	//reachable from the config tree, namely the ones that are not associated to any property or class)
	private Map<String, FormCollection> formCollMap = new HashMap<>();
	//Map of id-CustomForm object pairs (contains all the {@link CustomForm}, also the ones that are not reachable
	//from the config tree, namely the ones that are not contained in any {@link FormCollection})
	private Map<String, CustomForm> formsMap = new HashMap<>();
	
	private static Logger logger = LoggerFactory.getLogger(CustomFormManager.class);
	
	@PostConstruct
	public void init() {
		try {
			// check existing of customForms folders hierarchy
			File cfFolder = getCustomFormsFolder();
			if (!cfFolder.exists()) {
				//TODO here create CFC without a basic CFC file that is initalized in initializeCFBasicHierarchy
				cfConfig = new CustomFormsConfig(formCollMap, formsMap);
				initializeCFBasicHierarchy();
			} else {
				File formsFolder = getFormsFolder();
				if (!formsFolder.exists()) {
					formsFolder.mkdir();
				}
				File formCollFolder = getFormCollectionsFolder();
				if (!formCollFolder.exists()) {
					formCollFolder.mkdir();
				}
				// initialize CustomForm list (load files from forms/ folder) and store them in the forms map
				File[] formFiles = formsFolder.listFiles();
				for (File f : formFiles) {
					if (f.getName().startsWith(CustomForm.PREFIX)) {
						try {
							CustomForm cf = CustomFormLoader.loadCustomForm(f);
							formsMap.put(cf.getId(), cf);
						} catch (CustomFormException e) {
							System.out.println("Failed to initialize CustomForm from file " + f.getName()
									+ ". It may contain some errors.");
						}
					}
				}
				// initialize FormCollection list (load files from formCollections/ folder) and store them in the
				// formColl map
				File[] formCollFiles = formCollFolder.listFiles();
				for (File f : formCollFiles) {
					if (f.getName().startsWith(FormCollection.PREFIX)) {
						FormCollection formColl = CustomFormLoader.loadFormCollection(f, formsMap);
						formCollMap.put(formColl.getId(), formColl);
					}
				}
				cfConfig = new CustomFormsConfig(formCollMap, formsMap);
			}
		} catch (CustomFormInitializationException e) {
			System.out.println("Custom Form mechanism has not been initialized due to an error in its configuration file");
			e.printStackTrace();
		}
	}
	
	public CustomFormsConfig getCustomFormsConfig(){
		return cfConfig;
	}
	
	/* ##################
	 * ###### READ ######
	 * ################## */
	
	// FORM COLLECTION
	
	/**
	 * Returns all the {@link FormCollection} available into the customForms/formCollections folder
	 * @return
	 */
	public Collection<FormCollection> getAllFormCollections() {
		return formCollMap.values();
	}
	
	/**
	 * Given a resource URI (property or class) returns the {@link FormCollection} linked to that resource.
	 * <code>null</code> if no FormCollection is linked to the resource.
	 * @param resourceUri
	 * @return
	 */
	public FormCollection getFormCollectionForResource(String resourceUri) {
		return cfConfig.getFormCollectionForResource(resourceUri);
	}
	
	/**
	 * Returns the {@link FormCollection} with the given ID. <code>null</code> if there is no FC with that id.
	 * @param formCollId
	 * @return
	 */
	public FormCollection getFormCollectionById(String formCollId){
		return formCollMap.get(formCollId);
	}
	
	// CUSTOM FORM
	
	/**
	 * Returns all the {@link CustomForm} available into the forms/ folder
	 * @return
	 */
	public Collection<CustomForm> getAllCustomForms() {
		return formsMap.values();
	}
	
	/**
	 * Returns the {@link CustomForm} with the given ID
	 * @param cfId
	 * @return
	 */
	public CustomForm getCustomFormById(String cfId){
		return formsMap.get(cfId);
	}
	
	/**
	 * Returns all the {@link CustomForm} for the given resource. If the resource has not a {@link FormCollection}
	 * linked, then returns an empty collection 
	 * @param resourceUri
	 * @return
	 */
	public Collection<CustomForm> getCustomFormForResource(String resourceUri){
		FormCollection fc = getFormCollectionForResource(resourceUri);
		if (fc != null){
			return fc.getForms();	
		}
		else return new ArrayList<CustomForm>();
	}
	
	/**
	 * Returns all the {@link CustomFormGraph} for the given resource
	 * @param resourceUri
	 * @return
	 */
	public Collection<CustomFormGraph> getCustomFormGraphForResource(String resourceUri){
		Collection<CustomFormGraph> cFormsGraph = new ArrayList<>();
		FormCollection fc = getFormCollectionForResource(resourceUri);
		if (fc != null){
			for (CustomForm form : fc.getForms()){
				if (form.isTypeGraph()) {
					cFormsGraph.add(form.asCustomFormGraph());
				}
			}
		}
		return cFormsGraph;
	}
	
	/**
	 * Returns all the {@link CustomFormNode} for the given property
	 * @param resourceUri
	 * @return
	 */
	public Collection<CustomFormNode> getCustomFormNodeForResource(String resourceUri){
		Collection<CustomFormNode> cFormsNode = new ArrayList<>();
		FormCollection fc = getFormCollectionForResource(resourceUri);
		if (fc != null){
			for (CustomForm form : fc.getForms()){
				if (form.isTypeNode()) {
					cFormsNode.add(form.asCustomFormNode());
				}
			}
		}
		return cFormsNode;
	}
	
	/**
	 * Tells whether a resource has or not a {@link CustomForm} of type graph 
	 * @param resourceUri
	 * @return
	 */
	public boolean existsCustomFormGraphForResource(String resourceUri){
		return (!getCustomFormGraphForResource(resourceUri).isEmpty());
	}
	
	/* ##################
	 * ##### CREATE #####
	 * ################## */
	
	// FORM COLLECTION
	
	public FormCollection createFormCollection(String id) throws CustomFormException {
		//if already exists throw an exception
		if (this.getFormCollectionById(id) != null) {
			throw new CustomFormException("Impossible to create a FormCollection with  ID '"
					+ id + "'. A FormCollection with the same ID already exists");
		} else { //otherwise create the FormCollection
			FormCollection formColl = new FormCollection(id);
			formColl.saveXML(); //serialize it
			formCollMap.put(id, formColl); //and add it to the map
			return formColl;
		}
	}
	
	// CUSTOM FORM
	
	public CustomForm createCustomForm(String type, String id, String name, String description, String ref, List<IRI> showPropChain)
			throws CustomFormException {
		//if already exists throw an exception
		if (this.getCustomFormById(id) != null){
			throw new CustomFormException("Impossible to create a CustomForm with ID '"
					+ id + "'. A CustomForm with the same ID already exists");
		} else { //otherwise create the CustomForm
			CustomForm form = null;
			if (type.equalsIgnoreCase(CustomForm.Types.node.toString())){
				form = new CustomFormNode(id, name, description, ref);
			} else {
				form = new CustomFormGraph(id, name, description, ref, showPropChain);
			}
			form.saveXML(); //serialize it
			formsMap.put(id, form); //and add it to the map
			return form;
		}
	}
	
	/* ##################
	 * ##### UPDATE #####
	 * ################## */
	
	public void setReplace(IRI resource, boolean replace) {
		cfConfig.setReplace(resource.stringValue(), replace);
		cfConfig.saveXML();
	}
	
	// FORM COLLECTION
	
	public void addFormsMapping(IRI resource, FormCollection formColl, boolean replace) throws CustomFormException {
		//check if the resource has already a FormCollection linked
		if (cfConfig.getFormCollectionForResource(resource.stringValue()) != null) {
			throw new CustomFormException(resource.stringValue() + " has already a FormCollection assigned ("
					+ formColl.getId() + ")");
		}
		FormCollectionMapping formMapping = new FormCollectionMapping(resource.stringValue(), formColl, replace);
		cfConfig.addFormsMapping(formMapping);
		cfConfig.saveXML();
	}
	
	public void removeFormCollectionOfResource(IRI resource) throws CustomFormException {
		cfConfig.removeMappingOfResource(resource.stringValue());
		cfConfig.saveXML();
	}
	
	/**
	 * Removes a {@link FormCollection} from the configuration and its file from file-system
	 * @param formCollId
	 */
	public void deleteFormCollection(String formCollId) {
		formCollMap.remove(formCollId); // remove formColl from map
		// delete the file
		File[] formCollFiles = getFormCollectionsFolder().listFiles();
		for (File f : formCollFiles) {// search for the formCollection file with the given id/name
			if (f.getName().equals(formCollId + ".xml")) {
				f.delete();
				break;
			}
		}
		// remove FormsMapping about the given FormCollection
		cfConfig.removeMappingOfFormCollection(formCollId);
		cfConfig.saveXML();
	}
	
	// CUSTOM FORM
	
	public void updateCustomForm(String id, String name, String description, String ref, List<IRI> showPropChain)
			throws CustomFormException {
		CustomForm cf = this.getCustomFormById(id);
		if (cf == null) {
			throw new CustomFormException("CustomForm with ID " + id + " doesn't exist");
		} else {
			cf.setName(name);
			cf.setDescription(description);
			cf.setRef(ref);
			if (showPropChain != null) {
				cf.asCustomFormGraph().setShowPropertyChain(showPropChain);
			}
			cf.saveXML();
		}
	}
	
	/**
	 * Removes a CustomForm from the form collection and its file from file-system
	 * @param customForm
	 * @param deleteEmptyColl if true deletes FormCollection that are left empty after the deletion
	 * @throws CustomFormException 
	 */
	public void deleteCustomForm(String customFormId, boolean deleteEmptyColl) throws CustomFormException {
		if (this.getCustomFormById(customFormId) == null) {
			throw new CustomFormException("CustomForm with ID " + customFormId + " doesn't exist");
		} else {
			formsMap.remove(customFormId); // remove the custom form from the map
			// delete the CF file
			File[] cfFiles = getFormsFolder().listFiles();
			for (File f : cfFiles) {// search for the custom form file with the given id/name
				if (f.getName().equals(customFormId + ".xml")) {
					f.delete();
					break;
				}
			}
			// remove the entry from the FormCollection(s) that use it
			Collection<FormCollection> formCollections = this.getAllFormCollections();
			for (FormCollection formColl : formCollections) {
				if (formColl.containsForm(customFormId)) {
					formColl.removeForm(customFormId);
					// if deleteEmptyColl is true and collection is left empty, delete it
					if (deleteEmptyColl && formColl.getForms().isEmpty()) {
						this.deleteFormCollection(formColl.getId());
					} else { // otherwise save it
						formColl.saveXML();
					}
				}
			}
		}
	}
	
	public void addFormToCollection(FormCollection formColl, CustomForm customForm) {
		formColl.addForm(customForm);
		formColl.saveXML();
	}
	
	public void removeFormFromCollection(FormCollection formColl, CustomForm customForm) {
		formColl.removeForm(customForm.getId());
		formColl.saveXML();
	}
	
	
	/* #############################################
	 * ################# Utility ###################
	 * ############################################*/
	
	/**
	 * Returns the customForms folder, located under SemanticTurkeyData/ folder
	 * @return
	 */
	protected static File getCustomFormsFolder(){
		return new File(Resources.getSemTurkeyDataDir() + File.separator + CUSTOM_FORMS_FOLDER_NAME);
	}
	
	/**
	 * Returns the formCollections folder, located under SemanticTurkeyData/customForms/ folder
	 * @return
	 */
	protected static File getFormCollectionsFolder(){
		return new File(getCustomFormsFolder() + File.separator + FORM_COLLECTIONS_FOLDER_NAME);
	}
	
	/**
	 * Returns the forms folder, located under SemanticTurkeyData/customForms/ folder
	 * @return
	 */
	protected static File getFormsFolder(){
		return new File(getCustomFormsFolder() + File.separator + FORMS_FOLDER_NAME);
	}
	
	/**
	 * Creates the following hierarchy in SemanticTurkeyData folder:
	 * <ul>
	 *  <li>SemanticTurkeyData</li>
	 *   <ul>
	 *    <li>customForms</li>
	 *     <ul>
	 *      <li>customFormConfig.xml</li>
	 *      <li>forms/</li>
	 *      <li>formCollections</li>
	 *     </ul>
	 *   </ul>
	 * </ul>
	 * Where customFormConfig.xml is a sample config file.
	 * This is invoked only at the first start of SemanticTurkey server if the hierarchy is not ready
	 */
	private void initializeCFBasicHierarchy(){
		File cfFolder = getCustomFormsFolder();
		File formsFolder = getFormsFolder();
		File formCollFolder = getFormCollectionsFolder();
		cfFolder.mkdir();
		formsFolder.mkdir();
		formCollFolder.mkdir();
		cfConfig.createBasicCustomFormsConfig();
	}

}
