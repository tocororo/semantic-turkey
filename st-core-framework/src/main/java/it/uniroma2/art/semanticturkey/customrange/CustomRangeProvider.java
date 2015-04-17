package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.exceptions.CustomRangeInitializationException;
import it.uniroma2.art.semanticturkey.resources.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

/*
 * This class should be used as Autowired (singleton scoped) component, because its initialization 
 * is an heavy process so, this should be automatically initialized by Karaf at the start. 
 */
@Component
public class CustomRangeProvider {
	
	private static final String CUSTOM_RANGE_FOLDER_NAME = "customRange";
	private static final String CUSTOM_RANGE_ENTRY_FOLDER_NAME = "customRangeEntry";
	
	private CustomRangeConfig crConfig;
	private Collection<CustomRange> crList;
	private Collection<CustomRangeEntry> creList;
	
	public CustomRangeProvider() {
		crConfig = new CustomRangeConfig();
		crList = new ArrayList<CustomRange>();
		creList = new ArrayList<CustomRangeEntry>();
		//check existing of CR folders hierarchy
		File crFolder = getCustomRangeFolder();
		File creFolder = getCustomRangeEntryFolder();
		if (!crFolder.exists() && !creFolder.exists()){
			initializeCRBasicHierarchy();
		} else {
			//initialize CR list (load files from CR folder)
			File[] crFiles = crFolder.listFiles();
			for (File f : crFiles){
				if (f.getName().startsWith("it.uniroma2.art.semanticturkey.customrange"))
					crList.add(CustomRangeFactory.loadCustomRange(f));
			}
			//initialize CRE list (load files from CRE folder)
			File[] creFiles = creFolder.listFiles();
			for (File f : creFiles){
				if (f.getName().startsWith("it.uniroma2.art.semanticturkey.entry")){
					try {
						creList.add(CustomRangeEntryFactory.loadCustomRangeEntry(f));
					} catch (CustomRangeInitializationException e) {
						//TODO Write in the log to warn that the CRE defined by f has not been initialized
					}
				}
			}
		}
	}
	
	public CustomRangeConfig getCustomRangeConfig(){
		return crConfig;
	}
	
	/**
	 * Returns all the CustomRange available into the custom range folder of the project
	 * @return
	 */
	public Collection<CustomRange> getAllCustomRanges() {
		return crList;
	}
	
	/**
	 * Adds a CustomRange to the CR collection
	 * @param customRange
	 */
	public void addCustomRange(CustomRange customRange){
		crList.add(customRange);
	}
	
	/**
	 * Returns all the CustomRangeEntry available into the custom range entry folder of the project
	 * @return
	 */
	public Collection<CustomRangeEntry> getAllCustomRangeEntries() {
		return creList;
	}
	
	/**
	 * Adds a CustomRange to the CRE collection
	 * @param crEntry
	 */
	public void addCustomRangeEntries(CustomRangeEntry crEntry){
		creList.add(crEntry);
	}
	
	/**
	 * Given a property returns the CustomRange associated to that property. <code>null</code> if no
	 * custom range is specified for that property.
	 * @param propertyUri
	 * @return
	 */
	public CustomRange getCustomRangeForProperty(String propertyUri) {
		return crConfig.getCustomRangeForProperty(propertyUri);
	}
	
	/**
	 * Returns all the CustomRangeEntry for the given property. If the property has not a CustomRange
	 * associated, then returns an empty collection
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntry> getCustomRangeEntriesForProperty(String propertyUri){
		CustomRange cr = getCustomRangeForProperty(propertyUri);
		if (cr != null){
			return cr.getEntries();	
		}
		else return new ArrayList<CustomRangeEntry>();
	}
	
	/**
	 * Returns all the CustomRangeEntryGraph for the given property
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntryGraph> getCustomRangeEntriesGraphForProperty(String propertyUri){
		Collection<CustomRangeEntryGraph> creGraph = new ArrayList<CustomRangeEntryGraph>();
		CustomRange cr = getCustomRangeForProperty(propertyUri);
		if (cr != null){
			for (CustomRangeEntry cre : cr.getEntries()){
				if (cre.isTypeGraph())
					creGraph.add(cre.asCustomRangeEntryGraph());
			}
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
		return crConfig.existsCustomRangeForProperty(propertyUri);
	}
	
	/**
	 * Tells whether a property has or not a CustomRangeEntry of type graph 
	 * @param propertyUri
	 * @return
	 */
	public boolean existsCustomRangeEntryGraphForProperty(String propertyUri){
		return (!getCustomRangeEntriesGraphForProperty(propertyUri).isEmpty());
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
	
	/**
	 * Creates the following hierarchy in SemanticTurkeyData folder:
	 * <ul>
	 *  <li>SemanticTurkeyData</li>
	 *   <ul>
	 *    <li>CustomRange</li>
	 *     <ul>
	 *      <li>customRangeConfig.xml</li>
	 *      <li>customRangeEntry</li>
	 *     </ul>
	 *   </ul>
	 * </ul>
	 * Where customRangeConfig.xml is a sample config file.
	 * To invoke only at the first start of SemanticTurkey server if the hierarchy is not ready
	 */
	private void initializeCRBasicHierarchy(){
		File crFolder = getCustomRangeFolder();
		File creFolder = getCustomRangeEntryFolder();
		crFolder.mkdir();
		creFolder.mkdir();
		crConfig.createBasicCustomRangeConfig();
	}
	

}
