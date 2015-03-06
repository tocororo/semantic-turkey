package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.DependencyException;
import it.uniroma2.art.coda.exception.PRParserException;
import it.uniroma2.art.coda.osgi.bundle.CODAOSGiFactory;
import it.uniroma2.art.coda.pearl.model.PlaceholderStruct;
import it.uniroma2.art.coda.pearl.model.ProjectionRule;
import it.uniroma2.art.coda.pearl.model.ProjectionRulesModel;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.query.MalformedQueryException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CustomRangeCODAManager {
	
	private CODACore codaCore;
	
	private static final String USER_PROMPT_FEATURE_NAME = "userPrompt";
	private static final String USER_PROMPT_TYPE_PATH = "it.uniroma2.art.semanticturkey.userPromptFS";
	private String annotationTypeName;//UIMA type taken from pearl rule (rule ....)
	
	@Autowired
	public CustomRangeCODAManager(CODAOSGiFactory codaFactory, BundleContext context){
		this.codaCore = codaFactory.getInstance(context);
	}
	
	/**
	 * Parse the CODA rule contained in the <code>ref</code> tag and build a map of &ltuserPrompt, 
	 * type&gt pairs, where <code>userPrompt</code> is a field of the <code>userPrompt/</code>
	 * feature path and <code>type</code> is the converter used for that feature.
	 * Returns an empty map if the CustomRangeEntry type is <code>node</code>. Is supposed that
	 * should be checks to avoid to call this method in case of <code>node</code> CustomRangeEntry
	 * @param crEntry CustomRangeEntry containing pearl rule used to 
	 * @return
	 * @throws PRParserException
	 */
	public List<UserPromptStruct> getForm(CustomRangeEntry crEntry) throws PRParserException{
		List<UserPromptStruct> form = new ArrayList<UserPromptStruct>();
		if (crEntry.getType().equals("graph")){
			InputStream pearlStream = new ByteArrayInputStream(crEntry.getRef().getBytes(StandardCharsets.UTF_8));
			ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModel(pearlStream);
			Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
			Set<String> prRuleIds = prRuleMap.keySet();
			for (String prId : prRuleIds){
				ProjectionRule projRule = prRuleMap.get(prId);
				//get the nodes section
				Map<String, PlaceholderStruct> plHolderMap = projRule.getPlaceholderMap();
				Set<String> pHoldIds = plHolderMap.keySet();
				for (String phId : pHoldIds){
					PlaceholderStruct placeHolderStruct = plHolderMap.get(phId);
					if (placeHolderStruct.hasFeaturePath()){
						String featurePath = placeHolderStruct.getFeaturePath();
						if (featurePath.startsWith(USER_PROMPT_FEATURE_NAME+"/")){
							String userPromptField = featurePath.substring(USER_PROMPT_FEATURE_NAME.length()+1);
							String type = placeHolderStruct.getRDFType();
							UserPromptStruct upStruct = new UserPromptStruct(userPromptField, type);
							//fill the UserPromptStruct independently from its type (literal or uri)
							upStruct.setLiteralDatatype(placeHolderStruct.getLiteralDatatype());
							upStruct.setLiteralLang(placeHolderStruct.getLiteralLang());
//							String datatype = placeHolderStruct.getLiteralDatatype();
//							if (type.equals("literal") && datatype != null){
//								type = type + "^^" + datatype;
//							}
							form.add(upStruct);
						}
					}
				}
			}
		}
		return form;
	}
	
	/**
	 * Fills a CAS with the value specified in the given userPromptMap, then executes CODA with the CAS,
	 * generates the triples and add it to the model.
	 * @param rdfModel model where the triples generated by CODA will be added.
	 * @param modelFactory ModelFactory used by CODA
	 * @param userPromptMap map containing userPrompt-value pairs, where userPrompt is a feature name
	 * (the same indicated in the pearl userPrompt/...) and value is the value given by user.
	 * @param crEntry CustomRangeEntry containing pearl rule used to generate TSD and to generate triples
	 * @throws PRParserException 
	 * @throws UIMAException 
	 * @throws DependencyException 
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws ModelAccessException 
	 * @throws UnsupportedQueryLanguageException 
	 * @throws ConverterException 
	 * @throws ComponentProvisioningException 
	 */
	public void fillMapAndAddTriples(RDFModel rdfModel, ModelFactory<?> modelFactory, Map<String, String> userPromptMap, CustomRangeEntry crEntry)
			throws PRParserException, UIMAException, ComponentProvisioningException, 
			ConverterException, UnsupportedQueryLanguageException, ModelAccessException,
			MalformedQueryException, QueryEvaluationException, DependencyException {
		
		codaCore.initialize(rdfModel, modelFactory);

		TypeSystemDescription tsd = createTypeSystemDescription(crEntry);
		JCas jcas = JCasFactory.createJCas(tsd);//this jcas has the structure defined by the TSD (created following the pearl)
		CAS aCAS = jcas.getCas();
		TypeSystem ts = aCAS.getTypeSystem();
		
		//create an annotation named as the pearlRule (annotationTypeName is set with the pearl rule name in getTypeSystemDescription())
		Type annotationType = ts.getType(annotationTypeName);
		AnnotationFS ann = aCAS.createAnnotation(annotationType, 0, 0);
		
		//create a FS of type userPromptType and set its features with the value find in inputMap
		Type userPromptType = ts.getType(USER_PROMPT_TYPE_PATH);
		FeatureStructure userPromptFS = aCAS.createFS(userPromptType);
		//get the userPrompt features (userPrompt/...)
		List<Feature> featuresList = userPromptType.getFeatures();
		//fill the feature with the values specified in the inputMap
		for (Feature f : featuresList){
			if (f.getName().startsWith(USER_PROMPT_TYPE_PATH)){
				//get the value of the given feature from the map
				String userPromptName = f.getShortName();
				String userPromptValue = userPromptMap.get(userPromptName);
				//assign the value to the feature
				userPromptFS.setStringValue(f, userPromptValue);
			}
		}
		Feature userPromptFeature = annotationType.getFeatureByBaseName(USER_PROMPT_FEATURE_NAME);
		ann.setFeatureValue(userPromptFeature, userPromptFS);
		
		aCAS.addFsToIndexes(ann);
		
		analyseCas(aCAS);
		
		//run coda with the given pearl and the cas just created.
		System.out.println("pearl:\t" + crEntry.getRef());
		InputStream pearlStream = new ByteArrayInputStream(crEntry.getRef().getBytes(StandardCharsets.UTF_8));
		codaCore.setProjectionRulesModel(pearlStream);
		codaCore.setJCas(jcas);
		while (codaCore.isAnotherAnnotationPresent()){
			SuggOntologyCoda suggOntCoda = codaCore.processNextAnnotation();
			
			if (suggOntCoda.getAnnotation().getType().getName().startsWith("it.uniroma2")){//returns only rilevant annotations
				List<ARTTriple> triples = suggOntCoda.getAllARTTriple();
				for (ARTTriple triple : triples){
					System.out.println("s:\t" + triple.getSubject() + "\np:\t" + triple.getPredicate() + "\no:\t" + triple.getObject());
//					rdfModel.addTriple(triple.getSubject(), triple.getPredicate(), triple.getObject(), NodeFilters.MAINGRAPH);
				}
			}
		}
	}
	
	/**
	 * Creates a TypeSystemDescription based on the CODA rule contained in the <code>ref</code> tag.
	 * The TypeSystemDescription returned contains a top feature structure named <code>userPrompt</code>
	 * structured following the node section of the CODA rule.
	 * @param crEntry CustomRangeEntry containing pearl rule used to generate TSD
	 * @return
	 * @throws ResourceInitializationException
	 * @throws PRParserException
	 */
	private TypeSystemDescription createTypeSystemDescription(CustomRangeEntry crEntry) throws ResourceInitializationException, PRParserException{
		TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
		if (crEntry.getType().equals("graph")){
			//init the projection rules model with the pearl
			InputStream pearlStream = new ByteArrayInputStream(crEntry.getRef().getBytes(StandardCharsets.UTF_8));
			ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModel(pearlStream);
			Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
			Set<String> prRuleIds = prRuleMap.keySet();
			for (String prId : prRuleIds){
				ProjectionRule projRule = prRuleMap.get(prId);
				//create annotation (named as the feature path in the pearl)
				annotationTypeName = projRule.getUIMAType();//name of the annotation type get from the first line of pearl code (rule ....)
				TypeDescription annotationType = tsd.addType(annotationTypeName, "", CAS.TYPE_NAME_ANNOTATION); 
				//get the pearl nodes section
				Map<String, PlaceholderStruct> placeHolderMap = projRule.getPlaceholderMap();
				Set<String> placeHolderIds = placeHolderMap.keySet();
				//create an annotation (it...userPromptFS) which its structure is based on the value find in the userPrompt features
				TypeDescription userPromptType = tsd.addType(USER_PROMPT_TYPE_PATH, "", CAS.TYPE_NAME_TOP);
				//look for the userPrompt/... feature in PEARL code and add the related Features to the above annotation 
				for (String placeHolderId : placeHolderIds){
//					System.out.println("placeHolderId: " + placeHolderId);
					PlaceholderStruct placeHolderStruct = placeHolderMap.get(placeHolderId);
					if (placeHolderStruct.hasFeaturePath()){
						String featurePath = placeHolderStruct.getFeaturePath();
						if (featurePath.startsWith(USER_PROMPT_FEATURE_NAME+"/")){//add feature only for that featurePath that start with userPrompt/
							String prompt = featurePath.substring(USER_PROMPT_FEATURE_NAME.length()+1);
							userPromptType.addFeature(prompt, "", CAS.TYPE_NAME_STRING);
						}
					}
				}
				//finally add to the main annotation a feature named "userPrompt" of the type just created
				annotationType.addFeature(USER_PROMPT_FEATURE_NAME, "", userPromptType.getName());
			}
		}
		
		describeTSD(tsd);
		
		return tsd;
	}
	
	private void describeTSD(TypeSystemDescription tsd){
		System.out.println("================ TSD structure ================");
		TypeDescription[] types = tsd.getTypes();
		System.out.println("type list:");
		for (int i=0; i<types.length; i++){
			TypeDescription type = types[i];
			if (type.getName().startsWith("it.uniroma2.art.semanticturkey")){
				System.out.println("\nType: " + type.getName());
				FeatureDescription[] features = type.getFeatures();
				System.out.println("features:");
				for (int j=0; j<features.length; j++){
					FeatureDescription feature = features[j];
					System.out.println("\t" + feature.getName() + "\t" + feature.getRangeTypeName());
				}
			}
		}
		System.out.println("===============================================");
	}

	private void analyseCas(CAS aCAS){
		System.out.println("======== CAS ==========");
		AnnotationIndex<AnnotationFS> anIndex = aCAS.getAnnotationIndex();
		for (AnnotationFS an : anIndex){
			if (an.getType().getName().startsWith("it.uniroma")){//I want to explode only my annotation (ignore DocumentAnnotation)
				System.out.println("Annotation: " + an.getType().getName());
				Feature feature = an.getType().getFeatureByBaseName("userPrompt");
				System.out.println("\tFeature: " + feature.getName());
				FeatureStructure userPromptFS = an.getFeatureValue(feature);
				Type userPromptType = userPromptFS.getType();
				List<Feature> upFeatures = userPromptType.getFeatures();
				for (Feature upF : upFeatures){
					String upfValue = userPromptFS.getStringValue(upF);
					System.out.println("\t\tFeature: " + upF.getShortName() + "; value: " + upfValue);
				}
				
			}
		}
		System.out.println("=======================");
	}

}
