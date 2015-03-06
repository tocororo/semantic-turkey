package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.resources.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is a provider of CustomRange. 
 * @author Tiziano Lorenzetti
 *
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CustomRangeProvider {
	
	private static final String CUSTOM_RANGE_CONFIG_FILENAME = "customRangeConfig.xml";
	private static final String CUSTOM_RANGE_FOLDER_NAME = "customRange";
	private static final String CUSTOM_RANGE_ENTRY_FOLDER_NAME = "customRangeEntry";
	
	public static final String CR_MODE_UNION = "union";
	public static final String CR_MODE_OVERRIDE = "override";
	
	private CustomRangeConfigXMLReader customRangeConfig; //contains the mapping between property and CustomRange
	
	public CustomRangeProvider() {
		String crConfigFilePath = getCustomRangeFolder().getPath() + File.separator + CUSTOM_RANGE_CONFIG_FILENAME;
		File crConfigFile = new File(crConfigFilePath);
		customRangeConfig = new CustomRangeConfigXMLReader(crConfigFile);
	}
	
	/**
	 * Returns all the CustomRange available into the custom range folder of the project
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public Collection<CustomRange> loadCustomRanges() {
		Collection<CustomRange> customRanges = new ArrayList<CustomRange>();
		File[] crFiles = getCustomRangeFolder().listFiles();//get list of files into custom range folder
		for (File f : crFiles){//search for the custom range files
			if (f.getName().startsWith("it.uniroma2.art.semanticturkey.customrange")){
				customRanges.add(new CustomRange(f));
			}
		}
		return customRanges;
	}
	
	/**
	 * Given a property returns the CustomRange associated to that property. <code>null</code> if no
	 * custom range is specified for that property.
	 * @param property
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public CustomRange loadCustomRange(String property) {
		String customRangeId = customRangeConfig.getCustomRangeId(property);
		if (customRangeId != null){ //custom range of the given property specified in customRangeConfig
			File crFolder = CustomRangeProvider.getCustomRangeFolder();
			File[] crFiles = crFolder.listFiles();//get list of files into custom range folder
			for (File f : crFiles){//search for the custom range file with the given name
				if (f.getName().equals(customRangeId+".xml")){
					return new CustomRange(f);
				}
			} 
			//message to warn that custom range with the specified "customRangeId" has not be found in customRange folder ?? 
		}
		return null;
	}
	
	/**
	 * Given a property returns the <code>mode</code> associated to that property (default 
	 * <code>union</code>). <code>mode</code> can assume two different values: <code>union</code> or
	 * <code>override</code> and tells if the CustomRange should be joined in the legacy getRange
	 * response or it should override it.
	 * @param property
	 * @return
	 */
	public String getResponseMode(String property){
		return customRangeConfig.getResponseMode(property);
	}
	
	/**
	 * Returns the CustomRange folder, located under SemanticTurkeyData/ folder
	 * @return
	 */
	public static File getCustomRangeFolder(){
		return new File(Config.getDataDir() + File.separator + CUSTOM_RANGE_FOLDER_NAME);
	}
	
	/**
	 * Returns the CustoRangeEntry folder, located under SemanticTurkeyData/custoRange/ folder
	 * @return
	 */
	public static File getCustomRangeEntryFolder(){
		return new File(getCustomRangeFolder() + File.separator + CUSTOM_RANGE_ENTRY_FOLDER_NAME);
	}

	
	
	private class CustomRangeConfigXMLReader {
		
		private Document doc;
		
		private static final String CONFIG_ENTRY_TAG = "configEntry";
		private static final String PROPERTY_ATTRIBUTE_TAG = "property";
		private static final String MODE_ATTRIBUTE_TAG = "responseMode";
		private static final String ID_ATTRIBUTE_TAG = "idCustomRange";
		
		/**
		 * Initialize the reader for the given file. If the file doesn't exists, no Exception will
		 * be thrown.
		 * @param crConfigFile
		 */
		public CustomRangeConfigXMLReader(File crConfigFile){
			try {
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
		
		/**
		 * Returns the id of the customRange related to the given property
		 * @param property
		 * @return
		 */
		public String getCustomRangeId(String property){
			String crId = null;
			if (doc != null){
				NodeList entryList = doc.getElementsByTagName(CONFIG_ENTRY_TAG);
				for (int i=0; i<entryList.getLength(); i++){
					Node crNode = entryList.item(i);
					if (crNode.getNodeType() == Node.ELEMENT_NODE) {
						Element crElement = (Element) crNode;
						String crProp = crElement.getAttribute(PROPERTY_ATTRIBUTE_TAG);
						if (crProp.equals(property)){
							crId = crElement.getAttribute(ID_ATTRIBUTE_TAG);
							break;
						}
					}
				}
			}
			return crId;
		}
		
		/**
		 * Returns the mode of the customRange related to the given property. <code>mode</code> can
		 * assume two different values: <code>union</code> (default) or <code>override</code> and tells
		 * if the CustomRange should be joined in the legacy getRange response or it should override it.
		 * @param property
		 * @return
		 */
		public String getResponseMode(String property){
			String mode = null;
			if (doc != null){
				NodeList entryList = doc.getElementsByTagName(CONFIG_ENTRY_TAG);
				for (int i=0; i<entryList.getLength(); i++){
					Node entryNode = entryList.item(i);
					if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
						Element crElement = (Element) entryNode;
						String crProp = crElement.getAttribute(PROPERTY_ATTRIBUTE_TAG);
						if (crProp.equals(property)){
							mode = crElement.getAttribute(MODE_ATTRIBUTE_TAG);
							break;
						}
					}
				}
			}
			//if mode is not specified, or it is an invalid value, return "union", otherwise return
			//the specified value
			if (mode == null) {
				return CR_MODE_UNION;
			} else if (mode.equals(CR_MODE_OVERRIDE) || mode.equals(CR_MODE_UNION)) {
				return mode;
			} else {
				return CR_MODE_UNION;
			}
		}

	}
}
