package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A collection of {@link CustomForm}
 * @author Tiziano
 *
 */
public class FormCollection {
	
	public static String PREFIX = "it.uniroma2.art.semanticturkey.customform.collection.";

	private String id;
	private Collection<CustomForm> forms;
	private CustomFormLevel level;
	
	public FormCollection(String id, Collection<CustomForm> forms){
		this.id = id;
		this.forms = forms;
		this.level = CustomFormLevel.project;
	}
	
	public FormCollection(String id){
		this.id = id;
		this.forms = new ArrayList<CustomForm>();
		this.level = CustomFormLevel.project;
	}
	
	/**
	 * Returns the ID of the CustomForm.
	 * @return
	 */
	public String getId(){
		return id;
	}
	
	/**
	 * Returns the {@link CustomForm}s contained in this collection
	 * @return
	 */
	public Collection<CustomForm> getForms(){
		return forms;
	}
	
	/**
	 * Returns the {@link CustomForm}s of type graph contained in this collection
	 * @return
	 */
	public Collection<CustomFormGraph> getGraphForms() {
		ArrayList<CustomFormGraph> cfColl = new ArrayList<>();
		for (CustomForm cf : forms) {
			if (cf.isTypeGraph()) {
				cfColl.add(cf.asCustomFormGraph());
			}
		}
		return cfColl;
	}
	
	/**
	 * Returns the {@link CustomForm} with the given ID. Null if it doesn't exists in this collection
	 * @param formId
	 * @return
	 */
	public CustomForm getCustomForm(String formId){
		for (CustomForm f : forms){
			if (f.getId().equals(formId)){
				return f;
			}
		}
		return null;
	}
	
	/**
	 * Adds a {@link CustomForm} to the current collection
	 * @param form
	 * @return true if the form is added, false if it is already present in the current collection 
	 */
	public boolean addForm(CustomForm form){
		for (CustomForm f : forms){
			if (f.getId().equals(form.getId()))
				return false;
		}
		forms.add(form);
		return true;
	}
	
	/**
	 * Removes the {@link CustomForm} with the given id from this collection
	 * @param formId
	 */
	public void removeForm(String formId){
		for (CustomForm f : forms){
			if (f.getId().equals(formId)){
				forms.remove(f);
				return;
			}
		}
	}
	
	/**
	 * Removes the given {@link CustomForm} from this collection
	 * @param formId
	 */
	public void removeForm(CustomForm customForm){
		removeForm(customForm.getId());
	}
	
	/**
	 * Returns true if the collection contains a {@link CustomForm} with the given ID
	 * @param formId
	 * @return
	 */
	public boolean containsForm(String formId){
		for (CustomForm f : forms){
			if (f.getId().equals(formId)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the collection contains a {@link CustomForm} with the given ID
	 * @param formId
	 * @return
	 */
	public boolean containsForm(CustomForm customForm){
		return containsForm(customForm.getId());
	}
	
	/**
	 * Returns all the id of the forms contained in the collection
	 * @return
	 */
	public Collection<String> getFormsId(){
		Collection<String> ids = new ArrayList<String>();
		for (CustomForm f : forms){
			ids.add(f.getId());
		}
		return ids;
	}
	
	public CustomFormLevel getLevel() {
		return level;
	}
	
	public void setLevel(CustomFormLevel level) {
		this.level = level;
	}
	
	/**
	 * Serialize the {@link FormCollection} on a xml file.
	 * @throws ParserConfigurationException 
	 */
	public void save(File file){
		CustomFormXMLHelper.serializeFormCollection(this, file);
	}
	
	

}
