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
	 * @throws CustomFormInitializationException 
	 */
	public CustomFormsConfig(Collection<FormCollection> projFormCollections, Collection<FormCollection> sysFormCollections, Project<?> project) throws CustomFormInitializationException {
		this.project = project;
		mappings = CustomFormLoader.loadProjectFormsMapping(project, projFormCollections, sysFormCollections);
	}
	
	public CustomFormsConfig(Collection<FormCollection> formCollections) throws CustomFormInitializationException {
		mappings = CustomFormLoader.loadSysyemFormsMapping(formCollections);
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
	public void saveXML(){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element cfConfElement = doc.createElement("customFormConfig");
			doc.appendChild(cfConfElement);
			
			for (FormsMapping m : mappings){
				Element mappingElement = doc.createElement("formsMapping");
				cfConfElement.appendChild(mappingElement);
				mappingElement.setAttribute("resource", m.getResource());
				mappingElement.setAttribute("formCollection", m.getFormCollection().getId());
				mappingElement.setAttribute("replace", m.getReplace()+"");
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
			StreamResult result = new StreamResult(new File(CustomFormManager.getCustomFormsFolder(project), CUSTOM_FORMS_CONFIG_FILENAME).getPath());
			transformer.transform(source, result);
		} catch (TransformerException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a basic customFormsConfig.xml containing just some instructions and examples
	 */
//	public void createBasicCustomFormsConfig() {
//		cfConfXmlHelper.createBasicCustomFormsConfig();
//	}
	
	
	
//	private class CustomFormsConfigXMLHelper {
//		
//		private static final String CUSTOM_FORM_CONF_TAG = "customFormConfig";
//		private static final String FORMS_MAPPING_TAG = "formsMapping";
//		private static final String RESOURCE_ATTR_TAG = "resource";
//		private static final String REPLACE_ATTR_TAG = "replace";
//		private static final String FORM_COLL_ATTR_TAG = "formCollection";
//		
//		private File cfConfigFile;
//		private DocumentBuilder docBuilder;
//		
//		/**
//		 * Initialize the reader for the custom forms configuration file.
//		 * There is no check for the existence of the file since {@link CustomFormManager} calls 
//		 * {@link CustomFormsConfig#createBasicCustomFormsConfig()} at startup if it doesn't exist.
//		 */
//		public CustomFormsConfigXMLHelper(){
//			try {
//				cfConfigFile = new File(CustomFormManager.getCustomFormsFolder(projectName) + File.separator + CUSTOM_FORMS_CONFIG_FILENAME);
//				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//				docBuilder = dbFactory.newDocumentBuilder();
//			} catch (ParserConfigurationException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		/**
//		 * Loads the FormMapping pointed by the CustomFormConfig file
//		 * @param formCollectionMap
//		 * @return
//		 * @throws CustomFormInitializationException
//		 */
//		public Collection<FormsMapping> getFormMappings(Map<String, FormCollection> formCollectionMap) throws CustomFormInitializationException {
//			try {
//				Collection<FormsMapping> mappings = new ArrayList<FormsMapping>();
//				if (cfConfigFile.exists()) { //parse the config file and retrieve the mappings only if the xml congif file exists
//					Document doc = docBuilder.parse(cfConfigFile);
//					doc.getDocumentElement().normalize();
//					NodeList mappingList = doc.getElementsByTagName(FORMS_MAPPING_TAG);
//					for (int i=0; i<mappingList.getLength(); i++){
//						Node mappingNode = mappingList.item(i);
//						if (mappingNode.getNodeType() == Node.ELEMENT_NODE) {
//							Element mappingElement = (Element) mappingNode;
//							String mapResource = mappingElement.getAttribute(RESOURCE_ATTR_TAG);
//							String formCollId = mappingElement.getAttribute(FORM_COLL_ATTR_TAG);
//							boolean replace = Boolean.parseBoolean(mappingElement.getAttribute(REPLACE_ATTR_TAG));
//							FormCollection formColl = formCollectionMap.get(formCollId);
//							if (formColl != null){
//								mappings.add(new FormsMapping(mapResource, formColl, replace));
//							} else {
//								System.out.println("CustomFormConfig initialization: Warning, a Forms mapping in CustomFormConfig.xml "
//										+ "points to a not existing FormCollection " + formCollId);
//							}
//						}
//					}
//				}
//				return mappings;
//			} catch (IOException | SAXException e) {
//				throw new CustomFormInitializationException(
//						"Cannot initialize configuration for CustomForms. Error(s) in " + cfConfigFile.getPath());
//			}
//		}
//		
//		/**
//		 * Serialize the CustomFormsConfig on a xml file.
//		 */
//		public void saveXML(){
//			try {
//				Document doc = docBuilder.newDocument();
//				
//				Element cfConfElement = doc.createElement(CUSTOM_FORM_CONF_TAG);
//				doc.appendChild(cfConfElement);
//				
//				for (FormsMapping m : mappings){
//					Element mappingElement = doc.createElement(FORMS_MAPPING_TAG);
//					cfConfElement.appendChild(mappingElement);
//					mappingElement.setAttribute(RESOURCE_ATTR_TAG, m.getResource());
//					mappingElement.setAttribute(FORM_COLL_ATTR_TAG, m.getFormCollection().getId());
//					mappingElement.setAttribute(REPLACE_ATTR_TAG, m.getReplace()+"");
//				}
//
//				// write the content into xml file
//				TransformerFactory transformerFactory = TransformerFactory.newInstance();
//				Transformer transformer = transformerFactory.newTransformer();
//				Properties outputProps = new Properties();
//				outputProps.setProperty("encoding", "UTF-8");
//				outputProps.setProperty("indent", "yes");
//				outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//				transformer.setOutputProperties(outputProps);
//				DOMSource source = new DOMSource(doc);
//				StreamResult result = new StreamResult(new File(CustomFormManager.getCustomFormsFolder(projectName), CUSTOM_FORMS_CONFIG_FILENAME).getPath());
//				transformer.transform(source, result);
//			} catch (TransformerException e) {
//				e.printStackTrace();
//			}
//		}
//	}

}
