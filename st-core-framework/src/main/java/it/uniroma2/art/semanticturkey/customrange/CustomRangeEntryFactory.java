package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.exceptions.CustomRangeException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class CustomRangeEntryFactory {
	
	public static CustomRangeEntry loadCustomRangeEntry(File creFile) throws CustomRangeException{
		CustomRangeEntryXMLReader creReader = new CustomRangeEntryXMLReader(creFile);
		String id = creReader.getId();
		String name = creReader.getName();
		String description = creReader.getDescription();
		String ref = creReader.getRef();
		String type = creReader.getType();
		if (type.equals(CustomRangeEntry.Types.graph.toString())){
			CustomRangeEntryGraph cre = new CustomRangeEntryGraph(id, name, description, ref, creReader.getShowPropertyChain());
			return cre;
		} else if (type.equals(CustomRangeEntry.Types.node.toString())){
			return new CustomRangeEntryNode(id, name, description, ref);
		} else {
			throw new CustomRangeException("Invalid type '" + type + "' in "
					+ "CustomRangeEntry file '" + creFile.getName() + "'");
		}
	}
	
	private static class CustomRangeEntryXMLReader {
		
		private Document doc;
		
		public CustomRangeEntryXMLReader(File customRangeEntryFile){
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				if (customRangeEntryFile.exists()){
					doc = dBuilder.parse(customRangeEntryFile);
					doc.getDocumentElement().normalize();
				}
			} catch (IOException | ParserConfigurationException | SAXException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Return the attribute <code>id</code> of the custom range entry xml file. 
		 * @return
		 */
		public String getId(){
			Element customRangeEntryElement = doc.getDocumentElement();
			String id = customRangeEntryElement.getAttribute("id");
			return id.trim();
		}

		/**
		 * Return the attribute <code>name</code> of the custom range entry xml file.
		 * @return
		 */
		public String getName(){
			Element customRangeEntryElement = doc.getDocumentElement();
			String name = customRangeEntryElement.getAttribute("name");
			return name.trim();
		}
		
		/**
		 * Return the text content of the <code>description</code> tag contained in the custom range entry xml file.
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
		 * Return the attribute <code>type</code> of the custom range entry xml file.
		 * @return
		 */
		public String getType(){
			Element customRangeEntryElement = doc.getDocumentElement();
			String type = customRangeEntryElement.getAttribute("type");
			return type.trim();
		}
		
		/**
		 * Return the text content of the <code>ref</code> tag contained in the custom range entry xml file.
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
		 * custom range entry xml file. N.B. the <code>showPredicate</code> attribute is available
		 * only if the type of the entry is <code>graph</code>
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
