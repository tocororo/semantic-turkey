package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import it.uniroma2.art.semanticturkey.project.Project;

public class CustomFormLoader {
	
	//TODO not use the map since values() return an unmodifiable collection. Test again clone collection
	
	private static Logger logger = LoggerFactory.getLogger(CustomFormLoader.class);
	
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
					CustomForm cf = loadCustomForm(f);
					cf.setLevel(CustomFormLevel.system);
					if (retrieveCustomForm(customForms, cf.getId()) != null) {
						System.out.println("A CustomForm with ID " + cf.getId() + " already exists at system level. "
								+ ". CustomForm from file " + f.getPath() + " will be ignored");
					} else {
						customForms.add(cf);
					}
				} catch (CustomFormInitializationException e) {
					System.out.println("Failed to initialize CustomForm from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored. " + e.getMessage());
				} catch (ParserConfigurationException | SAXException | IOException e) {
					System.out.println("Failed to initialize FormCollection from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored.");
					e.printStackTrace();
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
					FormCollection fc = CustomFormLoader.loadFormCollection(f, systemCustomForms);
					fc.setLevel(CustomFormLevel.system);
					if (retrieveFormCollection(formCollections, fc.getId()) != null) {
						System.out.println("A FormCollection with ID " + fc.getId() + " already exists at system level. "
								+ ". CustomForm from file " + f.getPath() + " will be ignored");
					} else {
						formCollections.add(fc);
					}
				} catch (ParserConfigurationException | SAXException | IOException e) {
					System.out.println("Failed to initialize FormCollection from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored.");
					e.printStackTrace();
				}
			}
		}
		return formCollections;
	}
	
	public static Collection<FormsMapping> loadSysyemFormsMapping(Collection<FormCollection> systemFormCollections) throws CustomFormInitializationException {
		File cfConfigFile = new File(CustomFormManager.getCustomFormsFolder(null), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME);
		try {
			CustomFormsConfigXMLReader cfcReader = new CustomFormsConfigXMLReader(cfConfigFile);
			return cfcReader.getFormMappings(systemFormCollections);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new CustomFormInitializationException(e);
		}
	}
	
	/**
	 * Loads the CustomForm files stored in the given project
	 * @param project
	 * @param systemCustomForms
	 * @return
	 */
	public static Collection<CustomForm> loadProjectCustomForms(Project<?> project, Collection<CustomForm> systemCustomForms) {
		File formsFolder = CustomFormManager.getFormsFolder(project);
		logger.debug("Loading CustomForms.");
		Collection<CustomForm> customForms = new ArrayList<>();
		File[] formFiles = formsFolder.listFiles();
		for (File f : formFiles) {
			if (f.getName().startsWith(CustomForm.PREFIX)) {
				logger.debug("Loading CustomForm file " + f.getPath());
				try {
					CustomForm cf = loadCustomForm(f);
					cf.setLevel(CustomFormLevel.project);
					if (retrieveCustomForm(customForms, cf.getId()) != null) {
						System.out.println("A CustomForm with ID " + cf.getId() + " already exists in project "
								+ project.getName() + ". CustomForm from file " + f.getPath() + " will be ignored");
					} else if (retrieveCustomForm(systemCustomForms, cf.getId()) != null) {
						System.out.println("A CustomForm with ID " + cf.getId() + " already exists at project level "
								+ project.getName() + ". CustomForm from file " + f.getPath() + " will be ignored");
					} else {
						customForms.add(cf);
					}
				} catch (CustomFormInitializationException e) {
					System.out.println("Failed to initialize CustomForm from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored. " + e.getMessage());
				} catch (ParserConfigurationException | SAXException | IOException e) {
					System.out.println("Failed to initialize FormCollection from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored.");
					e.printStackTrace();
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
	public static Collection<FormCollection> loadProjectFormCollections(Project<?> project,
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
					FormCollection fc = loadFormCollection(f, mergedCustomForms);
					fc.setLevel(CustomFormLevel.project);
					if (retrieveFormCollection(formCollections, fc.getId()) != null) {
						System.out.println("A FormCollection with ID " + fc.getId() + " already exists in project "
								+ project.getName() + ". FormCollection from file " + f.getPath() + " will be ignored");
					} else {
						formCollections.add(fc);
					}
				} catch (ParserConfigurationException | SAXException | IOException e) {
					System.out.println("Failed to initialize FormCollection from file " + f.getPath()
							+ ", it may contain some errors. It will be ignored.");
					e.printStackTrace();
				}
			}
		}
		return formCollections;
	}
	
	public static Collection<FormsMapping> loadProjectFormsMapping(Project<?> project,
			Collection<FormCollection> projectFormCollections, Collection<FormCollection> systemFormCollections) throws CustomFormInitializationException {
		File cfConfigFile = new File(CustomFormManager.getCustomFormsFolder(project), CustomFormsConfig.CUSTOM_FORMS_CONFIG_FILENAME);
		try {
			//FormCollections at project and system level (used to check if a mapping points to a not existing FC) 
			Collection<FormCollection> mergedFormCollections = new ArrayList<>(); 
			mergedFormCollections.addAll(projectFormCollections);
			mergedFormCollections.addAll(systemFormCollections);
			CustomFormsConfigXMLReader cfcReader = new CustomFormsConfigXMLReader(cfConfigFile);
			return cfcReader.getFormMappings(mergedFormCollections);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new CustomFormInitializationException(e);
		}
	}
	
	/* ==================================
	 * ===== Loaders of single file =====
	 * ================================== */
	
	/**
	 * Given a {@link FormCollection} file loads its content
	 * @param formCollFile
	 * @param customForms
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	private static FormCollection loadFormCollection(File formCollFile, Collection<CustomForm> customForms)
			throws ParserConfigurationException, SAXException, IOException {
		FormCollectionXMLReader formCollReader = new FormCollectionXMLReader(formCollFile);
		//load forms listed in the form collection file
		ArrayList<CustomForm> forms = new ArrayList<>();
		Collection<String> formIdList = formCollReader.getCustomFormIds();
		for (String formId : formIdList){
			CustomForm cf = retrieveCustomForm(customForms, formId);
			if (cf != null){
				forms.add(cf);
			} else {
				System.out.println("The FormCollection '" + formCollReader.getId()
					+ "' points to a not existing CustomForm (ID: '" + formId + "')");
			}
		}
		return new FormCollection(formCollReader.getId(), forms);
	}
	
	/**
	 * Given a {@link CustomForm} file loads its content
	 * @param cfFile
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws CustomFormException
	 */
	private static CustomForm loadCustomForm(File cfFile) throws CustomFormInitializationException, ParserConfigurationException, SAXException, IOException {
		CustomFormXMLReader cfReader = new CustomFormXMLReader(cfFile);
		String id = cfReader.getId();
		String name = cfReader.getName();
		String description = cfReader.getDescription();
		String ref = cfReader.getRef();
		String type = cfReader.getType();
		if (type.equals(CustomForm.Types.graph.toString())){
			CustomFormGraph cre = new CustomFormGraph(id, name, description, ref, cfReader.getShowPropertyChain());
			return cre;
		} else if (type.equals(CustomForm.Types.node.toString())){
			return new CustomFormNode(id, name, description, ref);
		} else {
			throw new CustomFormInitializationException("Invalid type '" + type + "' in CustomForm file '" + cfFile.getName() + "'");
		}
	}
	
	/* ==================================
	 * ===== Helper private classes =====
	 * ================================== */
	
	private static class CustomFormsConfigXMLReader {
		
		File cfConfigFile;
		private Document doc;
		
		/**
		 * Initialize the reader for the custom forms configuration file.
		 * There is no check for the existence of the file since {@link CustomFormManager} calls 
		 * {@link CustomFormsConfig#createBasicCustomFormsConfig()} at startup if it doesn't exist.
		 * @throws IOException 
		 * @throws SAXException 
		 * @throws ParserConfigurationException 
		 */
		public CustomFormsConfigXMLReader(File cfConfigFile) throws SAXException, IOException, ParserConfigurationException {
			this.cfConfigFile = cfConfigFile;
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			if (cfConfigFile.exists()) { // TODO check is necessary?
				doc = dBuilder.parse(cfConfigFile);
				doc.getDocumentElement().normalize();
			}
		}
		
		/**
		 * Loads the FormMapping pointed by the CustomFormConfig file
		 * @param formCollections
		 * @return
		 * @throws CustomFormInitializationException
		 */
		public Collection<FormsMapping> getFormMappings(Collection<FormCollection> formCollections) throws CustomFormInitializationException {
			Collection<FormsMapping> mappings = new ArrayList<FormsMapping>();
			if (doc != null) { // parse the config file and retrieve the mappings only if the xml congif file exists
				doc.getDocumentElement().normalize();
				NodeList mappingList = doc.getElementsByTagName("formsMapping");
				for (int i = 0; i < mappingList.getLength(); i++) {
					Node mappingNode = mappingList.item(i);
					if (mappingNode.getNodeType() == Node.ELEMENT_NODE) {
						Element mappingElement = (Element) mappingNode;
						String mapResource = mappingElement.getAttribute("resource");
						String formCollId = mappingElement.getAttribute("formCollection");
						boolean replace = Boolean.parseBoolean(mappingElement.getAttribute("replace"));
						FormCollection formColl = retrieveFormCollection(formCollections, formCollId);
						if (formColl != null) {
							mappings.add(new FormsMapping(mapResource, formColl, replace));
						} else {
							System.out.println("CustomFormConfig initialization: Warning, a Forms mapping in "
									+ cfConfigFile.getPath() + "points to a not existing FormCollection " + formCollId);
						}
					}
				}
			}
			return mappings;
		}
	}
	
	
	
	private static class FormCollectionXMLReader {
		
		private Document doc;
		
		public FormCollectionXMLReader(File formCollectionFile)
				throws ParserConfigurationException, SAXException, IOException {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			if (formCollectionFile.exists()) { // TODO check is necessary?
				doc = dBuilder.parse(formCollectionFile);
				doc.getDocumentElement().normalize();
			}
		}
		
		/**
		 * Return the attribute <code>id</code> of the form collection xml file. 
		 * @return
		 */
		public String getId(){
			if (doc != null){
				Element customFormElement = doc.getDocumentElement();
				String id = customFormElement.getAttribute("id");
				return id.trim();
			} else {
				return null;
			}
		}
		
		/**
		 * Return a collection of <code>id</code> contained in the <code>form</code> tag of the form collection xml file. 
		 * @return
		 */
		public Collection<String> getCustomFormIds() {
			Collection<String> formIdsList = new ArrayList<String>();
			if (doc != null){
				NodeList formList = doc.getElementsByTagName("form");
				for (int i = 0; i < formList.getLength(); i++) {
					Node formNode = formList.item(i);
					if (formNode.getNodeType() == Node.ELEMENT_NODE) {
						Element formElement = (Element) formNode;
						String formId = formElement.getAttribute("id");
						formIdsList.add(formId);
					}
				}
			}
			return formIdsList;
		}
	}
	
	
	private static class CustomFormXMLReader {
		
		private Document doc;
		
		public CustomFormXMLReader(File customFormFile) throws ParserConfigurationException, SAXException, IOException {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			if (customFormFile.exists()) { // TODO check is necessary?
				doc = dBuilder.parse(customFormFile);
				doc.getDocumentElement().normalize();
			}
		}
		
		/**
		 * Return the attribute <code>id</code> of the custom form xml file. 
		 * @return
		 */
		public String getId(){
			Element customFormElement = doc.getDocumentElement();
			String id = customFormElement.getAttribute("id");
			return id.trim();
		}

		/**
		 * Return the attribute <code>name</code> of the custom form xml file.
		 * @return
		 */
		public String getName(){
			Element customFormElement = doc.getDocumentElement();
			String name = customFormElement.getAttribute("name");
			return name.trim();
		}
		
		/**
		 * Return the text content of the <code>description</code> tag contained in the custom form xml file.
		 * @return
		 */
		public String getDescription(){
			String description ="";
			Node descriptionNode = doc.getElementsByTagName("description").item(0);
			if (descriptionNode.getNodeType() == Node.ELEMENT_NODE) {
				Element descriptionElement = (Element) descriptionNode;
				description = descriptionElement.getTextContent();
			}
			return description.trim();
		}
		
		/**
		 * Return the attribute <code>type</code> of the custom form xml file.
		 * @return
		 */
		public String getType(){
			Element customFormElement = doc.getDocumentElement();
			String type = customFormElement.getAttribute("type");
			return type.trim();
		}
		
		/**
		 * Return the text content of the <code>ref</code> tag contained in the custom form xml file.
		 * @return
		 */
		public String getRef(){
			String ref = "";
			Node refNode = doc.getElementsByTagName("ref").item(0);
			if (refNode.getNodeType() == Node.ELEMENT_NODE) {
				Element refElement = (Element) refNode;
				ref = refElement.getTextContent();
			}
			return ref.trim();
		}
		
		/**
		 * Return the <code>showPropertyChain</code> attribute of the <code>ref</code> tag contained in the 
		 * custom form xml file. N.B. the <code>showPropertyChain</code> attribute is available
		 * only if the type of the form is <code>graph</code>
		 * @return
		 */
		public List<IRI> getShowPropertyChain(){
			List<IRI> showPropChain = new ArrayList<IRI>();
			Node refNode = doc.getElementsByTagName("ref").item(0);
			if (refNode.getNodeType() == Node.ELEMENT_NODE) {
				Element refElement = (Element) refNode;
				String showPropChainString = refElement.getAttribute("showPropertyChain");
				//deserialize from string to List<IRI>
				if (!showPropChainString.isEmpty()) {
					String[] splittedChain = showPropChainString.split(",");
					SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();
					for (int i = 0; i < splittedChain.length; i++) {
						showPropChain.add(valueFactory.createIRI(splittedChain[i]));
					}
				}
			}
			return showPropChain;
		}
	}
	
	private static CustomForm retrieveCustomForm(Collection<CustomForm> coll, String cfId) {
		for (CustomForm c : coll) {
			if (c.getId().equals(cfId)) {
				return c;
			}
		}
		return null;
	}
	
	private static FormCollection retrieveFormCollection(Collection<FormCollection> coll, String fcId) {
		for (FormCollection c : coll) {
			if (c.getId().equals(fcId)) {
				return c;
			}
		}
		return null;
	}

}
