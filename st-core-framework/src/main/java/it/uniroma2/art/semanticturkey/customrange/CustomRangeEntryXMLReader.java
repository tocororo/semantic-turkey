package it.uniroma2.art.semanticturkey.customrange;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class CustomRangeEntryXMLReader {
	
	private Document doc;
	
	public CustomRangeEntryXMLReader(File customRangeEntryFile){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(customRangeEntryFile);
			doc.getDocumentElement().normalize();
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

}
