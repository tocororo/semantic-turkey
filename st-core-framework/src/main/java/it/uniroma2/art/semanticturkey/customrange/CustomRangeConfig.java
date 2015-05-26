package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.exceptions.CustomRangeInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CustomRangeConfig {
	
	private static final String CUSTOM_RANGE_CONFIG_FILENAME = "customRangeConfig.xml";
	
	private Collection<CustomRangeConfigEntry> crConfEntries;
	
	public CustomRangeConfig(){
		CustomRangeConfigXMLReader crConfReader = new CustomRangeConfigXMLReader();
		crConfEntries = crConfReader.getCustomRangeConfigEntries();
	}
	
	public Collection<CustomRangeConfigEntry> getCustomRangeConfigEntries(){
		return crConfEntries;
	}
	
	/**
	 * Given a property returns the CustomRange associated to that property. <code>null</code> if no
	 * custom range is specified for that property.
	 * @param propertyUri
	 * @return
	 */
	public CustomRange getCustomRangeForProperty(String propertyUri) {
		for (CustomRangeConfigEntry crcEntry : crConfEntries){
			if (crcEntry.getProperty().equals(propertyUri))
				return crcEntry.getCutomRange();
		}
		return null;
	}
	
	/**
	 * Adds an entry (property-CustomRange-override)
	 * @param crConfEntry
	 * @return true if the entry is added to the conf, false if an entry with the same prop-CR pair 
	 * already exists
	 */
	public boolean addEntry(CustomRangeConfigEntry crConfEntry){
		for (CustomRangeConfigEntry e : crConfEntries){
			if (e.getProperty().equals(crConfEntry.getProperty()) && 
					e.getCutomRange().getId().equals(crConfEntry.getCutomRange().getId()))
				return false;
		}
		crConfEntries.add(crConfEntry);
		return true;
	}
	
	/**
	 * Remove property-CustomRange pair with the given property
	 * @param propertyUri
	 * @param cr
	 */
	public void removeConfigEntryFromProperty(String propertyUri, CustomRange cr){
		for (CustomRangeConfigEntry e : crConfEntries){
			if (e.getProperty().equals(propertyUri) && e.getCutomRange().getId().equals(cr.getId())){
				crConfEntries.remove(e);
				return;
			}
		}
	}
	
	/**
	 * Remove property-CustomRange pair with the given CustomRange
	 * @param crId
	 */
	public void removeConfigEntryWithCrId(String crId){
		Iterator<CustomRangeConfigEntry> it = crConfEntries.iterator();
		while (it.hasNext()){
			CustomRangeConfigEntry crcEntry = it.next();
			if (crcEntry.getCutomRange().getId().equals(crId)){
				it.remove();
			}
		}
	}
	
	/**
	 * Tells whether a CustomRange exists or not for the given property 
	 * @param propertyUri
	 * @return
	 */
	public boolean existsCustomRangeForProperty(String propertyUri){
		for (CustomRangeConfigEntry crcEntry : crConfEntries){
			if (crcEntry.getProperty().equals(propertyUri))
				return true;
		}
		return false;
	}
	
	/**
	 * Tells if the CustomRange should be joined in the legacy getRange
	 * response or it should replace it.
	 * @param propertyUri
	 * @return
	 */
	public boolean getReplaceRanges(String propertyUri){
		for (CustomRangeConfigEntry crcEntry : crConfEntries){
			if (crcEntry.getProperty().equals(propertyUri))
				return crcEntry.getReplaceRange();
		}
		return false;
	}
	
	/**
	 * Serialize the CustomRangeConfig on a xml file.
	 */
	public void saveXML(){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			
			Element crConfElement = doc.createElement("customRangeConfig");
			doc.appendChild(crConfElement);
			
			for (CustomRangeConfigEntry entry : crConfEntries){
				Element entryElement = doc.createElement("configEntry");
				crConfElement.appendChild(entryElement);
				entryElement.setAttribute("property", entry.getProperty());
				entryElement.setAttribute("idCustomRange", entry.getCutomRange().getId());
				entryElement.setAttribute("replaceRanges", entry.getReplaceRange()+"");
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
			StreamResult result = new StreamResult(new File(CustomRangeProvider.getCustomRangeFolder(), CUSTOM_RANGE_CONFIG_FILENAME));
			transformer.transform(source, result);
		} catch (ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a basic customRangeConfig.xml containing just some instructions and examples
	 */
	public void createBasicCustomRangeConfig() {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			Element crConfElement = doc.createElement("customRangeConfig");
			doc.appendChild(crConfElement);

			Comment comment;
			String description = "\nThis element should contains a collection of configEntry elements "
					+ "which with properties are associated with the id of the CustomRange.\n"
					+ "Here it is an example of configEntry:\n\n"
					+ "<configEntry property=\"http://www.w3.org/2004/02/skos/core#definition\" "
					+ "idCustomRange=\"it.uniroma2.art.semanticturkey.customrange.note\" "
					+ "replaceRanges=\"false\" />\n\n"
					+ "The id of the CR must be the name of the CR file without extension.\n"
					+ "E.g. if idCustomRange=\"it.uniroma2.art.semanticturkey.customrange.foo\" "
					+ "the CR file must be names \"it.uniroma2.art.semanticturkey.customrange.foo.xml\".\n"
					+ "The attribute \"replaceRanges\" is boolean: \ntrue lets the CustomRange replace the "
					+ "\"classic\" ranges of the property;\nfalse adds the CustomRange to the \"classic\" range.\n";
			comment = doc.createComment(description);
			crConfElement.appendChild(comment);
			crConfElement.appendChild(comment);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			Properties outputProps = new Properties();
			outputProps.setProperty("encoding", "UTF-8");
			outputProps.setProperty("indent", "yes");
			transformer.setOutputProperties(outputProps);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(CustomRangeProvider.getCustomRangeFolder(), "customRangeConfig.xml"));
			transformer.transform(source, result);
		} catch (ParserConfigurationException | TransformerException e) {
			e.printStackTrace();
		}
	}
	
	private class CustomRangeConfigXMLReader {
		
		private static final String CONFIG_ENTRY_TAG = "configEntry";
		private static final String PROPERTY_ATTRIBUTE_TAG = "property";
		private static final String REPLACE_RANGES_ATTRIBUTE_TAG = "replaceRanges";
		private static final String ID_ATTRIBUTE_TAG = "idCustomRange";
		
		private Document doc;
		
		/**
		 * Initialize the reader for the given file. If the file doesn't exists, no Exception will
		 * be thrown.
		 * @param crConfigFile
		 */
		public CustomRangeConfigXMLReader(){
			try {
				String crConfigFilePath = CustomRangeProvider.getCustomRangeFolder().getPath() + File.separator + CUSTOM_RANGE_CONFIG_FILENAME;
				File crConfigFile = new File(crConfigFilePath);
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				if (crConfigFile.exists()){
					doc = dBuilder.parse(crConfigFile);
					doc.getDocumentElement().normalize();
				}
			} catch (IOException | ParserConfigurationException | SAXException e) {
				e.printStackTrace();
			}
		}
		
		public Collection<CustomRangeConfigEntry> getCustomRangeConfigEntries() {
			Collection<CustomRangeConfigEntry> crcEntries = new ArrayList<CustomRangeConfigEntry>();
			if (doc != null){
				NodeList entryList = doc.getElementsByTagName(CONFIG_ENTRY_TAG);
				for (int i=0; i<entryList.getLength(); i++){
					Node crNode = entryList.item(i);
					if (crNode.getNodeType() == Node.ELEMENT_NODE) {
						Element crElement = (Element) crNode;
						String crProp = crElement.getAttribute(PROPERTY_ATTRIBUTE_TAG);
						String crId = crElement.getAttribute(ID_ATTRIBUTE_TAG);
						boolean replace = Boolean.parseBoolean(crElement.getAttribute(REPLACE_RANGES_ATTRIBUTE_TAG));
						try {
							CustomRange cr = CustomRangeFactory.loadCustomRange(crId);
							crcEntries.add(new CustomRangeConfigEntry(crProp, cr, replace));
						} catch (CustomRangeInitializationException e) {
							//TODO testare
							System.out.println("CR with id " + crId + " was not found");
						}
					}
				}
			}
			return crcEntries;
		}
	}

}
