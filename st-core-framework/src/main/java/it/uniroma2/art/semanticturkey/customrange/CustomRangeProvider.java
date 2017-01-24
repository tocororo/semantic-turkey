package it.uniroma2.art.semanticturkey.customrange;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.semanticturkey.exceptions.CustomRangeException;
import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;

/*
 * This class should be used as Autowired (singleton scoped) component, because its initialization 
 * is an heavy process so, this should be automatically initialized by Karaf at the start. 
 */
@Component
public class CustomRangeProvider {
	
	private static final String CUSTOM_RANGE_FOLDER_NAME = "customRange";
	private static final String CUSTOM_RANGE_ENTRY_FOLDER_NAME = "customRangeEntry";
	
	private CustomRangeConfig crConfig;
	
	//Map of id-CR object pairs (contains all the CR, also the ones that are not reachable from the
	//config tree, namely the ones that are not associated to any property)
	private Map<String, CustomRange> crMap = new HashMap<String, CustomRange>();
	//Map of id-CRE object pairs (contains all the CRE, also the ones that are not reachable from the
	//config tree, namely the ones that are not contained in any CR)
	private Map<String, CustomRangeEntry> creMap = new HashMap<String, CustomRangeEntry>();
	
	private static Logger logger = LoggerFactory.getLogger(CustomRangeProvider.class);
	
	public CustomRangeProvider() {
		//check existing of CR folders hierarchy
		File crFolder = getCustomRangeFolder();
		File creFolder = getCustomRangeEntryFolder();
		if (!crFolder.exists() && !creFolder.exists()){
			crConfig = new CustomRangeConfig(crMap, creMap);
			initializeCRBasicHierarchy();
		} else {
			//initialize CRE list (load files from CRE folder) and store them in the CRE map
			File[] creFiles = creFolder.listFiles();
			for (File f : creFiles){
				if (f.getName().startsWith("it.uniroma2.art.semanticturkey.entry")){
					try {
						CustomRangeEntry cre = CustomRangeEntryFactory.loadCustomRangeEntry(f);
						creMap.put(cre.getId(), cre);
					} catch (CustomRangeException e) {
						logger.error("Failed to initialize CustomRangeEntry " + f.getName() + ". It may contain"
								+ "some errors.");
					}
				}
			}
			//initialize CR list (load files from CR folder) and store them in the CR map
			File[] crFiles = crFolder.listFiles();
			for (File f : crFiles){
				if (f.getName().startsWith("it.uniroma2.art.semanticturkey.customrange")){
					CustomRange cr = CustomRangeFactory.loadCustomRange(f, creMap);
					crMap.put(cr.getId(), cr);
				}
			}
			crConfig = new CustomRangeConfig(crMap, creMap);
		}
	}
	
	public CustomRangeConfig getCustomRangeConfig(){
		return crConfig;
	}
	
	/* ##################
	 * ###### READ ######
	 * ################## */
	
	// CUSTOM RANGE
	
	/**
	 * Returns all the CustomRange available into the custom range folder of the project
	 * @return
	 */
	public Collection<CustomRange> getAllCustomRanges() {
		return crMap.values();
	}
	
	/**
	 * Given a property returns the CustomRange associated to that property. <code>null</code> if no
	 * custom range is specified for that property.
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRange> getCustomRangesForProperty(String propertyUri) {
		return crConfig.getCustomRangesForProperty(propertyUri);
	}
	
	/**
	 * Returns the CustomRange with the given ID. <code>null</code> if there is no CR with that id.
	 * @param crEntryId
	 * @return
	 */
	public CustomRange getCustomRangeById(String crId){
		return crMap.get(crId);
	}
	
	/**
	 * Tells whether a CustomRange exists or not for the given property 
	 * @param propertyUri
	 * @return
	 */
	public boolean existsCustomRangeForProperty(String propertyUri){
		return crConfig.existsCustomRangeForProperty(propertyUri);
	}
	
	// CUSTOM RANGE ENTRY
	
	/**
	 * Returns all the CustomRangeEntry available into the custom range entry folder of the project
	 * @return
	 */
	public Collection<CustomRangeEntry> getAllCustomRangeEntries() {
		return creMap.values();
	}
	
	/**
	 * Returns the CustomRangeEntry with the given ID
	 * @param crEntryId
	 * @return
	 */
	public CustomRangeEntry getCustomRangeEntryById(String crEntryId){
		return creMap.get(crEntryId);
	}
	
	/**
	 * Returns all the CustomRangeEntry for the given property. If the property has not a CustomRange
	 * associated, then returns an empty collection
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntry> getCustomRangeEntriesForProperty(String propertyUri){
		Collection<CustomRangeEntry> creColl = new ArrayList<CustomRangeEntry>();
		for (CustomRange cr : getCustomRangesForProperty(propertyUri)) { //iterate over the CR of the given property
			for (CustomRangeEntry cre : cr.getEntries()) { //iterate over the CRE of a single CR
				//check if the entry is already added in the collection to return
				boolean alreadyIn = false;
				for (CustomRangeEntry e : creColl) {
					if (e.getId().equals(cre.getId())) {
						alreadyIn = true;
						break;
					}
				}
				//add the entry only if is not already added (eventually from another CR)
				if (!alreadyIn) {
					creColl.add(cre);
				}
			}
		}
		return creColl;
	}
	
	/**
	 * Returns all the CustomRangeEntryGraph for the given property
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntryGraph> getCustomRangeEntriesGraphForProperty(String propertyUri){
		Collection<CustomRangeEntryGraph> creColl = new ArrayList<CustomRangeEntryGraph>();
		for (CustomRange cr : getCustomRangesForProperty(propertyUri)) { //iterate over the CR of the given property
			for (CustomRangeEntry cre : cr.getEntries()) { //iterate over the CRE of a single CR
				if (cre.isTypeGraph()) {
					//check if the entry is already added in the collection to return
					boolean alreadyIn = false;
					for (CustomRangeEntryGraph e : creColl) {
						if (e.getId().equals(cre.getId())) {
							alreadyIn = true;
							break;
						}
					}
					//add the entry only if is not already added (eventually from another CR)
					if (!alreadyIn) {
						creColl.add(cre.asCustomRangeEntryGraph());
					}
				}
			}
		}
		return creColl;
		
	}
	
	/**
	 * Returns all the CustomRangeEntryNode for the given property
	 * @param propertyUri
	 * @return
	 */
	public Collection<CustomRangeEntryNode> getCustomRangeEntriesNodeForProperty(String propertyUri){
		Collection<CustomRangeEntryNode> creColl = new ArrayList<CustomRangeEntryNode>();
		for (CustomRange cr : getCustomRangesForProperty(propertyUri)) { //iterate over the CR of the given property
			for (CustomRangeEntry cre : cr.getEntries()) { //iterate over the CRE of a single CR
				if (cre.isTypeNode()) {
					//check if the entry is already added in the collection to return
					boolean alreadyIn = false;
					for (CustomRangeEntryNode e : creColl) {
						if (e.getId().equals(cre.getId())) {
							alreadyIn = true;
							break;
						}
					}
					//add the entry only if is not already added (eventually from another CR)
					if (!alreadyIn) {
						creColl.add(cre.asCustomRangeEntryNode());
					}
				}
			}
		}
		return creColl;
	}
	
	/**
	 * Tells whether a property has or not a CustomRangeEntry of type graph 
	 * @param propertyUri
	 * @return
	 */
	public boolean existsCustomRangeEntryGraphForProperty(String propertyUri){
		return (!getCustomRangeEntriesGraphForProperty(propertyUri).isEmpty());
	}
	
	/* ##################
	 * ##### CREATE #####
	 * ################## */
	
	// CUSTOM RANGE
	
	public CustomRange createCustomRange(String id) throws CustomRangeException {
		//if already exists throw an exception
		if (this.getCustomRangeById(id) != null) {
			throw new CustomRangeException("Impossible to create a CustomRange with "
					+ "ID '" + id + "'. A CustomRange with the same ID already exists");
		} else { //otherwise create the CR
			CustomRange cr = new CustomRange(id);
			cr.saveXML(); //serialize it
			crMap.put(id, cr); //and add it to the map
			return cr;
		}
	}
	
	// CUSTOM RANGE ENTRY
	
	public CustomRangeEntry createCustomRangeEntry(String type, String id, String name, String description, String ref, String showProp) throws CustomRangeException {
		//if already exists throw an exception
		if (this.getCustomRangeEntryById(id) != null){
			throw new CustomRangeException("Impossible to create a CustomRangeEntry with "
					+ "ID '" + id + "'. A CustomRangeEntry with the same ID already exists");
		} else { //otherwise create the CRE
			CustomRangeEntry cre = null;
			if (type.equalsIgnoreCase(CustomRangeEntry.Types.node.toString())){
				cre = new CustomRangeEntryNode(id, name, description, ref);
			} else {
				cre = new CustomRangeEntryGraph(id, name, description, ref);
				if (showProp != null && !showProp.equals("")) {
					cre.asCustomRangeEntryGraph().setShowProperty(showProp);
				}
			}
			cre.saveXML(); //serialize it
			creMap.put(id, cre); //and add it to the map
			return cre;
		}
	}
	
	/* ##################
	 * ##### UPDATE #####
	 * ################## */
	
	// CUSTOM RANGE
	
	public void addCustomRangeToProperty(String crId, ARTURIResource property, boolean replaceRanges) throws CustomRangeException {
		CustomRange cr = this.getCustomRangeById(crId);
		if (cr == null) {
			throw new CustomRangeException("CustomRange with ID " + crId + " doesn't exist");
		}
		CustomRangeConfigEntry crcEntry = new CustomRangeConfigEntry(property.getURI(), cr, replaceRanges);
		if (crConfig.addEntry(crcEntry)) {
			crConfig.saveXML(); //save crConfig file only if the entry did not exist already
		}
	}
	
	public void removeCustomRangeFromProperty(String crId, ARTURIResource property) throws CustomRangeException {
		if (this.getCustomRangeById(crId) == null) {
			throw new CustomRangeException("CustomRange with ID " + crId + " doesn't exist");
		}
		if (crConfig.removeConfigEntryFromProperty(crId, property.getURI())) {
			crConfig.saveXML(); //save crConfig file only if the entry exists and is removed
		}
	}
	
	
	/**
	 * Removes a CustomRange from the CR collection and its file from file-system
	 * @param crId
	 * @throws CustomRangeException 
	 */
	public void deleteCustomRange(String crId) throws CustomRangeException{
		if (this.getCustomRangeById(crId) == null) {
			throw new CustomRangeException("CustomRange with ID " + crId + " doesn't exist");
		} else {
			crMap.remove(crId); //remove cr from map
			//delete the file
			File[] crFiles = getCustomRangeFolder().listFiles();
			for (File f : crFiles){//search for the custom range file with the given id/name
				if (f.getName().equals(crId+".xml")){
					f.delete();
					return;
				}
			}
			//remove ConfigEntries about the given CustomRange
			crConfig.removeConfigEntryWithCrId(crId);
			crConfig.saveXML();
		}
	}
	
	// CUSTOM RANGE ENTRY
	
	public void updateCustomRangeEntry(String id, String name, String description, String ref, @Optional String showProp) throws CustomRangeException{
		CustomRangeEntry cre = this.getCustomRangeEntryById(id);
		if (cre == null) {
			throw new CustomRangeException("CustomRangeEntry with ID " + id + " doesn't exist");
		} else {
			cre.setName(name);
			cre.setDescription(description);
			cre.setRef(ref);
			if (showProp != null){
				cre.asCustomRangeEntryGraph().setShowProperty(showProp);
			}
			cre.saveXML();
		}
	}
	
	/**
	 * Removes a CustomRangeEntry from the CRE collection and its file from file-system
	 * @param creId
	 * @param deleteEmptyCr if true deletes CustomRange that are left empty after the deletion
	 * @throws CustomRangeException 
	 */
	public void deleteCustomRangeEntry(String creId, boolean deleteEmptyCr) throws CustomRangeException{
		if (this.getCustomRangeEntryById(creId) == null) {
			throw new CustomRangeException("CustomRangeEntry with ID " + creId + " doesn't exist");
		} else {
			creMap.remove(creId); //remove the CRE from the map
			//delete the CRE file
			File[] creFiles = getCustomRangeEntryFolder().listFiles();
			for (File f : creFiles){//search for the custom range entry file with the given id/name
				if (f.getName().equals(creId+".xml")){
					f.delete();
					return;
				}
			}
			//remove the entry from the CustomRange(s) that use it
			Collection<CustomRange> crColl = this.getAllCustomRanges();
			for (CustomRange cr : crColl){
				if (cr.containsEntry(creId)){
					cr.removeEntry(creId);
					if (deleteEmptyCr && cr.getEntries().isEmpty()) { //if deleteEmptyCr is true and CR is left empty, delete it
						this.deleteCustomRange(cr.getId());
					} else { //otherwise save it
						cr.saveXML(); 
					}
				}
			}
		}
	}
	
	public void addEntryToCustomRange(String crId, String creId) throws CustomRangeException {
		CustomRange cr = this.getCustomRangeById(crId);
		if (cr == null) {
			throw new CustomRangeException("CustomRange with ID " + crId + " doesn't exist");
		}
		CustomRangeEntry cre = this.getCustomRangeEntryById(creId);
		if (cre == null) {
			throw new CustomRangeException("CustomRangeEntry with ID " + creId + " doesn't exist");
		}
		cr.addEntry(cre);
		cr.saveXML();
	}
	
	public void removeEntryFromCustomRange(String crId, String creId) throws CustomRangeException {
		CustomRange cr = this.getCustomRangeById(crId);
		if (cr == null) {
			throw new CustomRangeException("CustomRange with ID " + crId + " doesn't exist");
		}
		if (this.getCustomRangeEntryById(creId) == null) {
			throw new CustomRangeException("CustomRangeEntry with ID " + creId + " doesn't exist");
		}
		cr.removeEntry(creId);
		cr.saveXML();
	}

	/* #############################################
	 * ################# Utility ###################
	 * ############################################*/
	
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
