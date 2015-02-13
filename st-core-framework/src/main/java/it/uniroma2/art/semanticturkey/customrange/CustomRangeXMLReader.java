package it.uniroma2.art.semanticturkey.customrange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CustomRangeXMLReader {
	
	private Document doc;
	
	public CustomRangeXMLReader(File customRangeFile){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(customRangeFile);
			doc.getDocumentElement().normalize();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the attribute <code>id</code> of the custom range xml file. 
	 * @return
	 */
	public String getId(){
		Element customRangeEntryElement = doc.getDocumentElement();
		String id = customRangeEntryElement.getAttribute("id");
		return id.trim();
	}
	
	/**
	 * Return a collection of <code>id</code> contained in the <code>entry</code> tag of the custom 
	 * range xml file. 
	 * @return
	 */
	public Collection<String> getCustomRangeEntries() {
		Collection<String> creList = new ArrayList<String>();
		doc.getDocumentElement().normalize();
		NodeList entryList = doc.getElementsByTagName("entry");
		for (int i = 0; i < entryList.getLength(); i++) {
			Node entryNode = entryList.item(i);
			if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
				Element entryElement = (Element) entryNode;
				String idCREntry = entryElement.getAttribute("id");
				creList.add(idCREntry);
			}
		}
		return creList;
	}

}
