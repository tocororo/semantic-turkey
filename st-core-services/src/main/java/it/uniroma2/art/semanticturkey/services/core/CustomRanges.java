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
import it.uniroma2.art.semanticturkey.customrange.CustomRangeConfigEntry;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntry;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntryGraph;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntryNode;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.customrange.UserPromptStruct;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.CustomRangeInitializationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.FileNotFoundException;
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
		Collection<CustomRangeConfigEntry> crConfEntries = crProvider.getCustomRangeConfig();
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
//		CustomRangeProvider crProvider = new CustomRangeProvider();
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
	 * Returns the serialization of all the CustomRangeEntry availabel for the given property
	 * @param property
	 * @return
	 * @throws ModelAccessException
	 */
	@GenerateSTServiceController
	public Response getCustomRangeEntries(String property) throws ModelAccessException{
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
//		CustomRangeProvider crProvider = new CustomRangeProvider();
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
//		CustomRangeProvider crProvider = new CustomRangeProvider();
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
					System.out.println("S:\t"+triple.getSubject()+"\nP:\t"+triple.getPredicate()+"\nO:\t"+triple.getObject());
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
				System.out.println("S:\t"+subject.getNominalValue()+"\nP:\t"+predicate.getNominalValue()+"\nO:\t"+artNode.getNominalValue());
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
	 * of the given property. First it retrieves, from the pearl of the CRE, the predicates that 
	 * describe the resource, then get the values for that predicates.
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
		Collection<CustomRangeEntryGraph> crEntries = crProvider.getCustomRangeEntriesGraphForProperty(predicate.getURI());
		//TODO: in caso di più CREGraph capire come fare ad individuare quello che ha generato la reified resource
		for (CustomRangeEntryGraph creGraph : crEntries){
			//try to identify the CRE which has generated the reified resource
			Collection<String> predicateList = creGraph.getGraphPredicates(codaCore, false);
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

}
