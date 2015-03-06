package it.uniroma2.art.semanticturkey.customrange;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class CustomRangeEntry {
	
	private String id;
	private String name;
	private String description;
	private String type;
	private String ref;
	
	/**
	 * Constructor that given the CustomRangeEntry ID searches the related file and loads its content 
	 * @param customRangeEntryId
	 * @throws FileNotFoundException 
	 */
	public CustomRangeEntry(String customRangeEntryId) throws FileNotFoundException{
		File creFolder = CustomRangeProvider.getCustomRangeEntryFolder();
		File[] creFiles = creFolder.listFiles();//get list of files into custom range entry folder
		for (File f : creFiles){//search for the custom range entry file with the given id/name
			if (f.getName().equals(customRangeEntryId+".xml")){
				CustomRangeEntryXMLReader creReader = new CustomRangeEntryXMLReader(f);
				this.id = creReader.getId();
				this.name = creReader.getName();
				this.description = creReader.getDescription();
				this.type = creReader.getType();
				this.ref = creReader.getRef();
				return; //stop looking for the custom range entry file
			}
		}
		throw new FileNotFoundException("CustomRangeEntry file '" + customRangeEntryId + ".xml' "
				+ "cannot be found in the CustomRangeEntry folder");
	}
	
	/**
	 * Constructor that given the CustomRangeEntry file loads its content
	 * @param customRangeEntryFile
	 * @throws FileNotFoundException 
	 */
	public CustomRangeEntry(File customRangeEntryFile) {
		CustomRangeEntryXMLReader creReader = new CustomRangeEntryXMLReader(customRangeEntryFile);
		this.id = creReader.getId();
		this.name = creReader.getName();
		this.description = creReader.getDescription();
		this.type = creReader.getType();
		this.ref = creReader.getRef();
	}
	
	/**
	 * Returns the ID of the CustomRangeEntry
	 * @return
	 */
	public String getId(){
		return id;
	}
	
	/**
	 * Returns the name of the CustomRangeEntry
	 * @return
	 */
	public String getName(){
		return name;
	}
		
	/**
	 * Returns a verbose description about the CustomRangeEntry
	 * @return
	 */
	public String getDescription(){
		return description;
	}
	
	/**
	 * Returns the type of the CustomRangeEntry. It can be <code>node</code> or <code>graph</code> 
	 * @return
	 */
	public String getType(){
		return type;
	}
	
	/**
	 * Returns the ref of the CustomRangeEntry. It could be a CODA rule if the type of the CustomRangeEntry
	 * is <coda>graph</code>, or a CODA converter if the type is <code>node</code>.
	 * @return
	 */
	public String getRef(){
		return ref;
	}
	
	private class CustomRangeEntryXMLReader {
		
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
	}
}
