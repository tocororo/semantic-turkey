package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.exceptions.CustomRangeInitializationException;
import it.uniroma2.art.semanticturkey.resources.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * This class should be used as Autowired (singleton scoped) component, because its initialization 
 * is an heavy process so, this should be automatically initialized by Karaf at the start. 
 */
@Component
public class CustomRangeProvider {
	
	private static final String CUSTOM_RANGE_CONFIG_FILENAME = "customRangeConfig.xml";
	private static final String CUSTOM_RANGE_FOLDER_NAME = "customRange";
	private static final String CUSTOM_RANGE_ENTRY_FOLDER_NAME = "customRangeEntry";
	
	private Collection<CustomRangeConfigEntry> crConfig;
	private Collection<CustomRange> crList;
	private Collection<CustomRangeEntry> creList;
	
	public CustomRangeProvider() {
		//initialize config
		CustomRangeConfigXMLReader crConfReader = new CustomRangeConfigXMLReader();
		crConfig = crConfReader.getCustomRangeConfigEntries();
		//initialize CR list (load files from CR folder)
		crList = new ArrayList<CustomRange>();
		File[] crFiles = getCustomRangeFolder().listFiles();
		for (File f : crFiles){
			if (f.getName().startsWith("it.uniroma2.art.semanticturkey.customrange"))
				crList.add(new CustomRange(f));
		}
		//initialize CRE list (load files from CRE folder)
		creList = new ArrayList<CustomRangeEntry>();
		File[] creFiles = getCustomRangeEntryFolder().listFiles();
		for (File f : creFiles){
			if (f.getName().startsWith("it.uniroma2.art.semanticturkey.entry")){
				try {
					creList.add(CustomRangeEntryFactory.createCustomRangeEntry(f));
				} catch (CustomRangeInitializationException e) {
					//TODO nel log scrivere che non è stato inizializzato il CRE del file f
				}
			}
		}
	}
	
	public Collection<CustomRangeConfigEntry> getCustomRangeConfig(){
		return crConfig;
	}
	
	/**
	 * Returns all the CustomRange available into the custom range folder of the project
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public Collection<CustomRange> getAllCustomRanges() {
		return crList;
	}
	
	/**
	 * Returns all the CustomRangeEntry available into the custom range entry folder of the project
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public Collection<CustomRangeEntry> getAllCustomRangeEntries() {
		return creList;
	}
	
	/**
	 * Given a property returns the CustomRange associated to that property. <code>null</code> if no
	 * custom range is specified for that property.
	 * @param propertyUri
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public CustomRange getCustomRangeForProperty(String propertyUri) {
		for (CustomRangeConfigEntry crcEntry : crConfig){
			if (crcEntry.getProperty().equals(propertyUri))
				return crcEntry.getCutomRange();
		}
		return null;
	}
	
	/**
	 * Returns all the CustomRangeEntry for the given property
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntry> getCustomRangeEntriesForProperty(String propertyUri){
		CustomRange cr = getCustomRangeForProperty(propertyUri);
		return cr.getEntries();
	}
	
	/**
	 * Returns all the CustomRangeEntryGraph for the given property
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntryGraph> getCustomRangeEntriesGraphForProperty(String propertyUri){
		Collection<CustomRangeEntryGraph> creGraph = new ArrayList<CustomRangeEntryGraph>();
		CustomRange cr = getCustomRangeForProperty(propertyUri);
		for (CustomRangeEntry cre : cr.getEntries()){
			if (cre.isTypeGraph())
				creGraph.add(cre.asCustomRangeEntryGraph());
		}
		return creGraph;
	}
	
	/**
	 * Returns all the CustomRangeEntryNode for the given property
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntryNode> getCustomRangeEntriesNodeForProperty(String propertyUri){
		Collection<CustomRangeEntryNode> creNode = new ArrayList<CustomRangeEntryNode>();
		CustomRange cr = getCustomRangeForProperty(propertyUri);
		for (CustomRangeEntry cre : cr.getEntries()){
			if (cre.isTypeNode())
				creNode.add(cre.asCustomRangeEntryNode());
		}
		return creNode;
	}
	
	/**
	 * Returns the CustomRange with the given ID. <code>null</code> if there is no CR with that id.
	 * @param crEntryId
	 * @return
	 */
	public CustomRange getCustomRangeById(String crId){
		for (CustomRange cr : crList){
			if (cr.getId().equals(crId)){
				return cr;
			}
		}
		return null;
	}
	
	/**
	 * Returns the CustomRangeEntry with the given ID
	 * @param crEntryId
	 * @return
	 */
	public CustomRangeEntry getCustomRangeEntryById(String crEntryId){
		for (CustomRangeEntry cre : creList){
			if (cre.getId().equals(crEntryId)){
				return cre;
			}
		}
		return null;
	}
	
	/**
	 * Tells whether a CustomRange exists or not for the given property 
	 * @param propertyUri
	 * @return
	 */
	public boolean existsCustomRangeForProperty(String propertyUri){
		for (CustomRangeConfigEntry crcEntry : crConfig){
			if (crcEntry.getProperty().equals(propertyUri))
				return true;
		}
		return false;
	}
	
	/**
	 * Given a property returns the <code>mode</code> associated to that property (default 
	 * <code>union</code>). <code>mode</code> can assume two different values: <code>union</code> or
	 * <code>override</code> and tells if the CustomRange should be joined in the legacy getRange
	 * response or it should override it.
	 * @param propertyUri
	 * @return
	 */
	public boolean getReplaceRanges(String propertyUri){
		for (CustomRangeConfigEntry crcEntry : crConfig){
			if (crcEntry.getProperty().equals(propertyUri))
				return crcEntry.getReplaceRange();
		}
		return false;
	}
	
	/**
	 * Returns the CustomRange folder, located under SemanticTurkeyData/ folder
	 * @return
	 */
	protected static File getCustomRangeFolder(){
		return new File(Config.getDataDir() + File.separator + CUSTOM_RANGE_FOLDER_NAME);
	}
	
	/**
	 * Returns the CustoRangeEntry folder, located under SemanticTurkeyData/custoRange/ folder
	 * @return
	 */
	protected static File getCustomRangeEntryFolder(){
		return new File(getCustomRangeFolder() + File.separator + CUSTOM_RANGE_ENTRY_FOLDER_NAME);
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
							CustomRange cr = new CustomRange(crId);
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
