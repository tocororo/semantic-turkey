package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.rdf4j.model.IRI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import it.uniroma2.art.semanticturkey.project.Project;

/**
 * Collection of {@link FormsMapping}. This class represent a model in memory of <code>customFormsConfig.xml</code>
 * @author Tiziano
 *
 */
public class CustomFormsConfig {
	
	protected static final String CUSTOM_FORMS_CONFIG_FILENAME = "customFormConfig.xml";
//	private CustomFormsConfigXMLHelper cfConfXmlHelper;
	
	private Project<?> project;
	private Collection<FormsMapping> mappings;
	
	/**
	 * Initialize the CustomForm configuration tree, a structure that links resources with
	 * {@link FormCollection}s and in turn {@link FormCollection}s with {@link CustomForm}s.
	 * The initialization need a map of all the {@link FormCollection}s and {@link CustomForm}s already initialized
	 * and found respectively in the customForms/formCollections folder and customForms/forms folder.
	 * @param crMap
	 * @param creMap
	 * @param project name of project 
	 * @throws CustomFormParseException 
	 */
	public CustomFormsConfig(Collection<FormCollection> projFormCollections, Collection<FormCollection> sysFormCollections, Project<?> project) throws CustomFormParseException {
		this.project = project;
		mappings = CustomFormXMLHelper.loadProjectFormsMapping(project, projFormCollections, sysFormCollections);
	}
	
	public CustomFormsConfig(Collection<FormCollection> formCollections) throws CustomFormParseException {
		mappings = CustomFormXMLHelper.loadSysyemFormsMapping(formCollections);
	}
	
	/**
	 * Returns the FormCollectionMapping of the given resource
	 * @param resource
	 * @return
	 */
	public FormsMapping getFormsMapping(IRI resource) {
		for (FormsMapping mapping : mappings) {
			if (mapping.getResource().equals(resource.stringValue())) {
				return mapping;
			}
		}
		return null;
	}
	
	public Collection<FormsMapping> getFormsMappings(){
		return mappings;
	}
	
	/**
	 * Given a resource returns the {@link FormCollection} associated to that resource. <code>null</code> if no
	 * {@link FormCollection} is specified for it.
	 * @param resource
	 * @return
	 */
	public FormCollection getFormCollectionForResource(IRI resource) {
		for (FormsMapping m : mappings){
			if (m.getResource().equals(resource.stringValue())) {
				return m.getFormCollection();
			}
		}
		return null;
	}
	
	/**
	 * Adds a {@link FormsMapping} to the configuration
	 * @param crConfEntry
	 */
	public void addFormsMapping(FormsMapping formMapping){
		mappings.add(formMapping);
	}
	
	/**
	 * Remove a {@link FormsMapping} from the configuration
	 * @param resource
	 */
	public void removeMappingOfResource(IRI resource){
		Iterator<FormsMapping> it = mappings.iterator();
		while (it.hasNext()){
			FormsMapping mapping = it.next();
			if (mapping.getResource().equals(resource.stringValue())){
				it.remove();
			}
		}
	}
	
	/**
	 * Remove resource-FormCollection pair with the given FormCollection ID
	 * @param formCollectionID
	 */
	public void removeMappingOfFormCollection(FormCollection formColl){
		Iterator<FormsMapping> it = mappings.iterator();
		while (it.hasNext()){
			FormsMapping mapping = it.next();
			if (mapping.getFormCollection().getId().equals(formColl.getId())){
				it.remove();
			}
		}
	}
	
	/**
	 * Tells if the CustomForm should replace the "classic" form for the given resource.
	 * @param resource
	 * @return
	 */
	public boolean getReplace(IRI resource){
		for (FormsMapping m : mappings){
			if (m.getResource().equals(resource.stringValue())) {
				return m.getReplace();
			}
		}
		return false;
	}
	
	/**
	 * Serialize the CustomFormsConfig on a xml file.
	 */
	public void save(){
		CustomFormXMLHelper.serializeCustomFormsConfig(this, new File(CustomFormManager.getCustomFormsFolder(project), CUSTOM_FORMS_CONFIG_FILENAME));
//		try {
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
//			Document doc = docBuilder.newDocument();
//			Element cfConfElement = doc.createElement("customFormConfig");
//			doc.appendChild(cfConfElement);
//			
//			for (FormsMapping m : mappings){
//				Element mappingElement = doc.createElement("formsMapping");
//				cfConfElement.appendChild(mappingElement);
//				mappingElement.setAttribute("resource", m.getResource());
//				mappingElement.setAttribute("formCollection", m.getFormCollection().getId());
//				mappingElement.setAttribute("replace", m.getReplace()+"");
//			}
//
//			// write the content into xml file
//			TransformerFactory transformerFactory = TransformerFactory.newInstance();
//			Transformer transformer = transformerFactory.newTransformer();
//			Properties outputProps = new Properties();
//			outputProps.setProperty("encoding", "UTF-8");
//			outputProps.setProperty("indent", "yes");
//			outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//			transformer.setOutputProperties(outputProps);
//			DOMSource source = new DOMSource(doc);
//			StreamResult result = new StreamResult(new File(CustomFormManager.getCustomFormsFolder(project), CUSTOM_FORMS_CONFIG_FILENAME).getPath());
//			transformer.transform(source, result);
//		} catch (TransformerException | ParserConfigurationException e) {
//			e.printStackTrace();
//		}
	}
	
}
