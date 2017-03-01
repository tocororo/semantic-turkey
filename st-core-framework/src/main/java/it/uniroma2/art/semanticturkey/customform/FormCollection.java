package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A collection of {@link CustomForm}
 * @author Tiziano
 *
 */
public class FormCollection {
	
	public static String PREFIX = "it.uniroma2.art.semanticturkey.forms.";

	private String id;
	private List<CustomForm> forms;
	
	FormCollection(String id, List<CustomForm> forms){
		this.id = id;
		this.forms = forms;
	}
	
	FormCollection(String id){
		this.id = id;
		this.forms = new ArrayList<CustomForm>();
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
	public List<CustomForm> getForms(){
		return forms;
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
	 * Returns all the id of the forms contained in the collection
	 * @return
	 */
	public List<String> getFormsId(){
		List<String> ids = new ArrayList<String>();
		for (CustomForm f : forms){
			ids.add(f.getId());
		}
		return ids;
	}
	
	/**
	 * Serialize the {@link FormCollection} on a xml file.
	 * @throws ParserConfigurationException 
	 */
	public void saveXML(){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element collElement = doc.createElement("formCollection");
			doc.appendChild(collElement);
			collElement.setAttribute("id", this.getId());
			
			for (CustomForm f : forms){
				Element formElement = doc.createElement("form");
				collElement.appendChild(formElement);
				formElement.setAttribute("id", f.getId());
			}
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			Properties outputProps = new Properties();
			outputProps.setProperty("encoding", "UTF-8");
			outputProps.setProperty("indent", "yes");
			outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperties(outputProps);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(CustomFormManager.getFormCollectionsFolder(), this.getId() + ".xml"));
			transformer.transform(source, result);
		} catch (ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}
	}
	
	

}
