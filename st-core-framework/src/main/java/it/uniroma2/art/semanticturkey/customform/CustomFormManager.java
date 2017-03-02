package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import it.uniroma2.art.semanticturkey.resources.Resources;

@Component
public class CustomFormManager {
	
	private static final String CUSTOM_FORMS_FOLDER_NAME = "customForms";
	private static final String FORM_COLLECTIONS_FOLDER_NAME = "formCollections";
	private static final String FORMS_FOLDER_NAME = "forms";
	
	//TODO the followings will become map of <projectName, CustomFormsConfig/FormCollectionList/CustomFormList>
	//where projectName is SYSTEM at SYSTEM level
	//I will also provide method register/unregisterCustomFormStructure(projectName) that load/unload in memory the
	//structure of a project when it's open/closed
	private CustomFormsConfig cfConfig;
	private FormCollectionList formCollList;
	private CustomFormList customFormList;
	
	private static Logger logger = LoggerFactory.getLogger(CustomFormManager.class);
	
	@PostConstruct
	public void init() {
		try {
			formCollList = new FormCollectionList();
			customFormList = new CustomFormList();
			
			// check existing of customForms folders hierarchy
			File cfFolder = getCustomFormsFolder();
			if (!cfFolder.exists()) {
				cfConfig = new CustomFormsConfig(formCollList, customFormList);
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
							customFormList.add(cf);
						} catch (DuplicateIdException e) {
							System.out.println("CustomForm from file " + f.getName() + " has duplicated ID. "
								+ "It will be ignored.");
						} catch (CustomFormInitializationException e) {
							System.out.println("Failed to initialize CustomForm from file " + f.getName()
								+ ", it may contain some errors. It will be ignored. " + e.getMessage());
						} catch (ParserConfigurationException | SAXException | IOException e) {
							System.out.println("Failed to initialize FormCollection from file " + f.getName()
								+ ", it may contain some errors. It will be ignored.");
							e.printStackTrace();
						}
					}
				}
				// initialize FormCollection list (load files from formCollections/ folder) and store them in the
				// formColl map
				File[] formCollFiles = formCollFolder.listFiles();
				for (File f : formCollFiles) {
					if (f.getName().startsWith(FormCollection.PREFIX)) {
						try {
							FormCollection formColl = CustomFormLoader.loadFormCollection(f, customFormList);
							formCollList.add(formColl);
						} catch (DuplicateIdException e) {
							System.out.println("FormCollection from file " + f.getName() + " has a duplicated ID. "
								+ "It will be ignored.");
						} catch (ParserConfigurationException | SAXException | IOException e) {
							System.out.println("Failed to initialize FormCollection from file " + f.getName()
								+ ", it may contain some errors. It will be ignored.");
							e.printStackTrace();
						}
					}
				}
				cfConfig = new CustomFormsConfig(formCollList, customFormList);
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
		return formCollList.getAllFormCollections();
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
		return formCollList.get(formCollId);
	}
	
	// CUSTOM FORM
	
	/**
	 * Returns all the {@link CustomForm} available into the forms/ folder
	 * @return
	 */
	public Collection<CustomForm> getAllCustomForms() {
		return customFormList.getAllCustomForms();
	}
	
	/**
	 * Returns the {@link CustomForm} with the given ID
	 * @param cfId
	 * @return
	 */
	public CustomForm getCustomFormById(String cfId){
		return customFormList.get(cfId);
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
	
	/**
	 * Creates and adds a FormCollection. If a {@link FormCollection} with the same ID exists, a 
	 * {@link DuplicateIdException} is thrown
	 * @param id
	 * @return
	 * @throws DuplicateIdException 
	 */
	public FormCollection createFormCollection(String id) throws DuplicateIdException {
		FormCollection formColl = new FormCollection(id);
		formCollList.add(formColl); // and add it to the map
		formColl.saveXML(); // serialize it (only if add doesn't throw an exception)
		return formColl;
	}
	
	// CUSTOM FORM
	
	/**
	 * Creates and adds a CustomForm. If a {@link CustomForm} with the same ID exists, a 
	 * {@link DuplicateIdException} is thrown
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
		CustomForm form = null;
		if (type.equalsIgnoreCase(CustomForm.Types.node.toString())) {
			form = new CustomFormNode(id, name, description, ref);
		} else {
			form = new CustomFormGraph(id, name, description, ref, showPropChain);
		}
		customFormList.add(form); // and add it to the map
		form.saveXML(); // serialize it (only if add doesn't throw an exception)
		return form;
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
		formCollList.remove(formCollId); // remove formColl from map
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
			customFormList.remove(customFormId); // remove the custom form from the map
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
