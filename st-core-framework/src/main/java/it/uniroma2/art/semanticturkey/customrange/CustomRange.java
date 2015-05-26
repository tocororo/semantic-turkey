package it.uniroma2.art.semanticturkey.customrange;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	
	CustomRange(String id, Collection<CustomRangeEntry> entries){
		this.id = id;
		this.entries = entries;
	}
	
	CustomRange(String id){
		this.id = id;
		this.entries = new ArrayList<CustomRangeEntry>();
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
	
	/**
	 * adds a CustomRangeEntry to the CustomRange
	 * @param crEntry
	 * @return true if the entry is added, false if it is already assigned to the CustomRange 
	 */
	public boolean addEntry(CustomRangeEntry crEntry){
		for (CustomRangeEntry cre : entries){
			if (cre.getId().equals(crEntry.getId()))
				return false;
		}
		entries.add(crEntry);
		return true;
	}
	
	/**
	 * Removes the CustomRangeEntry with the given id from the CustomRange
	 * @param creId
	 */
	public void removeEntry(String creId){
		for (CustomRangeEntry e : entries){
			if (e.getId().equals(creId)){
				entries.remove(e);
				return;
			}
		}
	}
	
	/**
	 * Returns true if the CustomRange contains a CustomRangeEntry with the given ID
	 * @param creId
	 * @return
	 */
	public boolean containsEntry(String creId){
		for (CustomRangeEntry e : entries){
			if (e.getId().equals(creId)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns all the id of the entry contained in the CustomRange
	 * @return
	 */
	public Collection<String> getEntriesId(){
		Collection<String> ids = new ArrayList<String>();
		for (CustomRangeEntry cre : entries){
			ids.add(cre.getId());
		}
		return ids;
	}
	
	/**
	 * Serialize the CustomRange on a xml file.
	 * @throws ParserConfigurationException 
	 */
	public void saveXML(){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element crElement = doc.createElement("customRange");
			doc.appendChild(crElement);
			crElement.setAttribute("id", this.getId());
			
			for (CustomRangeEntry entry : entries){
				Element entryElement = doc.createElement("entry");
				crElement.appendChild(entryElement);
				entryElement.setAttribute("id", entry.getId());
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
			StreamResult result = new StreamResult(new File(CustomRangeProvider.getCustomRangeFolder(), this.getId() + ".xml"));
			transformer.transform(source, result);
		} catch (ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}
	}
	
}
