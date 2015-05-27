package it.uniroma2.art.semanticturkey.customrange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CustomRangeFactory {
	
	private static Logger logger = LoggerFactory.getLogger(CustomRangeFactory.class);
	
	/**
	 * Given the CustomRange file loads its content
	 * @param customRangeFile
	 * @return
	 */
	public static CustomRange loadCustomRange(File customRangeFile, Map<String, CustomRangeEntry> creMap) {
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		return new CustomRange(crReader.getId(), loadEntries(crReader, creMap));
	}
	
	public static CustomRange createEmptyCustomRange(String id){
		return new CustomRange(id);
	}
	
	private static ArrayList<CustomRangeEntry> loadEntries(CustomRangeXMLReader crReader, Map<String, CustomRangeEntry> creMap) {
		ArrayList<CustomRangeEntry> entries = new ArrayList<CustomRangeEntry>();
		Collection<String> creIdList = crReader.getCustomRangeEntries();
		for (String creId : creIdList){
			CustomRangeEntry cre = creMap.get(creId);
			if (cre != null){
				entries.add(cre);
			} else {
				logger.warn("The CustomRange '" + crReader.getId() + "' points to a not existing "
						+ "CustomRangeEntry '" + creId + "'");
			}
		}
		return entries;
	}
	
	private static class CustomRangeXMLReader {
		
		private Document doc;
		
		public CustomRangeXMLReader(File customRangeFile){
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				if (customRangeFile.exists()){
					doc = dBuilder.parse(customRangeFile);
					doc.getDocumentElement().normalize();
				}
			} catch (IOException | ParserConfigurationException | SAXException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Return the attribute <code>id</code> of the custom range xml file. 
		 * @return
		 */
		public String getId(){
			if (doc != null){
				Element customRangeEntryElement = doc.getDocumentElement();
				String id = customRangeEntryElement.getAttribute("id");
				return id.trim();
			} else {
				return null;
			}
		}
		
		/**
		 * Return a collection of <code>id</code> contained in the <code>entry</code> tag of the custom 
		 * range xml file. 
		 * @return
		 */
		public Collection<String> getCustomRangeEntries() {
			Collection<String> creList = new ArrayList<String>();
			if (doc != null){
				NodeList entryList = doc.getElementsByTagName("entry");
				for (int i = 0; i < entryList.getLength(); i++) {
					Node entryNode = entryList.item(i);
					if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
						Element entryElement = (Element) entryNode;
						String idCREntry = entryElement.getAttribute("id");
						creList.add(idCREntry);
					}
				}
			}
			return creList;
		}
	}

}
