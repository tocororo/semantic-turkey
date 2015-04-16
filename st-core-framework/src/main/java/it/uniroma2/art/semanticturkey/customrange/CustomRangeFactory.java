package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.exceptions.CustomRangeInitializationException;

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

public class CustomRangeFactory {
	
	/**
	 * Given a CustomRange id, searches the CustomRange file and loads its content 
	 * @param crId
	 * @return
	 * @throws CustomRangeInitializationException
	 */
	public static CustomRange loadCustomRange(String crId) throws CustomRangeInitializationException{
		File customRangeFile = null;
		File crFolder = CustomRangeProvider.getCustomRangeFolder(); 
		File[] crFiles = crFolder.listFiles();//get list of files into custom range folder
		for (File f : crFiles){//search for the custom range file with the given name
			if (f.getName().equals(crId+".xml")){
				customRangeFile = f;
				break;
			}
		}
		if (customRangeFile == null){
			throw new CustomRangeInitializationException("CustomRange file '" + crId + ".xml' cannot be found in the CustomRange folder");
		}
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		return new CustomRange(crReader.getId(), loadEntries(crReader));
	}
	
	/**
	 * Given the CustomRange file loads its content
	 * @param customRangeFile
	 * @return
	 */
	public static CustomRange loadCustomRange(File customRangeFile) {
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		return new CustomRange(crReader.getId(), loadEntries(crReader));
	}
	
	public static CustomRange createEmptyCustomRange(String id){
		return new CustomRange(id);
	}
	
	private static ArrayList<CustomRangeEntry> loadEntries(CustomRangeXMLReader crReader) {
		ArrayList<CustomRangeEntry> entries = new ArrayList<CustomRangeEntry>();
		Collection<String> creIdList = crReader.getCustomRangeEntries();
		File creFolder = CustomRangeProvider.getCustomRangeEntryFolder();
		File[] creFiles = creFolder.listFiles();//get list of files into custom range entry folder
		for (String creId : creIdList){
			for (File f : creFiles){//search for the CustomRangeEntry file with the given name
				if (f.getName().equals(creId+".xml")){
					try {
						CustomRangeEntry cre = CustomRangeEntryFactory.loadCustomRangeEntry(f);
						entries.add(cre);
					} catch (CustomRangeInitializationException e) {
						// TODO Log per avvertire che il CRE con ID creId non è stato inizializzato
					}
					break;
				}
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
