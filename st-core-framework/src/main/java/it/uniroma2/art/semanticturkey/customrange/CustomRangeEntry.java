package it.uniroma2.art.semanticturkey.customrange;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.PRParserException;
import it.uniroma2.art.coda.pearl.model.PlaceholderStruct;
import it.uniroma2.art.coda.pearl.model.ProjectionRule;
import it.uniroma2.art.coda.pearl.model.ProjectionRulesModel;
import it.uniroma2.art.owlart.model.ARTNode;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

public class CustomRangeEntry {
	
	private CODACore codaCore; //TODO da ottenere in qualche modo
	
	private static final String USER_PROMPT_FS_NAME = "userPrompt";
	private static final String USER_PROMPT_TYPE_PATH = "it.uniroma2.art.customFS";
	
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
		this.id = customRangeEntryId;
		File creFolder = CustomRangeProvider.getCustomRangeEntryFolder();
		File[] creFiles = creFolder.listFiles();//get list of files into custom range entry folder
		for (File f : creFiles){//search for the custom range entry file with the given id/name
			if (f.getName().equals(customRangeEntryId+".xml")){
				CustomRangeEntryXMLReader creReader = new CustomRangeEntryXMLReader(f);
				this.name = creReader.getName();
				this.description = creReader.getDescription();
				this.type = creReader.getType();
				this.ref = creReader.getRef();
				break; //stop looking for the custom range entry file
			}
		}
		throw new FileNotFoundException("CustomRangeEntry file '" + customRangeEntryId + ".xml' cannot be found in the CustomRangeEntry folder");
	}
	
	/**
	 * Constructor that given the CustomRangeEntry file loads its content
	 * @param customRangeEntryFile
	 */
	public CustomRangeEntry(File customRangeEntryFile){
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
	public String getID(){
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
	
	/**
	 * Creates a TypeSystemDescription based on the CODA rule contained in the <code>ref</code> tag.
	 * The TypeSystemDescription returned contains a top feature structure named <code>userPrompt</code>
	 * structured following the node section of the CODA rule.
	 * @return
	 * @throws ResourceInitializationException
	 * @throws PRParserException
	 */
	public TypeSystemDescription getTypeSystemDescription() throws ResourceInitializationException, PRParserException{
		TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
		InputStream stream = new ByteArrayInputStream(ref.getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModel(stream);
		Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
		Set<String> prRuleIds = prRuleMap.keySet();
		for (String prId : prRuleIds){
			ProjectionRule projRule = prRuleMap.get(prId);
			//create annotation (named as the feature path in the pearl)
			String annotationTypeName = projRule.getUIMAType();//name of the annotation type get from the first line of pearl code (rule ....)
			tsd.addType(annotationTypeName, "", CAS.TYPE_NAME_ANNOTATION);
			TypeDescription annotation = tsd.getType(annotationTypeName);
			//get the nodes section
			Map<String, PlaceholderStruct> plHolderMap = projRule.getPlaceholderMap();
			Set<String> pHoldIds = plHolderMap.keySet();
			//create annotation type defining the structure of the userPrompt FS
			tsd.addType(USER_PROMPT_TYPE_PATH, "", CAS.TYPE_NAME_TOP);
			TypeDescription userPromptFSType = tsd.getType(USER_PROMPT_TYPE_PATH);
			//add the FS(s) to the annotation, based on the value find in the userPrompt annotation (in the node section of the pearl, e.g. userPrompt/<xyz>) 
			for (String phId : pHoldIds){
				PlaceholderStruct placeHolderStruct = plHolderMap.get(phId);
				String featurePath = placeHolderStruct.getFeaturePath();
				if (featurePath.startsWith(USER_PROMPT_FS_NAME+"/")){
					String prompt = featurePath.substring(USER_PROMPT_FS_NAME.length()+1);
					userPromptFSType.addFeature(prompt, "", CAS.TYPE_NAME_STRING);
				}
			}
			//create FS userPrompt and assign the structure defined by userPromptFSType
			annotation.addFeature(USER_PROMPT_FS_NAME, "", userPromptFSType.getName());
		}
		return tsd;
	}
	
	/**
	 * Parse the CODA rule contained in the <code>ref</code> tag and build a map of &ltuserPrompt, 
	 * type&gt pairs, where <code>userPrompt</code> is a field of the <code>userPrompt/</code>
	 * feature path and <code>type</code> is the converter used for that feature  
	 * @return
	 * @throws PRParserException
	 */
	public Map<String, String> getFormMap() throws PRParserException{
		Map<String, String> formMap = new HashMap<String, String>();
		InputStream stream = new ByteArrayInputStream(ref.getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModel(stream);
		Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
		Set<String> prRuleIds = prRuleMap.keySet();
		for (String prId : prRuleIds){
			ProjectionRule projRule = prRuleMap.get(prId);
			//get the nodes section
			Map<String, PlaceholderStruct> plHolderMap = projRule.getPlaceholderMap();
			Set<String> pHoldIds = plHolderMap.keySet();
			for (String phId : pHoldIds){
				PlaceholderStruct placeHolderStruct = plHolderMap.get(phId);
				String featurePath = placeHolderStruct.getFeaturePath();
				if (featurePath.startsWith(USER_PROMPT_FS_NAME+"/")){
					String userPromptField = featurePath.substring(USER_PROMPT_FS_NAME.length()+1);
					String type = placeHolderStruct.getRDFType();
					formMap.put(userPromptField, type);
				}
			}
		}
		return formMap;
	}
	
	/**
	 * Returns <code>true</code> if the CustomRangeEntry is compatible with the given object.
	 * Is used by the resource view to know if a triple matches a CustomRangeEntry (e.g. a literal as
	 * object of skos:definition would not match the reified skos:definition pattern)
	 * @param object
	 * @return
	 */
	public boolean matches(ARTNode object){
		return false; //TODO: ??
	}
	
}
