package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.owlart.model.ARTNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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

	private File customRangeFile;
	private String id;
	private Collection<CustomRangeEntry> entries; //range entries associated to the CustomRange instance TODO to initialize by parsing the xml and getting the CRE id
	
	
	/**
	 * Constructor that given the CustomRange id, searches the CustomRange file and loads its content 
	 * @param customRangeId
	 * @throws FileNotFoundException 
	 */
	public CustomRange(String customRangeId) throws FileNotFoundException{
		File crFolder = CustomRangeProvider.getCustomRangeFolder(); 
		File[] crFiles = crFolder.listFiles();//get list of files into custom range folder
		for (File f : crFiles){//search for the custom range file with the given name
			if (f.getName().equals(customRangeId+".xml")){
				this.customRangeFile = f;
				break;
			}
		}
		if (customRangeFile == null){
			throw new FileNotFoundException("CustomRange file '" + customRangeId + ".xml' cannot be found in the CustomRange folder");
		}
		loadEntries();
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		this.id = crReader.getId();
	}
	
	/**
	 * Constructor that given the CustomRange file loads its content
	 * @param customRangeName something like it.uniroma2.art.semanticturkey.customrange.reifiedNotes.xml
	 * @param projectFolderPath
	 * @throws FileNotFoundException 
	 */
	public CustomRange(File customRangeFile) throws FileNotFoundException {
		this.customRangeFile = customRangeFile;
		loadEntries();
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		this.id = crReader.getId();
	}
	
	private void loadEntries() throws FileNotFoundException{
		entries = new ArrayList<CustomRangeEntry>();
		CustomRangeXMLReader crReader = new CustomRangeXMLReader(customRangeFile);
		Collection<String> creIdList = crReader.getCustomRangeEntries();
		File creFolder = CustomRangeProvider.getCustomRangeEntryFolder();
		File[] creFiles = creFolder.listFiles();//get list of files into custom range entry folder
		for (String creId : creIdList){
			CustomRangeEntry cre = null;
			for (File f : creFiles){//search for the CustomRangeEntry file with the given name
				if (f.getName().equals(creId+".xml")){
					cre = new CustomRangeEntry(f);
					break;
				}
			}
			if (cre != null){//if the CustomRangeEntry file is found, add it to the entries collection
				entries.add(cre);
			} else {
				throw new FileNotFoundException("CustomRangeEntry file '" + creId + ".xml' cannot be found in the CustomRangeEntry folder."
						+ " Please, check if it exists in the CustomRangeEntry folder or fix the CustomRange file '" + customRangeFile.getName() + "'");
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
	 * Returns all the CustomRangeEntry that match the given object
	 * @param object
	 * @return
	 */
	public Collection<CustomRangeEntry> getMatches(ARTNode object){
		Collection<CustomRangeEntry> coll = new ArrayList<CustomRangeEntry>();
		for (CustomRangeEntry e : entries){
			if (e.matches(object))
				coll.add(e);
		}
		return coll;
	}
	
	/**
	 * Returns an XML representation of the range to put into the XML response (&lt;customRange&gt; tag)
	 * @return
	 */
	public Element getSerialization(){
		/* 
		 * TODO: I would like to return an Element object, but with XMLHelp is possible to create such object
		 * only if a parent element is provided, so what I should do?
		 * - change return type
		 * - pass a treeElement too (like injectPropertyRangeXML in servlet.main.ResourceOld st-core-services)  
		 */
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(customRangeFile));
			StringBuffer sBuffer = new StringBuffer();
			String content = "";
			while ((content = bReader.readLine()) != null) {
				sBuffer.append(content + "\n");
			}
			bReader.close();
			//TODO do something with content: parse it and create serialization XML
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	

}
