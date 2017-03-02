package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Collection of {@link FormCollectionMapping}. This class represent a model in memory of <code>customFormsConfig.xml</code>
 * @author Tiziano
 *
 */
public class CustomFormsConfig {
	
	private static final String CUSTOM_FORMS_CONFIG_FILENAME = "customFormConfig.xml";
	private CustomFormsConfigXMLHelper cfConfXmlHelper;
	
	private List<FormCollectionMapping> mappings;
	
	private static Logger logger = LoggerFactory.getLogger(CustomFormsConfig.class);
	
	/**
	 * Initialize the CustomForm configuration tree, a structure that links resources with
	 * {@link FormCollection}s and in turn {@link FormCollection}s with {@link CustomForm}s.
	 * The initialization need a map of all the {@link FormCollection}s and {@link CustomForm}s already initialized
	 * and found respectively in the customForms/formCollections folder and customForms/forms folder.
	 * @param crMap
	 * @param creMap
	 * @throws CustomFormInitializationException 
	 */
	public CustomFormsConfig(FormCollectionList formCollections, CustomFormList customForms) throws CustomFormInitializationException {
		cfConfXmlHelper = new CustomFormsConfigXMLHelper();
		mappings = cfConfXmlHelper.getFormMappings(formCollections, customForms);
	}
	
	public List<FormCollectionMapping> getFormsMappings(){
		return mappings;
	}
	
	/**
	 * Given a resource returns the {@link FormCollection} associated to that resource. <code>null</code> if no
	 * {@link FormCollection} is specified for it.
	 * @param resourceUri
	 * @return
	 */
	public FormCollection getFormCollectionForResource(String resourceUri) {
		for (FormCollectionMapping m : mappings){
			if (m.getResource().equals(resourceUri)) {
				return m.getFormCollection();
			}
		}
		return null;
	}
	
	/**
	 * Adds a {@link FormCollectionMapping} to the configuration
	 * @param crConfEntry
	 */
	public void addFormsMapping(FormCollectionMapping formMapping){
		mappings.add(formMapping);
	}
	
	/**
	 * Remove a {@link FormCollectionMapping} from the configuration
	 * @param resourceUri
	 */
	public void removeMappingOfResource(String resourceUri){
		Iterator<FormCollectionMapping> it = mappings.iterator();
		while (it.hasNext()){
			FormCollectionMapping mapping = it.next();
			if (mapping.getResource().equals(resourceUri)){
				it.remove();
			}
		}
	}
	
	/**
	 * Remove resource-FormCollection pair with the given FormCollection ID
	 * @param formCollectionID
	 */
	public void removeMappingOfFormCollection(String formCollectionID){
		Iterator<FormCollectionMapping> it = mappings.iterator();
		while (it.hasNext()){
			FormCollectionMapping mapping = it.next();
			if (mapping.getFormCollection().getId().equals(formCollectionID)){
				it.remove();
			}
		}
	}
	
	/**
	 * Tells if the CustomForm should replace the "classic" form for the given resource.
	 * @param resourceUri
	 * @return
	 */
	public boolean getReplace(String resourceUri){
		for (FormCollectionMapping m : mappings){
			if (m.getResource().equals(resourceUri)) {
				return m.getReplace();
			}
		}
		return false;
	}
	
	/**
	 * Set the replace parameter for the FormCollection linked to the given resource 
	 * @param resourceUri
	 * @param replace
	 */
	public void setReplace(String resourceUri, boolean replace) {
		for (FormCollectionMapping m : mappings){
			if (m.getResource().equals(resourceUri)) {
				m.setReplace(replace);
				return;
			}
		}
	}
	
	/**
	 * Serialize the CustomFormsConfig on a xml file.
	 */
	public void saveXML(){
		cfConfXmlHelper.saveXML();
	}
	
	/**
	 * Creates a basic customFormsConfig.xml containing just some instructions and examples
	 */
	public void createBasicCustomFormsConfig() {
		cfConfXmlHelper.createBasicCustomFormsConfig();
	}
	
	
	
	private class CustomFormsConfigXMLHelper {
		
		private static final String CUSTOM_FORM_CONF_TAG = "customFormConfig";
		private static final String FORM_MAPPING_TAG = "formMapping";
		private static final String RESOURCE_ATTR_TAG = "resource";
		private static final String REPLACE_ATTR_TAG = "replace";
		private static final String FORM_COLL_ATTR_TAG = "formCollection";
		
		private File cfConfigFile;
		private DocumentBuilder docBuilder;
		
		/**
		 * Initialize the reader for the custom forms configuration file.
		 * There is no check for the existence of the file since {@link CustomFormManager} calls 
		 * {@link CustomFormsConfig#createBasicCustomFormsConfig()} at startup if it doesn't exist.
		 */
		public CustomFormsConfigXMLHelper(){
			try {
				cfConfigFile = new File(CustomFormManager.getCustomFormsFolder() + File.separator + CUSTOM_FORMS_CONFIG_FILENAME);
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				docBuilder = dbFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
		}
		
		public List<FormCollectionMapping> getFormMappings(FormCollectionList formCollections, CustomFormList customForms) throws CustomFormInitializationException {
			try {
				List<FormCollectionMapping> mappings = new ArrayList<FormCollectionMapping>();
				if (cfConfigFile.exists()) { //parse the config file and retrieve the mappings only if the xml congif file exists
					Document doc = docBuilder.parse(cfConfigFile);
					doc.getDocumentElement().normalize();
					NodeList mappingList = doc.getElementsByTagName(FORM_MAPPING_TAG);
					for (int i=0; i<mappingList.getLength(); i++){
						Node mappingNode = mappingList.item(i);
						if (mappingNode.getNodeType() == Node.ELEMENT_NODE) {
							Element mappingElement = (Element) mappingNode;
							String mapResource = mappingElement.getAttribute(RESOURCE_ATTR_TAG);
							String formCollId = mappingElement.getAttribute(FORM_COLL_ATTR_TAG);
							boolean replace = Boolean.parseBoolean(mappingElement.getAttribute(REPLACE_ATTR_TAG));
							FormCollection formColl = formCollections.get(formCollId);
							if (formColl != null){
								mappings.add(new FormCollectionMapping(mapResource, formColl, replace));
							} else {
								System.out.println("CustomFormConfig initialization: Warning, a Forms mapping in CustomFormConfig.xml,"
										+ "points to a not existing FormCollection " + formCollId);
							}
						}
					}
				}
				return mappings;
			} catch (IOException | SAXException e) {
				throw new CustomFormInitializationException(
						"Cannot initialize configuration for CustomForms. Error(s) in " + cfConfigFile.getPath());
			}
		}
		
		/**
		 * Creates a basic customFormsConfig.xml containing just some instructions and examples
		 */
		public void createBasicCustomFormsConfig() {
			try {
				Document doc = docBuilder.newDocument();

				Element crConfElement = doc.createElement(CUSTOM_FORM_CONF_TAG);
				doc.appendChild(crConfElement);

				Comment comment;
				String description = "\nThis element should contains a collection of " + FORM_MAPPING_TAG + " elements which maps "
						+ "resources (classes or properties identified by IRI) with the FormCollections (identified by an ID).\n"
						+ "Here it is an example of " + FORM_MAPPING_TAG + ":\n\n"
						+ "<" + FORM_MAPPING_TAG + " " + RESOURCE_ATTR_TAG + "=\"http://www.w3.org/2004/02/skos/core#definition\" "
						+ FORM_COLL_ATTR_TAG + "=\"" + FormCollection.PREFIX + "note\" "
						+ REPLACE_ATTR_TAG + "=\"false\" />\n\n"
						+ "The value of " + FORM_COLL_ATTR_TAG + " must be the name of the FormCollection file without extension.\n"
						+ "E.g. if " + FORM_COLL_ATTR_TAG + "=\"" + FormCollection.PREFIX + "foo\" "
						+ "the CR file must be names \"" + FormCollection.PREFIX + "foo.xml\".\n"
						+ "The attribute \"" + REPLACE_ATTR_TAG + "\" is boolean.\n";
				comment = doc.createComment(description);
				crConfElement.appendChild(comment);
				crConfElement.appendChild(comment);

				// write the content into xml file
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				Properties outputProps = new Properties();
				outputProps.setProperty("encoding", "UTF-8");
				outputProps.setProperty("indent", "yes");
				transformer.setOutputProperties(outputProps);
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(CustomFormManager.getCustomFormsFolder(), CUSTOM_FORMS_CONFIG_FILENAME).getPath());
				transformer.transform(source, result);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}
	
		/**
		 * Serialize the CustomFormsConfig on a xml file.
		 */
		public void saveXML(){
			try {
				Document doc = docBuilder.newDocument();
				
				Element cfConfElement = doc.createElement(CUSTOM_FORM_CONF_TAG);
				doc.appendChild(cfConfElement);
				
				for (FormCollectionMapping m : mappings){
					Element mappingElement = doc.createElement(FORM_MAPPING_TAG);
					cfConfElement.appendChild(mappingElement);
					mappingElement.setAttribute(RESOURCE_ATTR_TAG, m.getResource());
					mappingElement.setAttribute(FORM_COLL_ATTR_TAG, m.getFormCollection().getId());
					mappingElement.setAttribute(REPLACE_ATTR_TAG, m.getReplace()+"");
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
				StreamResult result = new StreamResult(new File(CustomFormManager.getCustomFormsFolder(), CUSTOM_FORMS_CONFIG_FILENAME).getPath());
				transformer.transform(source, result);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}
	}

}
