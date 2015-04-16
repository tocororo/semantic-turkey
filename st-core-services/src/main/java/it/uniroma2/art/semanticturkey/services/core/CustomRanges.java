package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.PRParserException;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.model.ARTLiteral;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.ModelFactory;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.models.conf.ModelConfiguration;
import it.uniroma2.art.owlart.navigation.ARTNodeIterator;
import it.uniroma2.art.semanticturkey.customrange.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customrange.CustomRange;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeConfig;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeConfigEntry;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntry;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntryFactory;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntryGraph;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntryNode;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeFactory;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.customrange.UserPromptStruct;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.CustomRangeInitializationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.generation.annotation.RequestMethod;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

@GenerateSTServiceController
@Validated
@Component
public class CustomRanges extends STServiceAdapter {
	
	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;
	@Autowired
	private CustomRangeProvider crProvider;
	@Autowired
	private ServletRequest request;
	
	/**
	 * Returns all the property-CustomRange pairs defined in the CustomRange configuration
	 * @return
	 * @throws CustomRangeInitializationException 
	 */
	@GenerateSTServiceController
	public Response getCustomRangeConfigMap() throws CustomRangeInitializationException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Element crcElem = XMLHelp.newElement(dataElem, "customRangeConfig");
		Collection<CustomRangeConfigEntry> crConfEntries = crProvider.getCustomRangeConfig().getCustomRangeConfigEntries();
		for (CustomRangeConfigEntry crConfEntry : crConfEntries){
			Element confEntryElem = XMLHelp.newElement(crcElem, "configEntry");
			confEntryElem.setAttribute("property", crConfEntry.getProperty());
			confEntryElem.setAttribute("idCustomRange", crConfEntry.getCutomRange().getId());
			confEntryElem.setAttribute("replaceRanges", crConfEntry.getReplaceRange()+"");
		}
		return response;
	}
	
	/**
	 * Returns the serialization of the CurtomRangeEntry with the given id
	 * @param id
	 * @return
	 */
	@GenerateSTServiceController
	public Response getCustomRangeEntry(String id){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		CustomRangeEntry crEntry = crProvider.getCustomRangeEntryById(id);
		if (crEntry != null){
			Element creElem = XMLHelp.newElement(dataElem, "customRangeEntry");
			creElem.setAttribute("id", crEntry.getId());
			creElem.setAttribute("name", crEntry.getName());
			creElem.setAttribute("type", crEntry.getType());
			Element descriptionElem = XMLHelp.newElement(creElem, "description");
			descriptionElem.setTextContent(crEntry.getDescription());
			Element refElem = XMLHelp.newElement(creElem, "ref");
			refElem.setTextContent(crEntry.getRef());
		}
		return response;
	}
	
	/**
	 * Returns the serialization of all the CustomRangeEntry available for the given property
	 * @param property
	 * @return
	 * @throws ModelAccessException
	 */
	@GenerateSTServiceController
	public Response getCustomRangeEntries(String property) throws ModelAccessException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		Collection<CustomRangeEntry> crEntries = crProvider.getCustomRangeEntriesForProperty(getOWLModel().expandQName(property));
		for (CustomRangeEntry cre : crEntries){
			Element creElem = XMLHelp.newElement(dataElem, "customRangeEntry");
			creElem.setAttribute("id", cre.getId());
			creElem.setAttribute("name", cre.getName());
			creElem.setAttribute("type", cre.getType());
			Element descriptionElem = XMLHelp.newElement(creElem, "description");
			descriptionElem.setTextContent(cre.getDescription());
			Element refElem = XMLHelp.newElement(creElem, "ref");
			refElem.setTextContent(cre.getRef());
		}
		return response;
	}
	
	/**
	 * Returns the serialization of the CurtomRange with the given id
	 * This serialization doesn't contain the real range, it contains just the id(s) of the entries.
	 * @param id
	 * @return
	 */
	@GenerateSTServiceController
	public Response getCustomRange(String id){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		CustomRange cr = crProvider.getCustomRangeById(id);
		if (cr != null){
			Element crElem = XMLHelp.newElement(dataElem, "customRange");
			crElem.setAttribute("id", cr.getId());
			for (String creId : cr.getEntriesId()){
				Element entryElem = XMLHelp.newElement(crElem, "entry");
				entryElem.setAttribute("id", creId);
			}
		}
		return response;
	}
	
	/**
	 * This service get as parameters a custom range id and a set of userPrompt key-value pairs
	 * (userPrompt are unknown a priori, so pairs are dynamic and have to be get from the request
	 * TODO: find a better solution), then run CODA on the pearl specified in the CustomRangeEntry
	 * (with the given id <code>crEntryId</code>) and the features filled following the userPrompt parameters.
	 * Finally, "append" the triples generated by CODA to the subject-property pair
	 *  
	 * @param crEntryId
	 * @return
	 * @throws FileNotFoundException 
	 * @throws CODAException 
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws ModelUpdateException 
	 * @throws CustomRangeInitializationException 
	 */
	@SuppressWarnings("unchecked")
	@GenerateSTServiceController
	public Response runCoda(ARTResource subject, ARTURIResource predicate, String crEntryId) throws FileNotFoundException, CODAException, 
			UnavailableResourceException, ProjectInconsistentException, ModelUpdateException, CustomRangeInitializationException {
		//get the parameters to put in the userPromptMap from the request
		Map<String, String[]> parMap = request.getParameterMap();//the others params (form key and values) are dynamic, get it directly from request
		Map<String, String> userPromptMap = new HashMap<String, String>();
		for (Entry<String, String[]> par : parMap.entrySet()){
//			System.out.println("param: " + par.getKey() + ", value: " + par.getValue()[0]);
			userPromptMap.put(par.getKey(), par.getValue()[0]);
		}
		//Remove useless parameters for fillMapAndAddTriples method (parameters not belonging to userPrompt feature)
		/* N.B. if some other parameters will be put in this map, there should be no problem since
		 * when this map will be used to valorize the CAS, the value will be get based on the feature 
		 * in the TSD and the unnecessary params will be simply ignored */
		userPromptMap.remove("ctx_project");
		userPromptMap.remove("crEntryId");
		userPromptMap.remove("subject");
		userPromptMap.remove("predicate");
//		for (Entry<String, String> e : userPromptMap.entrySet()){
//			System.out.println("userPrompt: " + e.getKey() + " = " + e.getValue());
//		}
		try {
			ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
			RDFModel rdfModel = getOWLModel();
			CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
			codaCore.initialize(rdfModel, ontFact);
			CustomRangeEntry crEntry = crProvider.getCustomRangeEntryById(crEntryId);
			if (crEntry.isTypeGraph()){
				CustomRangeEntryGraph creGraph = crEntry.asCustomRangeEntryGraph();
				List<ARTTriple> triples = creGraph.executePearl(codaCore, userPromptMap);
				rdfModel.addTriple(subject, predicate, detectGraphEntry(triples), getWorkingGraph());
				for (ARTTriple triple : triples){
//					System.out.println("S:\t"+triple.getSubject()+"\nP:\t"+triple.getPredicate()+"\nO:\t"+triple.getObject());
					rdfModel.addTriple(triple.getSubject(), triple.getPredicate(), triple.getObject(), getWorkingGraph());
				}
			} else if (crEntry.isTypeNode()){
				CustomRangeEntryNode creNode = crEntry.asCustomRangeEntryNode();
				UserPromptStruct upStruct = creNode.getForm(codaCore).iterator().next();//if type=node, it's sure that there is only 1 form entry
				String value = userPromptMap.entrySet().iterator().next().getValue();//get the only value
				ARTNode artNode;
				if (upStruct.isLiteral()){
					artNode = codaCore.executeLiteralConverter(
							upStruct.getConverter(), value, upStruct.getLiteralDatatype(), upStruct.getLiteralLang());
				} else {//type "uri"
					artNode = codaCore.executeURIConverter(upStruct.getConverter(), value);
				}
//				System.out.println("S:\t"+subject.getNominalValue()+"\nP:\t"+predicate.getNominalValue()+"\nO:\t"+artNode.getNominalValue());
				rdfModel.addTriple(subject, predicate, artNode, getWorkingGraph());
			}
		} catch (PRParserException | ComponentProvisioningException | ConverterException e){
			throw new CODAException(e);
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		return response;
	}
	
	/**
	 * This method detects the entry of a graph (list of triples) based on an heuristic: entry is that subject that never appears as object
	 * @param triples
	 * @return
	 */
	private ARTResource detectGraphEntry(List<ARTTriple> triples){
		for (ARTTriple t1 : triples){
			ARTResource subj = t1.getSubject();
			boolean neverObj = true;
			for (ARTTriple t2 : triples){
				if (subj.getNominalValue().equals(t2.getObject().getNominalValue()))
					neverObj = false;
			}
			if (neverObj)
				return subj;
		}
		return null;
	}
	
	/**
	 * This service returns a description of a reified resource based on the existing CustomRangeEntry
	 * of the given predicate. First it retrieves, from the pearl of the CRE, the predicates that 
	 * describe the resource, then get the values for that predicates. If no CustomRangeEntryGraph
	 * is found for the given predicate, returns an empty description
	 * 
	 * @param crEntryId
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws FileNotFoundException
	 * @throws PRParserException
	 * @throws ModelAccessException 
	 * @throws CustomRangeInitializationException 
	 */
	@GenerateSTServiceController
	public Response getReifiedResDescription(ARTURIResource resource, ARTURIResource predicate) 
			throws UnavailableResourceException, ProjectInconsistentException, 
			PRParserException, ModelAccessException, CustomRangeInitializationException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element descriptionElem = XMLHelp.newElement(dataElement, "description");
		descriptionElem.setAttribute("resource", resource.getNominalValue());
		ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
		RDFModel rdfModel = getOWLModel();
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		codaCore.initialize(rdfModel, ontFact);
		//try to identify the CRE which has generated the reified resource
		CustomRangeEntryGraph creGraph = getCREGraphSeed(resource, predicate, codaCore);
		if (creGraph != null){
			Collection<String> predicateList = creGraph.getGraphPredicates(codaCore, false, true);
			for (String pred : predicateList){
				ARTNodeIterator itValues = rdfModel.listValuesOfSubjPredPair(resource, rdfModel.createURIResource(pred), false, getWorkingGraph());
				if (itValues.hasNext()){
					Element propertyElem = XMLHelp.newElement(descriptionElem, "property");
					propertyElem.setAttribute("predicate", pred);
					propertyElem.setAttribute("show", rdfModel.getQName(pred));
					Element objectElem = XMLHelp.newElement(propertyElem, "object");
					while (itValues.hasNext()){
						ARTNode value = itValues.next();
						if (value.isURIResource()){
							Element uriElem = XMLHelp.newElement(objectElem, "uri");
							uriElem.setTextContent(value.getNominalValue());
						} else { //is literal (since being generated by coda, it cannot be a bnode)
								ARTLiteral valueLit = value.asLiteral();
							if (valueLit.getDatatype() != null){
								Element typedElem = XMLHelp.newElement(objectElem, "typedLiteral");
								typedElem.setTextContent(valueLit.getLabel());
								typedElem.setAttribute("datatype", valueLit.getDatatype().getNominalValue());
							} else {
								Element plainElem = XMLHelp.newElement(objectElem, "plainLiteral");
								plainElem.setTextContent(valueLit.getLabel());
								if (valueLit.getLanguage() != null){
									plainElem.setAttribute("lang", valueLit.getLanguage());
								}
							}
						}
					}
				}
			}
		}
		return response;
	}
	
	/**
	 * Returns the CustomRangeEntryGraph that probably generated the reified resource.
	 * If for the given property there is no CRE available returns null,
	 * if there's just one CRE then return it, otherwise if there are multiple CRE returns the one 
	 * which its PEARL fits better the given reified resource description. 
	 * This choice in the last case is made through the following algorithm:
	 * <ul>
	 *  <li>Collect the perfect match candidates, namely the CREs which all the mandatory predicates
	 * are valued in the resource</li>
	 *  <li>If there is only one perfect candidate return that</li>
	 *  <li>If there are multiple perfect candidates, return the one with more predicates</li>
	 *   <ul>
	 *    <li>If also in this case the candidates have the same number of matched mandatory predicates,
	 *    return the one that has more total matched predicate (if multiple candidates returns the first)</li>
	 *   </ul>
	 *  <li>If there is no perfect candidate, return the one with more mandatory matched predicates</li>
	 *   <ul>
	 *    <li>If there is only one "best fit" candidate, return that</li>
	 *    <li>If there are multiple best fit candidates, return the one with more total matched
	 *    predicate (if multiple candidates, returns the first)</li>
	 *   </ul>
	 * </ul>
	 * @param resource
	 * @param predicate
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws PRParserException
	 * @throws ModelAccessException
	 */
	private CustomRangeEntryGraph getCREGraphSeed(ARTURIResource resource, ARTURIResource predicate, CODACore codaCore) 
			throws UnavailableResourceException, ProjectInconsistentException, PRParserException, ModelAccessException{
		RDFModel rdfModel = getOWLModel();
		Collection<CustomRangeEntryGraph> crEntries = crProvider.getCustomRangeEntriesGraphForProperty(predicate.getURI());
		if (crEntries.isEmpty()){
			return null;
		} else if (crEntries.size() == 1){
			return crEntries.iterator().next();
		} else { //crEntries.size() > 1
			Collection<CREMatchingStruct> creMatchStructs = new ArrayList<CREMatchingStruct>();
			for (CustomRangeEntryGraph cre : crEntries){
				creMatchStructs.add(new CREMatchingStruct(cre, resource, rdfModel, codaCore));
			}
			//look for perfect match with only mandatory predicates
			Collection<CREMatchingStruct> perfectCandidates = new ArrayList<CREMatchingStruct>();
			for (CREMatchingStruct cremStruct : creMatchStructs){
				if (cremStruct.isPerfectMandatoryMatch())
					perfectCandidates.add(cremStruct);
			}
			if (perfectCandidates.size() == 1){//if there's only one perfect CRE candidate return it
				return perfectCandidates.iterator().next().getCustomRangeEntryGraph();
			} else if (perfectCandidates.size() > 1){//multiple perfect candidates
				//return the one with more mandatory preds
				Collection<CREMatchingStruct> biggerPerfectCandidates = new ArrayList<CREMatchingStruct>();
				int maxMandatoryPreds = 0;
				for (CREMatchingStruct cremStruct : perfectCandidates){
					if (cremStruct.getMandatoryPreds() == maxMandatoryPreds){
						biggerPerfectCandidates.add(cremStruct);
					} else if (cremStruct.getMandatoryPreds() > maxMandatoryPreds){
						maxMandatoryPreds = cremStruct.getMandatoryPreds();
						biggerPerfectCandidates.clear();
						biggerPerfectCandidates.add(cremStruct);
					}
				}
				if (biggerPerfectCandidates.size() == 1){
					return biggerPerfectCandidates.iterator().next().getCustomRangeEntryGraph();
				} else { //if there are more perfect match candidate with the same number of mandatory preds
					//return the one with more total preds match
					CREMatchingStruct bestCandidate = null;
					int maxPredsMatch = 0;
					for (CREMatchingStruct cremStruct : biggerPerfectCandidates){
						if (cremStruct.getTotalPredsMatched() > maxPredsMatch){
							maxPredsMatch = cremStruct.getTotalPredsMatched();
							bestCandidate = cremStruct;
						}
					}
					return bestCandidate.getCustomRangeEntryGraph();
				}
			} else {//perfectCandidate empty => no CRE with perfect match
				//return the CRE with more matched mandatory preds
				Collection<CREMatchingStruct> bestFitCandidates = new ArrayList<CREMatchingStruct>();
				int maxMandatoryPreds = 0;
				for (CREMatchingStruct cremStruct : creMatchStructs){
					if (cremStruct.getMandatoryPredsMatched() == maxMandatoryPreds)
						bestFitCandidates.add(cremStruct);
					else if (cremStruct.getMandatoryPredsMatched() > maxMandatoryPreds){
						bestFitCandidates.clear();
						bestFitCandidates.add(cremStruct);
					}
				}
				if (bestFitCandidates.size() == 1){
					return bestFitCandidates.iterator().next().getCustomRangeEntryGraph();
				} else { //if there are more candidate with the same number of mandatory preds matched
					//return the one with more total preds match
					CREMatchingStruct bestCandidate = null;
					int maxPredsMatch = 0;
					for (CREMatchingStruct cremStruct : bestFitCandidates){
						if (cremStruct.getTotalPredsMatched() > maxPredsMatch){
							maxPredsMatch = cremStruct.getTotalPredsMatched();
							bestCandidate = cremStruct;
						}
					}
					return bestCandidate.getCustomRangeEntryGraph();
				}
			}
		}
	}
	
	/**
	 * Private class useful to facilitate the research of a "better fit" CustomRangeEntryGraph
	 * in getCREGraphSeed
	 */
	private class CREMatchingStruct {
		private CustomRangeEntryGraph cre;
		private boolean perfectMandatoryMatch;
		private int nMandatoryPreds = 0;
		private int nMandatoryPredsMatched = 0;
		private int nTotalPredsMatched = 0;
		
		public CREMatchingStruct(CustomRangeEntryGraph creGraph, ARTURIResource reifRes, RDFModel model, CODACore codaCore)
				throws PRParserException, ModelAccessException{
			this.cre = creGraph;
			Collection<String> mandatoryPreds = creGraph.getGraphPredicates(codaCore, true, true);
			nMandatoryPreds = mandatoryPreds.size();
			for (String pred : mandatoryPreds){
				ARTNodeIterator itValues = model.listValuesOfSubjPredPair(reifRes, model.createURIResource(pred), false, getWorkingGraph());
				if (itValues.hasNext())
					nMandatoryPredsMatched++;
			}
			perfectMandatoryMatch = (mandatoryPreds.size() == nMandatoryPredsMatched);
			Collection<String> allPreds = creGraph.getGraphPredicates(codaCore, false, true);
			for (String pred : allPreds){
				ARTNodeIterator itValues = model.listValuesOfSubjPredPair(reifRes, model.createURIResource(pred), false, getWorkingGraph());
				if (itValues.hasNext())
					nTotalPredsMatched++;
			}
		}
		
		public CustomRangeEntryGraph getCustomRangeEntryGraph(){
			return this.cre;
		}
		public boolean isPerfectMandatoryMatch(){
			return perfectMandatoryMatch;
		}
		public int getMandatoryPreds(){
			return nMandatoryPreds;
		}
		public int getMandatoryPredsMatched(){
			return nMandatoryPredsMatched;
		}
		public int getTotalPredsMatched(){
			return nTotalPredsMatched;
		}
	}
	
	/**
	 * Removes a reified resource and all the triples where it appears as subject 
	 * @param subject
	 * @param predicate
	 * @param resource
	 * @return
	 * @throws ModelUpdateException
	 */
	@GenerateSTServiceController
	public Response removeReifiedRes(ARTURIResource subject, ARTURIResource predicate, ARTURIResource resource) throws ModelUpdateException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		RDFModel model = getOWLModel();
		model.deleteTriple(subject, predicate, resource, getWorkingGraph());
		model.deleteTriple(resource, NodeFilters.ANY, NodeFilters.ANY, getWorkingGraph());
		return response;
	}
	
	@GenerateSTServiceController
	public Response executeURIConverter(String converter, @Optional String value) throws ComponentProvisioningException, 
			ConverterException, UnavailableResourceException, ProjectInconsistentException {
		String result = "";
		ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		codaCore.initialize(getOWLModel(), ontFact);
		if (value != null){
			result = codaCore.executeURIConverter(converter, value).getNominalValue();
		} else {
			result = codaCore.executeURIConverter(converter).getNominalValue();
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element valueElement = XMLHelp.newElement(dataElement, "value");
		valueElement.setAttribute("converter", converter);
		valueElement.setTextContent(result);
		return response;
	}
	
	@GenerateSTServiceController
	public Response executeLiteralConverter(String converter, String value, @Optional String datatype, @Optional String lang) 
			throws ComponentProvisioningException, ConverterException, UnavailableResourceException, ProjectInconsistentException {
		ModelFactory<ModelConfiguration> ontFact = PluginManager.getOntManagerImpl(getProject().getOntologyManagerImplID()).createModelFactory();
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		codaCore.initialize(getOWLModel(), ontFact);
		String result = codaCore.executeLiteralConverter(converter, value, datatype, lang).getNominalValue();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element valueElement = XMLHelp.newElement(dataElement, "value");
		valueElement.setAttribute("converter", converter);
		valueElement.setTextContent(result);
		return response;
	}
	
	/**
	 * This service is thought to create a custom range entry from client. To be useful is necessary
	 * a proper UI for client support (e.g. a wizard)
	 * This is a POST because ref and description could be quite long for a GET parameter
	 * @param id
	 * @param name
	 * @param type
	 * @param description
	 * @param ref
	 * @return
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response createCustomRangeEntry(String id, String name, String type, String description, String ref){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		CustomRangeEntry cre = CustomRangeEntryFactory.createCustomRangeEntry(type, id, name, description, ref);
		cre.saveXML();
		crProvider.addCustomRangeEntries(cre);
		return response;
	}
	
	/**
	 * This service is thought to add an existing entry to an existing custom range entry from client.
	 * To be useful is necessary a proper UI for client support (e.g. a wizard)
	 * TODO: check or not that cr and cre exist?
	 * @param customRangeId
	 * @param customRangeEntryId
	 * @return
	 */
	@GenerateSTServiceController
	public Response addEntryToCustomRange(String customRangeId, String customRangeEntryId){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		CustomRange cr = crProvider.getCustomRangeById(customRangeId);
		CustomRangeEntry cre = crProvider.getCustomRangeEntryById(customRangeEntryId);
		cr.addEntry(cre);
		cr.saveXML();
		return response;
	}
	
	/**
	 * Creates an empty CustomRange (a CR without CREntries related)
	 * @param id
	 * @return
	 */
	@GenerateSTServiceController
	public Response createCustomRange(String id){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		CustomRange cr = CustomRangeFactory.createEmptyCustomRange(id);
		cr.saveXML();
		crProvider.addCustomRange(cr);
		return response;
	}
	
	/**
	 * Adds a CustomRangeEntry to the configuration, namely an entry that relates a predicate with an
	 * existing CustomRange
	 * TODO: check or not that cr exists?
	 * @param predicate
	 * @param customRangeId
	 * @param replaceRanges
	 * @return
	 */
	@GenerateSTServiceController
	public Response addCustomRangeToPredicate(ARTURIResource predicate, String customRangeId, @Optional (defaultValue = "false") boolean replaceRanges){
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		CustomRange cr = crProvider.getCustomRangeById(customRangeId);
		CustomRangeConfig crConfig = crProvider.getCustomRangeConfig();
		crConfig.addEntry(new CustomRangeConfigEntry(predicate.getURI(), cr, replaceRanges));
		crConfig.saveXML();
		return response;
	}

}
