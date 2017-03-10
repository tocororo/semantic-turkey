package it.uniroma2.art.semanticturkey.customform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.coda.converters.contracts.ContractConstants;
import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.DependencyException;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ConverterPlaceholderArgument;
import it.uniroma2.art.coda.pearl.model.GraphElement;
import it.uniroma2.art.coda.pearl.model.GraphStruct;
import it.uniroma2.art.coda.pearl.model.OptionalGraphStruct;
import it.uniroma2.art.coda.pearl.model.PlaceholderStruct;
import it.uniroma2.art.coda.pearl.model.ProjectionRule;
import it.uniroma2.art.coda.pearl.model.ProjectionRulesModel;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemBNode;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemUri;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElement;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;

public class CustomFormGraph extends CustomForm {
	
	private static final String USER_PROMPT_FEATURE_NAME = "userPrompt";
	private static final String USER_PROMPT_TYPE_PATH = "it.uniroma2.art.semanticturkey.userPromptFS";
	private String annotationTypeName;//UIMA type taken from pearl rule (rule ....)
	private List<IRI> showPropertyChain;
	
	CustomFormGraph(String id, String name, String description, String ref, List<IRI> showPropChain) {
		super(id, name, description, ref);
		showPropertyChain = showPropChain;
	}
	
	/**
	 * Returns the property chain that suggests which of the property in the pearl determines the value to
	 * show instead of the URL
	 * @return
	 */
	public List<IRI> getShowPropertyChain(){
		return this.showPropertyChain;
	}
	
	public void setShowPropertyChain(List<IRI> propertyChain){
		this.showPropertyChain = propertyChain;
	}
	
	/**
	 * Returns a list (without duplicates) of predicates contained in the pearl of the {@link CustomFormGraph}.
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
	 * @throws RDFModelNotSetException 
	 * @throws ModelAccessException 
	 */
	public Collection<String> getGraphPredicates(CODACore codaCore, boolean onlyMandatory, boolean onlyShowable) 
			throws PRParserException, RDFModelNotSetException {
		Collection<String> predicates = new ArrayList<String>();
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModelAndParseIt(pearlStream);
		Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
		Set<String> prRuleIds = prRuleMap.keySet();
		for (String prId : prRuleIds){
			ProjectionRule projRule = prRuleMap.get(prId);
			//get the graph section
			Collection<GraphElement> graphList = projRule.getInsertGraphList();
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
	
	/**
	 * Returns a Collection of {@link UserPromptStruct} that is used to render a form
	 */
	@Override
	public Collection<UserPromptStruct> getForm(CODACore codaCore) throws PRParserException, 
			RDFModelNotSetException {
		Map<String, UserPromptStruct> formMap = new LinkedHashMap<>();
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModelAndParseIt(pearlStream);
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
						String placeholderId = placeHolderStruct.getName();
						String rdfType = placeHolderStruct.getRDFType();
						ConverterMention converter = placeHolderStruct.getConverterList().get(0);
						String converterArgPh = null;
						if (converter.getURI().equals(ContractConstants.CODA_CONTRACTS_BASE_URI + "langString")) {
							//there's no control of the proper use of coda:langString, is take for granted that it is used properly (coda:langString[$lang])
							//otherwise it can throw an unchecked exception
							converterArgPh = ((ConverterPlaceholderArgument) converter.getAdditionalArguments().get(0)).getPlaceholderId();
						}
						String literalLang = placeHolderStruct.getLiteralLang();
						String literalDatatype = placeHolderStruct.getLiteralDatatype();
						String userPromptField = featurePath.substring(USER_PROMPT_FEATURE_NAME.length()+1);
						/* Create a new UserPromptStruct only when for a new placeholder one of the
						 * following condition is true:
						 * - The feature seed of the placeholder is not in any of the UserPromptStruct already created
						 * - The coda:langSting converter (if used) depends from a language placeholder never used in the UPS already created
						 * - The type is literal and language is never used in the UPS already created
						 * To perform these checks create a String id for every UPS based on the above values
						 */
						String ID_SEPARATOR = "$";
						String upsId = userPromptField + ID_SEPARATOR + converterArgPh + ID_SEPARATOR + literalLang;
						boolean alreadyInForm = false;
						for (Entry<String, UserPromptStruct> formMapEntry : formMap.entrySet()){
							if (formMapEntry.getKey().equals(upsId)){
								alreadyInForm = true;
								//update the mandatory attribute with the OR between the mandatory of the current phStruct and the one of UPS already in form
								UserPromptStruct upsAlreadyIn = formMapEntry.getValue();
								upsAlreadyIn.setMandatory(upsAlreadyIn.isMandatory() || placeHolderStruct.isMandatoryInGraphSection());
								//if the already in form is uri and the new one is literal, prioritize the literal one, so remove the uri ups
								if (upsAlreadyIn.getRdfType().equals("uri") && rdfType.equals("literal")) {
									formMap.remove(upsId);
									alreadyInForm = false;//cause the old UPS has been removed, so it allows to add the new one
								}
								break;
							}
						}
						if (!alreadyInForm) {
							UserPromptStruct upStruct = new UserPromptStruct(placeholderId, userPromptField, rdfType);
							//fill the UserPromptStruct independently from its type (literal or uri)
							upStruct.setLiteralDatatype(literalDatatype);
							upStruct.setLiteralLang(literalLang);
							upStruct.setConverter(converter);//for now I suppese there is used only one converter
							upStruct.setConverterArgPhId(converterArgPh);
							upStruct.setMandatory(placeHolderStruct.isMandatoryInGraphSection());
							formMap.put(upsId, upStruct);
						}
						
					}
				}
			}
		}
		return formMap.values();
	}
	
	/**
	 * Returns a map placeholderId-userPrompt used in the node section to identify userPrompt fields.
	 * Returns only those placeholder not used as argument of some converter (as for langString argument) 
	 * @param codaCore
	 * @return
	 * @throws PRParserException 
	 */
	public Map<String, String> getRelevantFormPlaceholders(CODACore codaCore) throws PRParserException {
		Map<String, String> phPromptMap = new HashMap<String, String>(); //placeholderId-userPrompt
		//crete the PEARL model
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModelAndParseIt(pearlStream);
		Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
		Set<String> prRuleIds = prRuleMap.keySet();
		//here iterate over the rules of the pearl. This is not necessary since a CRE has only one rule (I could get directly the first ruleId)
		for (String prId : prRuleIds){ 
			ProjectionRule projRule = prRuleMap.get(prId);
			//get the nodes section
			Map<String, PlaceholderStruct> plHolderMap = projRule.getPlaceholderMap();
			Set<String> pHoldIds = plHolderMap.keySet();
			for (String phId : pHoldIds){ //iterate over the placeholder structs (placeholder-converter-featPath) of the node section
				PlaceholderStruct placeHolderStruct = plHolderMap.get(phId);
				if (placeHolderStruct.hasFeaturePath()){
					String featurePath = placeHolderStruct.getFeaturePath();
					if (featurePath.startsWith(USER_PROMPT_FEATURE_NAME+"/")){
						//add the phId and userPrompt (eventual placeholder used as argument of langString will be removed later)
						phPromptMap.put(phId, featurePath.substring(USER_PROMPT_FEATURE_NAME.length()+1));
						
						ConverterMention converter = placeHolderStruct.getConverterList().get(0);
						//if langString is used, remove the placeholder argument from the placeholder to return;
						if (converter.getURI().equals(ContractConstants.CODA_CONTRACTS_BASE_URI + "langString")) {
							//there's no control of the proper use of coda:langString, is take for granted that it is used properly (coda:langString[$lang])
							//otherwise it can throw an unchecked exception
							String convArgPhId = ((ConverterPlaceholderArgument) converter.getAdditionalArguments().get(0)).getPlaceholderId();
							phPromptMap.remove(convArgPhId);
						}
					}
				}
			}
		}
		return phPromptMap;
	}
	
	/**
	 * Returns the placeholder of the entry point of the graph section
	 * @param codaCore
	 * @return
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 */
	public String getEntryPointPlaceholder(CODACore codaCore) throws PRParserException, RDFModelNotSetException {
		String entryPoint = "";
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModelAndParseIt(pearlStream);
		Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
		Iterator<ProjectionRule> prRulesIt = prRuleMap.values().iterator();
		if (prRulesIt.hasNext()) {
			ProjectionRule projRule = prRulesIt.next();
			Iterator<GraphElement> graphListIt = projRule.getInsertGraphList().iterator();
			if (graphListIt.hasNext()) {
				entryPoint = graphListIt.next().asGraphStruct().getSubject().getValueAsString();
			}
		}
		return entryPoint;
	}
	
	/**
	 * Returns the serialization of the graph section of the PEARL in the current CustomForm.
	 * @param codaCore
	 * @param optional specify if the <code>OPTIONAL {}</code> has to be serialized. Note that
	 * if is set to false, it doesn't mean that the content of the OPTIONAL is omitted, but only
	 * that the keyword <code>OPTIONAL</code> and the curly braces <code>{}</code> are omitted. 
	 * @return
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 */
	public String getGraphSectionAsString(CODACore codaCore, boolean optional) throws PRParserException, RDFModelNotSetException {
		StringBuilder sb = new StringBuilder();
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));

		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModelAndParseIt(pearlStream);
		Map<String, ProjectionRule> prRuleMap = prRuleModel.getProjRule();
		Iterator<ProjectionRule> prRulesIt = prRuleMap.values().iterator();
		if (prRulesIt.hasNext()) {
			ProjectionRule projRule = prRulesIt.next();
			serializeGraphList(projRule.getInsertGraphList(), sb, optional);
		}
		return sb.toString();
	}
	
	/**
	 * Serializes (recursively) a GraphElement collection
	 * @param graphList
	 * @param sb
	 * @param opt
	 */
	private void serializeGraphList(Collection<GraphElement> graphList, StringBuilder sb, boolean opt) {
		Set<String> bnodeVariables = opt ? new HashSet<>() : null;
		for (GraphElement graphElem : graphList) {
			if (!graphElem.isOptionalGraphStruct()) {
				GraphStruct gs = graphElem.asGraphStruct();
				sb.append(getSingleValueAsString(gs.getSubject(), bnodeVariables) + " "
						+ getSingleValueAsString(gs.getPredicate(), bnodeVariables) + " "
						+ getSingleValueAsString(gs.getObject(), bnodeVariables) + " . ");
			} else { // Optional
				if (opt)
					sb.append("OPTIONAL { ");
				OptionalGraphStruct optGS = graphElem.asOptionalGraphStruct();
				// call serializeGraphList recursively
				serializeGraphList(optGS.getOptionalTriples(), sb, opt);
				if (opt)
					sb.append("} ");
			}
		}
		if (bnodeVariables != null && !bnodeVariables.isEmpty()) {
			sb.append("FILTER(");
			sb.append(bnodeVariables.stream().map(v -> "isBLANK(" + v + ")").collect(Collectors.joining(" && ")));
			sb.append(")");
		}
	}
	
	/**
	 * Returns the serialization of a {@link GraphSingleElement}. If the given element is a
	 * {@link GraphSingleElemBNode}, then adds the corresponding variabile to <code>bnodeVariables</code> (if
	 * not <code>null</code>)
	 * 
	 * @param elem
	 * @param bnodeVariables
	 * @return
	 */
	private String getSingleValueAsString(GraphSingleElement elem, Set<String> bnodeVariables) {
		if (elem instanceof GraphSingleElemBNode) {
			String genVar = "?bnodeVar_" + ((GraphSingleElemBNode) elem).getBnodeIdentifier();
			if (bnodeVariables != null) {
				bnodeVariables.add(genVar);
			}
			return genVar;
		} else {
			return elem.getValueAsString();
		}
	}
	
	/**
	 * Fills a CAS with the value specified in the given userPromptMap, then executes CODA with the 
	 * CAS, generates the triples and returns them.
	 * @param userPromptMap map containing userPrompt-value pairs, where userPrompt is a feature name
	 * (the same indicated in the pearl userPrompt/...) and value is the value given by user.
	 * @param codaCore an instance of CODACore already initialized
	 * @return 
	 * @throws CODAException 
	 * @throws UnassignableFeaturePathException 
	 * @throws ProjectionRuleModelNotSet 
	 */
	public List<ARTTriple> executePearl(CODACore codaCore, Map<String, String> userPromptMap)
			throws CODAException, ProjectionRuleModelNotSet, UnassignableFeaturePathException {
		List<ARTTriple> triples = null;
		try {
			TypeSystemDescription tsd = createTypeSystemDescription(codaCore);
			JCas jcas = JCasFactory.createJCas(tsd);// this jcas has the structure defined by the TSD (created
													// following the pearl)
			CAS aCAS = jcas.getCas();
			TypeSystem ts = aCAS.getTypeSystem();
			// create an annotation named as the pearlRule (annotationTypeName is set with the pearl rule name
			// in getTypeSystemDescription())
			Type annotationType = ts.getType(annotationTypeName);
			AnnotationFS ann = aCAS.createAnnotation(annotationType, 0, 0);
			// create a FS of type userPromptType and set its features with the value find in inputMap
			Type userPromptType = ts.getType(USER_PROMPT_TYPE_PATH);
			FeatureStructure userPromptFS = aCAS.createFS(userPromptType);
			// get the userPrompt features (userPrompt/...)
			List<Feature> featuresList = userPromptType.getFeatures();
			// fill the feature with the values specified in the inputMap
			for (Feature f : featuresList) {
				if (f.getName().startsWith(USER_PROMPT_TYPE_PATH)) {
					// get the value of the given feature from the map
					String userPromptName = f.getShortName();
					String userPromptValue = userPromptMap.get(userPromptName);
					// assign the value to the feature
					userPromptFS.setStringValue(f, userPromptValue);
				}
			}
			Feature userPromptFeature = annotationType.getFeatureByBaseName(USER_PROMPT_FEATURE_NAME);
			ann.setFeatureValue(userPromptFeature, userPromptFS);
			aCAS.addFsToIndexes(ann);
			// analyseCas(aCAS);
			// run coda with the given pearl and the cas just created.
			// System.out.println("pearl:\t" + getRef());
			InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
			codaCore.setProjectionRulesModelAndParseIt(pearlStream);
			codaCore.setJCas(jcas);
			while (codaCore.isAnotherAnnotationPresent()) {
				SuggOntologyCoda suggOntCoda = codaCore.processNextAnnotation();
				// get only triples of relevant annotations (those triples that start with it.uniroma2.
				if (suggOntCoda.getAnnotation().getType().getName().startsWith("it.uniroma2")) {
					triples = suggOntCoda.getAllInsertARTTriple();
				}
			}
		} catch (PRParserException | ComponentProvisioningException | ConverterException
				| DependencyException | UIMAException | RDFModelNotSetException e) {
			throw new CODAException(e);
		}
		return triples;
	}
	
	/**
	 * Returns the property chain serialized as list of URIs separated by a comma.
	 * Return an empty string if the chain is empty
	 * @return
	 */
	public String serializePropertyChain() {
		String serializedPropChain = "";
		if (this.showPropertyChain.size() > 0){
			for (int i = 0; i < showPropertyChain.size(); i++) {
				serializedPropChain += showPropertyChain.get(i).stringValue() + ",";
			}
			serializedPropChain = serializedPropChain.substring(0, serializedPropChain.length()-1); //remove last ","
		}
		return serializedPropChain;
	}
	
	/**
	 * Creates a TypeSystemDescription based on the CODA rule contained in the <code>ref</code> tag.
	 * The TypeSystemDescription returned contains a top feature structure named <code>userPrompt</code>
	 * structured following the node section of the CODA rule.
	 * @return
	 * @throws ResourceInitializationException
	 * @throws PRParserException
	 * @throws RDFModelNotSetException 
	 * @throws ModelAccessException 
	 */
	private TypeSystemDescription createTypeSystemDescription(CODACore codaCore) 
			throws ResourceInitializationException, PRParserException, RDFModelNotSetException {
		TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
		//init the projection rules model with the pearl
		InputStream pearlStream = new ByteArrayInputStream(getRef().getBytes(StandardCharsets.UTF_8));
		ProjectionRulesModel prRuleModel = codaCore.setProjectionRulesModelAndParseIt(pearlStream);
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
