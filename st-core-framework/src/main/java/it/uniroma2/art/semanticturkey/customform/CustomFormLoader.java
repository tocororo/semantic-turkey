package it.uniroma2.art.semanticturkey.customform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CustomFormLoader {
	
	/**
	 * Given a {@link FormCollection} file loads its content
	 * @param formCollFile
	 * @param customForms
	 * @return
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static FormCollection loadFormCollection(File formCollFile, CustomFormList customForms) throws ParserConfigurationException, SAXException, IOException {
		FormCollectionXMLReader formCollReader = new FormCollectionXMLReader(formCollFile);
		//load forms listed in the form collection file
		ArrayList<CustomForm> forms = new ArrayList<>();
		Collection<String> formIdList = formCollReader.getCustomFormIds();
		for (String formId : formIdList){
			CustomForm cf = customForms.get(formId);
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
	public static CustomForm loadCustomForm(File cfFile) throws CustomFormInitializationException, ParserConfigurationException, SAXException, IOException {
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

}
