package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import it.uniroma2.art.semanticturkey.project.Project;

public class CustomFormXMLHelper {
	
	private static final String CUSTOM_FORMS_CONFIG_FILENAME = "customFormConfig.xml";
	
	//prefix of system FormCollection's ids referenced in project config file
	//and of system CustomForm's ids referenced in project form collection file
	private static final String SYSTEM_LEVEL_PREFIX = "sys://";  
	
	private static final String CUSTOM_FORM_ROOT_TAG = "customForm";
	private static final String CUSTOM_FORM_ID_ATTR = "id";
	private static final String CUSTOM_FORM_NAME_ATTR = "name";
	private static final String CUSTOM_FORM_TYPE_ATTR = "type";
	private static final String CUSTOM_FORM_DESCRIPTION_TAG = "description";
	private static final String CUSTOM_FORM_REF_TAG = "ref";
	private static final String CUSTOM_FORM_SHOW_PROP_CHAIN_ATTR = "showPropertyChain";
	
	private static final String FORM_COLLECTION_ROOT_TAG = "formCollection";
	private static final String FORM_COLLECTION_ID_ATTR = "id";
	private static final String FORM_COLLECTION_FORM_TAG = "form";
	private static final String FORM_COLLECTION_FORM_ID_ATTR = "id";
	
	private static final String CONFIG_ROOT_TAG = "customFormConfig";
	private static final String CONFIG_MAPPING_TAG = "formsMapping";
	private static final String CONFIG_MAPPING_COLLECTION_ATTR = "formCollection";
	private static final String CONFIG_MAPPING_REPLACE_ATTR = "replace";
	private static final String CONFIG_MAPPING_RESOURCE_ATTR = "resource";
	
	private static Logger logger = LoggerFactory.getLogger(CustomFormXMLHelper.class);
	
	/* =========================================
	 * ============== XML Loaders ==============
	 * ========================================= */
	
	/**
	 * Loads the CustomForm files stored at system level
	 * @return
	 */
	public static Collection<CustomForm> loadSystemCustomForms() {
		File formsFolder = CustomFormManager.getFormsFolder(null);
		logger.debug("Loading CustomForms.");
		Collection<CustomForm> customForms = new ArrayList<>();
		File[] formFiles = formsFolder.listFiles();
		for (File f : formFiles) {
			if (f.getName().startsWith(CustomForm.PREFIX)) {
				logger.debug("Loading CustomForm file " + f.getPath());
				try {
					CustomForm cf = parseAndCreateCustomForm(f);
					cf.setLevel(CustomFormLevel.system);
					//check existence of CustomForm with the same ID at system level
					if (retrieveCustomForm(customForms, cf.getId(), CustomFormLevel.system) != null) {
						System.out.println("A CustomForm with ID " + cf.getId() + " already exists at system level. "
								+ ". CustomForm from file " + f.getPath() + " will be ignored");
					} else {
						customForms.add(cf);
					}
				} catch (CustomFormParseException e) {
					System.out.println("Failed to initialize CustomForm from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored. " + e.getMessage());
				}
			}
		}
		return customForms;
	}
	
	/**
	 * Loads the FormCollection files stored at system level
	 * @return
	 */
	public static Collection<FormCollection> loadSystemFormCollections(Collection<CustomForm> systemCustomForms) {
		// initialize FormCollection list (load files from formCollections/ folder)
		File formCollFolder = CustomFormManager.getFormCollectionsFolder(null);
		logger.debug("Loading FormCollections.");
		Collection<FormCollection> formCollections = new ArrayList<>();
		File[] formCollFiles = formCollFolder.listFiles();
		for (File f : formCollFiles) {
			if (f.getName().startsWith(FormCollection.PREFIX)) {
				logger.debug("Loading FormCollection file " + f.getPath());
				try {
					FormCollection fc = CustomFormXMLHelper.parseAndCreateFormCollection(f, systemCustomForms, CustomFormLevel.system);
					fc.setLevel(CustomFormLevel.system);
					//check existence of FormCollection with the same ID at system level
					if (retrieveFormCollection(formCollections, fc.getId(), CustomFormLevel.system) != null) {
						System.out.println("A FormCollection with ID " + fc.getId() + " already exists at system level. "
								+ ". CustomForm from file " + f.getPath() + " will be ignored");
					} else {
						formCollections.add(fc);
					}
				} catch (CustomFormParseException e) {
					System.out.println("Failed to initialize FormCollection from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored.");
					e.printStackTrace();
				}
			}
		}
		return formCollections;
	}
	
	public static Collection<FormsMapping> loadSysyemFormsMapping(Collection<FormCollection> systemFormCollections) throws CustomFormParseException {
		File cfConfigFile = new File(CustomFormManager.getCustomFormsFolder(null), CUSTOM_FORMS_CONFIG_FILENAME);
		return parseAndCreateFormsMappingCollection(cfConfigFile, systemFormCollections, CustomFormLevel.system);
	}
	
	/**
	 * Loads the CustomForm files stored in the given project
	 * @param project
	 * @param systemCustomForms
	 * @return
	 */
	public static Collection<CustomForm> loadProjectCustomForms(Project project, Collection<CustomForm> systemCustomForms) {
		File formsFolder = CustomFormManager.getFormsFolder(project);
		logger.debug("Loading CustomForms.");
		Collection<CustomForm> customForms = new ArrayList<>();
		File[] formFiles = formsFolder.listFiles();
		for (File f : formFiles) {
			if (f.getName().startsWith(CustomForm.PREFIX)) {
				logger.debug("Loading CustomForm file " + f.getPath());
				try {
					CustomForm cf = parseAndCreateCustomForm(f);
					cf.setLevel(CustomFormLevel.project);
					//check existence of CustomForm with the same ID in the same project
					if (retrieveCustomForm(customForms, cf.getId(), CustomFormLevel.project) != null) {
						System.out.println("A CustomForm with ID " + cf.getId() + " already exists in project "
								+ project.getName() + ". CustomForm from file " + f.getPath() + " will be ignored");
					} else {
						customForms.add(cf);
					}
				} catch (CustomFormParseException e) {
					System.out.println("Failed to initialize CustomForm from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored. " + e.getMessage());
				}
			}
		}
		return customForms;
	}
	
	/**
	 * Loads the FormCollection files stored in the given project
	 * @param project
	 * @param projectCustomForms
	 * @return
	 */
	public static Collection<FormCollection> loadProjectFormCollections(Project project,
			Collection<CustomForm> projectCustomForms, Collection<CustomForm> systemCustomForms) {
		//Custom Form at project and system level (used to check if a FC points to a not existing CF) 
		Collection<CustomForm> mergedCustomForms = new ArrayList<>(); 
		mergedCustomForms.addAll(projectCustomForms);
		mergedCustomForms.addAll(systemCustomForms);
		// initialize FormCollection list (load files from formCollections/ folder)
		File formCollFolder = CustomFormManager.getFormCollectionsFolder(project);
		logger.debug("Loading FormCollections.");
		Collection<FormCollection> formCollections = new ArrayList<>();
		File[] formCollFiles = formCollFolder.listFiles();
		for (File f : formCollFiles) {
			if (f.getName().startsWith(FormCollection.PREFIX)) {
				logger.debug("Loading FormCollection file " + f.getPath());
				try {
					FormCollection fc = parseAndCreateFormCollection(f, mergedCustomForms, CustomFormLevel.project);
					fc.setLevel(CustomFormLevel.project);
					//check existence of FormCollection with the same ID in the same project
					if (retrieveFormCollection(formCollections, fc.getId(), CustomFormLevel.project) != null) {
						System.out.println("A FormCollection with ID " + fc.getId() + " already exists in project "
								+ project.getName() + ". FormCollection from file " + f.getPath() + " will be ignored");
					} else {
						formCollections.add(fc);
					}
				} catch (CustomFormParseException e) {
					System.out.println("Failed to initialize FormCollection from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored.");
					e.printStackTrace();
				}
			}
		}
		return formCollections;
	}
	
	public static Collection<FormsMapping> loadProjectFormsMapping(Project project,
			Collection<FormCollection> projectFormCollections, Collection<FormCollection> systemFormCollections) throws CustomFormParseException {
		File cfConfigFile = new File(CustomFormManager.getCustomFormsFolder(project), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME);
		//FormCollections at project and system level (used to check if a mapping points to a not existing FC) 
		Collection<FormCollection> mergedFormCollections = new ArrayList<>(); 
		mergedFormCollections.addAll(projectFormCollections);
		mergedFormCollections.addAll(systemFormCollections);
		return parseAndCreateFormsMappingCollection(cfConfigFile, mergedFormCollections, CustomFormLevel.project);
	}
	
	/* ==================================
	 * ===== Loaders of single file =====
	 * ================================== */
	
	/**
	 * Given a {@link FormCollection} file loads its content
	 * @param fcFile
	 * @param customForms
	 * @return
	 * @throws CustomFormParseException 
	 */
	public static FormCollection parseAndCreateFormCollection(File fcFile, Collection<CustomForm> customForms,
			CustomFormLevel level) throws CustomFormParseException {
		try {
			String id = "";
			ArrayList<CustomForm> forms = new ArrayList<>();
	
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fcFile);
			doc.getDocumentElement().normalize();
			Element customFormElement = doc.getDocumentElement();
			
			if (!customFormElement.getTagName().equals(FORM_COLLECTION_ROOT_TAG)) {
				throw new CustomFormParseException("The root element of FormCollection file " + fcFile.getAbsolutePath() + " is not " + FORM_COLLECTION_ROOT_TAG);
			}
			
			//ID
			id = customFormElement.getAttribute(FORM_COLLECTION_ID_ATTR);
			if (id == null) {
				throw new CustomFormParseException(FORM_COLLECTION_ID_ATTR + " attribute missing in " + fcFile.getAbsolutePath());
			}
			id = id.trim();
	
			//Forms
			Collection<String> formIdsList = new ArrayList<String>();
			NodeList formNodeList = customFormElement.getElementsByTagName(FORM_COLLECTION_FORM_TAG);
			for (int i = 0; i < formNodeList.getLength(); i++) {
				Node formNode = formNodeList.item(i);
				if (formNode.getNodeType() == Node.ELEMENT_NODE) {
					Element formElement = (Element) formNode;
					String formId = formElement.getAttribute(FORM_COLLECTION_FORM_ID_ATTR);
					if (formId == null) {
						throw new CustomFormParseException(FORM_COLLECTION_FORM_ID_ATTR + " attribute missing in " 
								+ FORM_COLLECTION_FORM_TAG + " element in " + fcFile.getAbsolutePath());
					}
					formIdsList.add(formId);
				}
			}
			
			for (String formId : formIdsList){
				CustomForm cf;
				//if the formCollection file is at project level, check the prefix of the referenced customForm id
				//(formCollection at project level could reference project and system level customForm)
				if (level.equals(CustomFormLevel.project)) {
					//only in project formCollection file there is distinction between system and project customForm 
					if (formId.startsWith(SYSTEM_LEVEL_PREFIX)) {
						//looks if the referenced customForm exists at system level
						cf = retrieveCustomForm(customForms, formId.replace(SYSTEM_LEVEL_PREFIX, ""), CustomFormLevel.system);
					} else {
						//looks if the referenced customForm exists at project level
						cf = retrieveCustomForm(customForms, formId, CustomFormLevel.project);
					}
				} else {
					//if the formCollection file is at system level check if the referenced customForm exists at system level
					cf = retrieveCustomForm(customForms, formId, CustomFormLevel.system);
				}
				
				if (cf != null){
					forms.add(cf);
				} else {
					System.out.println("The FormCollection '" +  fcFile.getAbsolutePath() + "' points to a not existing"
							+ " CustomForm (ID: '" + formId + "'). The CustomForm has not been added to the FormCollection");
				}
			}
			
			return new FormCollection(id, forms);
		} catch (SAXException | ParserConfigurationException | IOException e) {
			throw new CustomFormParseException(e);
		}
	}
	
	/**
	 * Given a {@link CustomForm} file loads its content
	 * @param cfFile
	 * @return
	 * @throws CustomFormParseException 
	 */
	public static CustomForm parseAndCreateCustomForm(File cfFile) throws CustomFormParseException {
		try {
			String id = "";
			String name = "";
			String description = "";
			String ref = "";
			String type = "";
			List<IRI> showPropChain = new ArrayList<>();
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(cfFile);
			doc.getDocumentElement().normalize();
				
			Element customFormElement = doc.getDocumentElement();
			if (!customFormElement.getTagName().equals(CUSTOM_FORM_ROOT_TAG)) {
				throw new CustomFormParseException("The root element of CustomForm file " + cfFile.getAbsolutePath() + " is not " + CUSTOM_FORM_ROOT_TAG);
			}
			//ID
			id = customFormElement.getAttribute(CUSTOM_FORM_ID_ATTR);
			if (id == null) {
				throw new CustomFormParseException(CUSTOM_FORM_ID_ATTR + " attribute missing in " + cfFile.getAbsolutePath());
			}
			id = id.trim();
			
			//Name
			name = customFormElement.getAttribute(CUSTOM_FORM_NAME_ATTR);
			if (name == null) {
				throw new CustomFormParseException(CUSTOM_FORM_NAME_ATTR + " attribute missing in " + cfFile.getAbsolutePath());
			}
			name = name.trim();
	
			//description
			Node descriptionNode = doc.getElementsByTagName(CUSTOM_FORM_DESCRIPTION_TAG).item(0);
			if (descriptionNode == null) {
				throw new CustomFormParseException(CUSTOM_FORM_DESCRIPTION_TAG + " element is missing in " + cfFile.getAbsolutePath());
			}
			if (descriptionNode.getNodeType() == Node.ELEMENT_NODE) {
				Element descriptionElement = (Element) descriptionNode;
				description = descriptionElement.getTextContent();
			}
			description = description.trim();
			
			//type
			type = customFormElement.getAttribute(CUSTOM_FORM_TYPE_ATTR);
			if (type == null) {
				throw new CustomFormParseException(CUSTOM_FORM_TYPE_ATTR + " attribute missing in " + cfFile.getAbsolutePath());
			}
			type = type.trim();
	
			//ref
			Node refNode = doc.getElementsByTagName(CUSTOM_FORM_REF_TAG).item(0);
			if (refNode == null) {
				throw new CustomFormParseException(CUSTOM_FORM_REF_TAG + " element is missing in " + cfFile.getAbsolutePath());
			}
			if (refNode.getNodeType() == Node.ELEMENT_NODE) {
				Element refElement = (Element) refNode;
				ref = refElement.getTextContent();
				ref = ref.trim();
				
				if (type.equals(CustomForm.Types.graph.toString())){
					String showPropChainString = refElement.getAttribute(CUSTOM_FORM_SHOW_PROP_CHAIN_ATTR);
					//deserialize from string to List<IRI>
					if (showPropChainString != null && !showPropChainString.isEmpty()) {
						String[] splittedChain = showPropChainString.split(",");
						SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();
						for (int i = 0; i < splittedChain.length; i++) {
							showPropChain.add(valueFactory.createIRI(splittedChain[i]));
						}
					}
				}
			}
			
			if (type.equals(CustomForm.Types.graph.toString())){
				CustomFormGraph cre = new CustomFormGraph(id, name, description, ref, showPropChain);
				return cre;
			} else if (type.equals(CustomForm.Types.node.toString())){
				return new CustomFormNode(id, name, description, ref);
			} else {
				throw new CustomFormParseException("Invalid type '" + type + "' in CustomForm file '" + cfFile.getName() + "'");
			}
		} catch (SAXException | ParserConfigurationException | IOException e) {
			throw new CustomFormParseException(e);
		}
		
	}
	
	/**
	 * 
	 * @param cfConfigFile
	 * @param existingFormColls Existing FormCollection, used to check the existence of FormCollection referenced in the 
	 * configuration file.
	 * @return
	 * @throws CustomFormParseException 
	 */
	public static Collection<FormsMapping> parseAndCreateFormsMappingCollection(File cfConfigFile, 
			Collection<FormCollection> existingFormColls, CustomFormLevel level) throws CustomFormParseException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(cfConfigFile);
			doc.getDocumentElement().normalize();
			
			Element configElement = doc.getDocumentElement();
			if (!configElement.getTagName().equals(CONFIG_ROOT_TAG)) {
				throw new CustomFormParseException("The root element of CustomFormsConfig file " + cfConfigFile.getAbsolutePath()	
					+ " is not " + CONFIG_ROOT_TAG);
			}
			
			Collection<FormsMapping> mappings = new ArrayList<FormsMapping>();
			NodeList mappingList = doc.getElementsByTagName(CONFIG_MAPPING_TAG);
			for (int i = 0; i < mappingList.getLength(); i++) {
				Node mappingNode = mappingList.item(i);
				if (mappingNode.getNodeType() == Node.ELEMENT_NODE) {
					Element mappingElement = (Element) mappingNode;
					String mapResource = mappingElement.getAttribute(CONFIG_MAPPING_RESOURCE_ATTR);
					boolean replace = Boolean.parseBoolean(mappingElement.getAttribute(CONFIG_MAPPING_REPLACE_ATTR));
					String formCollId = mappingElement.getAttribute(CONFIG_MAPPING_COLLECTION_ATTR);
					//if the config file is at project level, check the prefix of the formCollection id
					//(formsMapping at project level could reference project and system level formCollection)
					FormCollection formColl;
					if (level.equals(CustomFormLevel.project)) {
						//only in project configuration file there is distinction between system and project FormCollection 
						if (formCollId.startsWith(SYSTEM_LEVEL_PREFIX)) {
							//looks if the referenced formCollection exists at system level
							formColl = retrieveFormCollection(existingFormColls, formCollId.replace(SYSTEM_LEVEL_PREFIX, ""), CustomFormLevel.system);
						} else {
							//looks if the referenced formCollection exists at project level
							formColl = retrieveFormCollection(existingFormColls, formCollId, CustomFormLevel.project);
						}
					} else { //if the config file is at system level check if the referenced formColl exists at system level
						formColl = retrieveFormCollection(existingFormColls, formCollId, CustomFormLevel.system);
					}
					if (formColl != null) {
						mappings.add(new FormsMapping(mapResource, formColl, replace));
					} else {
						System.out.println("CustomFormConfig initialization: Warning, a Forms mapping in "
								+ cfConfigFile.getPath() + " has a reference to a not existing FormCollection " + formCollId);
					}
				}
			}
			return mappings;
		} catch (SAXException | ParserConfigurationException | IOException e) {
			throw new CustomFormParseException(e);
		}
	}
	
	/**
	 * Retrieve the CustomForm with the given ID from a CustomForm list. If level parameter is not null,
	 * search only the CustomForm with the given level
	 * @param coll
	 * @param cfId
	 * @param level
	 * @return
	 */
	private static CustomForm retrieveCustomForm(Collection<CustomForm> coll, String cfId, CustomFormLevel level) {
		for (CustomForm c : coll) {
			if (c.getId().equals(cfId) && c.getLevel().equals(level)) {
				return c;
			}
		}
		return null;
	}
	
	/**
	 * Retrieve the FormCollection with the given ID from a FormCollection list at the given level
	 * @param coll
	 * @param fcId
	 * @param level
	 * @return
	 */
	private static FormCollection retrieveFormCollection(Collection<FormCollection> coll, String fcId, CustomFormLevel level) {
		for (FormCollection c : coll) {
			if (c.getId().equals(fcId) && c.getLevel().equals(level)) {
				return c;
			}
		}
		return null;
	}
	
	/* =========================================
	 * ============ XML Serializers ============
	 * ========================================= */
	
	public static void serializeCustomFormsConfig(CustomFormsConfig config, File file) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element cfConfElement = doc.createElement(CONFIG_ROOT_TAG);
			doc.appendChild(cfConfElement);
			
			for (FormsMapping m : config.getFormsMappings()){
				Element mappingElement = doc.createElement(CONFIG_MAPPING_TAG);
				cfConfElement.appendChild(mappingElement);
				mappingElement.setAttribute(CONFIG_MAPPING_RESOURCE_ATTR, m.getResource());
				String fcId = m.getFormCollection().getId();
				if (m.getFormCollection().getLevel().equals(CustomFormLevel.system)) {
					fcId = SYSTEM_LEVEL_PREFIX + fcId;
				}
				mappingElement.setAttribute(CONFIG_MAPPING_COLLECTION_ATTR, fcId);
				mappingElement.setAttribute(CONFIG_MAPPING_REPLACE_ATTR, m.getReplace()+"");
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
			StreamResult result = new StreamResult(new FileOutputStream(file));
			transformer.transform(source, result);
			result.getOutputStream().close();
		} catch (TransformerException | ParserConfigurationException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void serializeFormCollection(FormCollection formCollection, File file) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element collElement = doc.createElement(FORM_COLLECTION_ROOT_TAG);
			doc.appendChild(collElement);
			collElement.setAttribute(FORM_COLLECTION_ID_ATTR, formCollection.getId());
			
			for (CustomForm f : formCollection.getForms()){
				Element formElement = doc.createElement(FORM_COLLECTION_FORM_TAG);
				collElement.appendChild(formElement);
				String cfId = f.getId();
				if (f.getLevel().equals(CustomFormLevel.system)) {
					cfId = SYSTEM_LEVEL_PREFIX + cfId;
				}
				formElement.setAttribute(FORM_COLLECTION_FORM_ID_ATTR, cfId);
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
			StreamResult result = new StreamResult(new FileOutputStream(file));
			transformer.transform(source, result);
			result.getOutputStream().close();
		} catch (ParserConfigurationException | TransformerException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void serializeCustomForm(CustomForm customForm, File file) {
		if (customForm.isTypeGraph()) {
			serializeCustomFormGraph(customForm.asCustomFormGraph(), file);
		} else {
			serializeCustomFormNode(customForm.asCustomFormNode(), file);
		}
	}
	
	private static void serializeCustomFormNode(CustomFormNode customForm, File file) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element creElement = doc.createElement(CUSTOM_FORM_ROOT_TAG);
			doc.appendChild(creElement);
			creElement.setAttribute(CUSTOM_FORM_ID_ATTR, customForm.getId());
			creElement.setAttribute(CUSTOM_FORM_NAME_ATTR, customForm.getName());
			creElement.setAttribute(CUSTOM_FORM_TYPE_ATTR, customForm.getType());
			
			Element descrElement = doc.createElement(CUSTOM_FORM_DESCRIPTION_TAG);
			descrElement.setTextContent(customForm.getDescription());
			creElement.appendChild(descrElement);
			
			Element refElement = doc.createElement(CUSTOM_FORM_REF_TAG); 
			refElement.setTextContent(customForm.getRef());
			creElement.appendChild(refElement);
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			Properties outputProps = new Properties();
			outputProps.setProperty("encoding", "UTF-8");
			outputProps.setProperty("indent", "yes");
			outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperties(outputProps);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new FileOutputStream(file));
			transformer.transform(source, result);
			result.getOutputStream().close();
		} catch (ParserConfigurationException | TransformerException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void serializeCustomFormGraph(CustomFormGraph customForm, File file) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element creElement = doc.createElement("customForm");
			doc.appendChild(creElement);
			creElement.setAttribute(CUSTOM_FORM_ID_ATTR, customForm.getId());
			creElement.setAttribute(CUSTOM_FORM_NAME_ATTR, customForm.getName());
			creElement.setAttribute(CUSTOM_FORM_TYPE_ATTR, customForm.getType());
			
			Element descrElement = doc.createElement("description");
			descrElement.setTextContent(customForm.getDescription());
			creElement.appendChild(descrElement);
			
			Element refElement = doc.createElement("ref"); 
			CDATASection cdata = doc.createCDATASection(customForm.getRef());
			refElement.appendChild(cdata);
			
			String propChain = customForm.serializePropertyChain();
			if (!propChain.isEmpty()) {
				refElement.setAttribute("showPropertyChain", propChain);
			}
			creElement.appendChild(refElement);
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			Properties outputProps = new Properties();
			outputProps.setProperty("encoding", "UTF-8");
			outputProps.setProperty("indent", "yes");
			outputProps.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperties(outputProps);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new FileOutputStream(file));
			transformer.transform(source, result);
			result.getOutputStream().close();
		} catch (ParserConfigurationException | TransformerException | IOException e) {
			e.printStackTrace();
		}
	}

}
