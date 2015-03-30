package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.exceptions.CustomRangeInitializationException;

import java.io.File;
import java.io.FileNotFoundException;
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

/**
 * A CustomRange is a class which describe the range of a property. It is compose by one or more
 * {@link CustomRangeEntry}.
 * E.g. the CustomRange instance of <code>skos:definition</code> could have two CustomRangeEntry: one for 
 * reified notes and one for normal notes.
 * Each instance of CustomRange is associated to a property and it has an associated XML file into
 * SemanticTurkeyData folder that describe this class.
 * @author Tiziano Lorenzetti
 *
 */
public class CustomRange {

	private String id;
	private Collection<CustomRangeEntry> entries; //range entries associated to the CustomRange instance
	
	/**
	 * Constructor that given the CustomRange id, searches the CustomRange file and loads its content 
	 * @param customRangeId
	 * @throws FileNotFoundException 
	 * @throws CustomRangeInitializationException 
	 */
	public CustomRange(String customRangeId) throws CustomRangeInitializationException {
		File customRangeFile = null;
		File crFolder = CustomRangeProvider.getCustomRangeFolder(); 
		File[] crFiles = crFolder.listFiles();//get list of files into custom range folder
		for (File f : crFiles){//search for the custom range file with the given name
			if (f.getName().equals(customRangeId+".xml")){
				customRangeFile = f;
				break;
			}
		}
		if (customRangeFile == null){
			throw new CustomRangeInitializationException("CustomRange file '" + customRangeId + ".xml' cannot be found in the CustomRange folder");
		}
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		this.id = crReader.getId();
		loadEntries(customRangeFile);
	}
	
	/**
	 * Constructor that given the CustomRange file loads its content
	 * @param customRangeName something like it.uniroma2.art.semanticturkey.customrange.reifiedNotes.xml
	 * @param projectFolderPath
	 * @throws CustomRangeInitializationException 
	 * @throws FileNotFoundException 
	 */
	public CustomRange(File customRangeFile) {
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		this.id = crReader.getId();
		loadEntries(customRangeFile);
	}
	
	private void loadEntries(File customRangeFile) {
		entries = new ArrayList<CustomRangeEntry>();
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		Collection<String> creIdList = crReader.getCustomRangeEntries();
		File creFolder = CustomRangeProvider.getCustomRangeEntryFolder();
		File[] creFiles = creFolder.listFiles();//get list of files into custom range entry folder
		for (String creId : creIdList){
			for (File f : creFiles){//search for the CustomRangeEntry file with the given name
				if (f.getName().equals(creId+".xml")){
					try {
						CustomRangeEntry cre = CustomRangeEntryFactory.createCustomRangeEntry(f);
						entries.add(cre);
					} catch (CustomRangeInitializationException e) {
						// TODO Log per avvertire che il CRE con ID creId non è stato inizializzato
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Returns the ID of the CustomRange.
	 * @return
	 */
	public String getId(){
		return id;
	}
	
	/**
	 * Returns the CustomRangeEntry associated to the given property
	 * @return
	 */
	public Collection<CustomRangeEntry> getEntries(){
		return entries;
	}
	
	/**
	 * Returns the CustomRangeEntry with the given ID. Null if it doesn't exists.
	 * @param entryId
	 * @return
	 */
	public CustomRangeEntry getEntry(String entryId){
		for (CustomRangeEntry cre : entries){
			if (cre.getId().equals(entryId)){
				return cre;
			}
		}
		return null;
	}
	
	public Collection<String> getEntriesId(){
		Collection<String> ids = new ArrayList<String>();
		for (CustomRangeEntry cre : entries){
			ids.add(cre.getId());
		}
		return ids;
	}

	
	private class CustomRangeXMLReader {
		
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
