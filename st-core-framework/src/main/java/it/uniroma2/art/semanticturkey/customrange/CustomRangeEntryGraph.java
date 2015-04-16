package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.DependencyException;
import it.uniroma2.art.coda.exception.PRParserException;
import it.uniroma2.art.coda.pearl.model.GraphElement;
import it.uniroma2.art.coda.pearl.model.GraphStruct;
import it.uniroma2.art.coda.pearl.model.OptionalGraphStruct;
import it.uniroma2.art.coda.pearl.model.PlaceholderStruct;
import it.uniroma2.art.coda.pearl.model.ProjectionRule;
import it.uniroma2.art.coda.pearl.model.ProjectionRulesModel;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemUri;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElement;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
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

public class CustomRangeEntryGraph extends CustomRangeEntry {
	
	private static final String USER_PROMPT_FEATURE_NAME = "userPrompt";
	private static final String USER_PROMPT_TYPE_PATH = "it.uniroma2.art.semanticturkey.userPromptFS";
	private String annotationTypeName;//UIMA type taken from pearl rule (rule ....)
	
	CustomRangeEntryGraph(String id, String name, String description, String ref) {
		super(id, name, description, ref);
	}
	
	/**
	 * Returns a list (without duplicates) of predicates contained in the pearl of the CustomRangeEntryGraph.
	 * @param codaCore an instance of CODACore already initialized, used to parse and retrieve
	 * necessary information from PEARL code.
	 * @param onlyMandatory <code>false</code> returns all the predicate; <code>true</code> returns
	 * just the ones non-optional
	 * @param onlyShowable <code>false</code> returns all the predicate; <code>true</code> returns
	 * just the ones to show to the user.
	 * tells if the returned collection should contain all the predicate or 
	 * TODO: onlyShowable parameter will be useful when the CRE will provide further information to
	 * know whether or not a predicate-object has to be shown to the user in the UI (through pearl 
	 * annotation or other attribute in CRE xml)
	 * @return
	 * @throws PRParserException 
	 */
	public Collection<String> getGraphPredicates(CODACore codaCore, boolean onlyMandatory, boolean onlyShowable) throws PRParserException{
		Collection<String> predicates = new ArrayList<String>();
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModel(pearlStream);
		Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
		Set<String> prRuleIds = prRuleMap.keySet();
		for (String prId : prRuleIds){
			ProjectionRule projRule = prRuleMap.get(prId);
			//get the graph section
			Collection<GraphElement> graphList = projRule.getGraphList();
			//it's supposed that the graph entry is the subject of the first graphElement (that is not optional)
			String graphEntry = graphList.iterator().next().asGraphStruct().getSubject().getValueAsString();
			for (GraphElement g : graphList) {
				if (g.isGraphStruct()){
					GraphStruct gs = g.asGraphStruct();
					if (gs.getSubject().getValueAsString().equals(graphEntry)){
						GraphSingleElement predGraphElem = gs.getPredicate();
						if (predGraphElem instanceof GraphSingleElemUri){
							String pred = ((GraphSingleElemUri) predGraphElem).getURI();
							if (!predicates.contains(pred)){//prevent duplicates
								predicates.add(pred);
							}
						}
					}
				} else { //g.isOptionalGraphStruct
					if (!onlyMandatory){
						OptionalGraphStruct ogs = g.asOptionalGraphStruct();
						Collection<GraphElement> optionalGraphList = ogs.getOptionalTriples();
						for (GraphElement otpG : optionalGraphList) {
							if (otpG.isGraphStruct()){
								GraphStruct gs = otpG.asGraphStruct();
								if (gs.getSubject().getValueAsString().equals(graphEntry)){
									GraphSingleElement predGraphElem = gs.getPredicate();
									if (predGraphElem instanceof GraphSingleElemUri){
										String pred = ((GraphSingleElemUri) predGraphElem).getURI();
										if (!predicates.contains(pred)){//prevent duplicates
											predicates.add(pred);
										}
									}
								}
							} //2nd level optional graph are not considered.
						}
					}
				}
			}
		}
		return predicates;
	}

	@Override
	public Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException {
		Collection<UserPromptStruct> form = new ArrayList<UserPromptStruct>();
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
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
						String rdfType = placeHolderStruct.getRDFType();
						UserPromptStruct upStruct = new UserPromptStruct(userPromptField, rdfType);
						//fill the UserPromptStruct independently from its type (literal or uri)
						upStruct.setLiteralDatatype(placeHolderStruct.getLiteralDatatype());
						upStruct.setLiteralLang(placeHolderStruct.getLiteralLang());
						upStruct.setConverter(placeHolderStruct.getConverterList().get(0));//for now I suppesed there is used only one converter
						upStruct.setMandatory(placeHolderStruct.isMandatoryInGraphSection());
						form.add(upStruct);
					}
				}
			}
		}
		return form;
	} 

	/**
	 * Fills a CAS with the value specified in the given userPromptMap, then executes CODA with the 
	 * CAS, generates the triples and returns them.
	 * @param userPromptMap map containing userPrompt-value pairs, where userPrompt is a feature name
	 * (the same indicated in the pearl userPrompt/...) and value is the value given by user.
	 * @param codaCore an instance of CODACore already initialized
	 * @return 
	 * @throws CODAException 
	 */
	public List<ARTTriple> executePearl(CODACore codaCore, Map<String, String> userPromptMap) throws CODAException{
		List<ARTTriple> triples = null;
		try{
			TypeSystemDescription tsd = createTypeSystemDescription(codaCore);
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
//			analyseCas(aCAS);
			//run coda with the given pearl and the cas just created.
			System.out.println("pearl:\t" + getRef());
			InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
			codaCore.setProjectionRulesModel(pearlStream);
			codaCore.setJCas(jcas);
			while (codaCore.isAnotherAnnotationPresent()){
				SuggOntologyCoda suggOntCoda = codaCore.processNextAnnotation();
				if (suggOntCoda.getAnnotation().getType().getName().startsWith("it.uniroma2")){//get only triples of rilevant annotations
					triples = suggOntCoda.getAllARTTriple();
				}
			}
		} catch (PRParserException | ComponentProvisioningException | ConverterException | 
				UnsupportedQueryLanguageException | ModelAccessException | MalformedQueryException | 
				QueryEvaluationException | DependencyException | UIMAException e) {
			throw new CODAException(e);
		}
		return triples;
	}
	
	/**
	 * Creates a TypeSystemDescription based on the CODA rule contained in the <code>ref</code> tag.
	 * The TypeSystemDescription returned contains a top feature structure named <code>userPrompt</code>
	 * structured following the node section of the CODA rule.
	 * @return
	 * @throws ResourceInitializationException
	 * @throws PRParserException
	 */
	private TypeSystemDescription createTypeSystemDescription(CODACore codaCore) throws ResourceInitializationException, PRParserException{
		TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
		//init the projection rules model with the pearl
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
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
//				System.out.println("placeHolderId: " + placeHolderId);
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
//		describeTSD(tsd);
		return tsd;
	}
	
	
	//For debug decomment in createTypeSystemDescription
	@SuppressWarnings("unused")
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

	//For debug decomment in executePearl
	@SuppressWarnings("unused")
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
