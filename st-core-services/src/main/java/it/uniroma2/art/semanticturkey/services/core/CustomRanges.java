package it.uniroma2.art.semanticturkey.services.core;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;

import org.antlr.runtime.RecognitionException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.ProjectionOperator;
import it.uniroma2.art.coda.pearl.parser.antlr.AntlrParserRuntimeException;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.QueryEvaluationException;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.exceptions.UnsupportedQueryLanguageException;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.query.GraphQuery;
import it.uniroma2.art.owlart.query.MalformedQueryException;
import it.uniroma2.art.owlart.query.TupleBindings;
import it.uniroma2.art.owlart.query.TupleBindingsIterator;
import it.uniroma2.art.owlart.query.TupleQuery;
import it.uniroma2.art.owlart.query.Update;
import it.uniroma2.art.semanticturkey.customrange.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customrange.CustomRange;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeConfigEntry;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntry;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntryGraph;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeEntryParseUtils;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.customrange.UserPromptStruct;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.CustomRangeException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.generation.annotation.RequestMethod;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.ontology.model.PredicateObjectsListFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFURI;
import it.uniroma2.art.semanticturkey.services.STServiceAdapterOLD;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.RDF4JMigrationUtils;
import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

@GenerateSTServiceController
@Validated
@Component
public class CustomRanges extends STServiceAdapterOLD {
	
	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;
	@Autowired
	private CustomRangeProvider crProvider;
	@Autowired
	private ServletRequest request;
	
	protected static Logger logger = LoggerFactory.getLogger(CustomRanges.class);
	
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
	 * @throws CustomRangeException 
	 * @throws UnassignableFeaturePathException 
	 * @throws ProjectionRuleModelNotSet 
	 */
	@SuppressWarnings("unchecked")
	@GenerateSTServiceController
	public Response runCoda(ARTResource subject, ARTURIResource predicate, String crEntryId) throws FileNotFoundException, CODAException, 
			UnavailableResourceException, ProjectInconsistentException, ModelUpdateException, CustomRangeException,  
			ProjectionRuleModelNotSet, UnassignableFeaturePathException {
		//get the parameters to put in the userPromptMap from the request
		Map<String, String[]> parMap = request.getParameterMap();//the others params (form key and values) are dynamic, get it directly from request
		Map<String, String> userPromptMap = new HashMap<String, String>();
		for (Entry<String, String[]> par : parMap.entrySet()){
//			System.out.println("param: " + par.getKey() + ", value: " + par.getValue()[0]);
			userPromptMap.put(par.getKey(), par.getValue()[0]);
		}
		//Remove useless parameters for executePearl method (parameters not belonging to userPrompt feature)
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
		RDFModel rdfModel = getOWLModel();
		CODACore codaCore = getInitializedCodaCore(rdfModel);
		try {
			CustomRangeEntry crEntry = crProvider.getCustomRangeEntryById(crEntryId);
			if (crEntry.isTypeGraph()){
				CustomRangeEntryGraph creGraph = crEntry.asCustomRangeEntryGraph();
				List<ARTTriple> triples = creGraph.executePearl(codaCore, userPromptMap);
				rdfModel.addTriple(subject, predicate, detectGraphEntry(triples), getWorkingGraph());
				String query = createInsertQuery(triples);
				Update uq = rdfModel.createUpdateQuery(query);
				uq.evaluate(false);
			} else if (crEntry.isTypeNode()){
				String value = userPromptMap.entrySet().iterator().next().getValue();//get the only value
				ProjectionOperator projOperator = CustomRangeEntryParseUtils.getProjectionOperator(codaCore, crEntry.getRef());
				Value rdf4jNode = codaCore.executeProjectionOperator(projOperator, value);
				ARTNode artNode = RDF4JMigrationUtils.convert2art(rdf4jNode);
				rdfModel.addTriple(subject, predicate, artNode, getWorkingGraph());
			}
		} catch (PRParserException | ComponentProvisioningException | ConverterException | UnsupportedQueryLanguageException | 
				ModelAccessException | MalformedQueryException | QueryEvaluationException e){
			throw new CODAException(e);
		} finally {
			shutDownCodaCore(codaCore);
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		return response;
	}
	
	private String createInsertQuery(List<ARTTriple> triples) {
		String query = "INSERT DATA { \n"
				+ "GRAPH " + SPARQLHelp.toSPARQL(RDF4JMigrationUtils.convert2rdf4j(getWorkingGraph().asURIResource())) + " {\n";
		for (ARTTriple triple : triples){
			query += SPARQLHelp.toSPARQL(triple.getSubject()) + " " + 
					SPARQLHelp.toSPARQL(triple.getPredicate()) + " " + 
					SPARQLHelp.toSPARQL(triple.getObject()) + " .\n";
		}
		query += "}\n}";
		return query;
	}
	
	/**
	 * This method detects the entry of a graph (list of triples) based on an heuristic: entry is that subject that never appears as object
	 * @param triples
	 * @return
	 */
	private ARTResource detectGraphEntry(List<ARTTriple> triples){
		for (ARTTriple t1 : triples){
//			System.out.println("triple " + t1.getSubject().stringValue() + " " + t1.getPredicate().stringValue() + " " + t1.getObject().stringValue());
			org.eclipse.rdf4j.model.Resource subj = t1.getSubject();
			boolean neverObj = true;
			for (ARTTriple t2 : triples){
				if (subj.equals(t2.getObject()))
					neverObj = false;
			}
			if (neverObj) {
				return RDF4JMigrationUtils.convert2art(subj);
			}
		}
		return null;
	}
	
	/**
	 * Returns a description of a graph created through a CRE. The description is 
	 * based on the userPrompt fields of the CRE that generated the graph.
	 * If no CustomRangeEntryGraph is found for the given predicate, returns an empty description.
	 * 
	 * @param crEntryId
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws FileNotFoundException
	 * @throws PRParserException
	 * @throws ModelAccessException 
	 * @throws RDFModelNotSetException 
	 * @throws QueryEvaluationException 
	 * @throws MalformedQueryException 
	 * @throws UnsupportedQueryLanguageException 
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response getGraphObjectDescription(ARTResource resource, ARTURIResource predicate) 
			throws UnavailableResourceException, ProjectInconsistentException, ModelAccessException, 
			RDFModelNotSetException, UnsupportedQueryLanguageException, MalformedQueryException, 
			QueryEvaluationException, PRParserException {
		
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		
		RDFModel rdfModel = getOWLModel();
		CODACore codaCore = getInitializedCodaCore(rdfModel);
		try {
			//try to identify the CRE which has generated the graph
			CustomRangeEntryGraph creGraph = getCREGraphSeed(resource, predicate, codaCore);
			if (creGraph != null){
				//predicate-object list Element (although this is not a pred-obj list, but a "userPrompt"-value, exploit the POList structure)
				Map<ARTURIResource, STRDFResource> art2STRDFPredicates = new LinkedHashMap<ARTURIResource, STRDFResource>();
				Multimap<ARTURIResource, STRDFNode> resultPredicateObjectValues = HashMultimap.create();
				PredicateObjectsList predicateObjectsList = PredicateObjectsListFactory
						.createPredicateObjectsList(art2STRDFPredicates, resultPredicateObjectValues);
				//retrieves, through a query, the values of those placeholders filled through a userPrompt in the CustomForm
				Map<String, ARTNode> promptValuesMap = new HashMap<String, ARTNode>();
				String graphSection = creGraph.getGraphSectionAsString(codaCore, true);
				Map<String, String> phUserPromptMap = creGraph.getRelevantFormPlaceholders(codaCore);
				String bindings = "";
				for (String ph : phUserPromptMap.keySet()) {
					bindings += "?" + ph + " ";
				}
				String query = "SELECT " + bindings + " WHERE { " + graphSection + " }";
				TupleQuery tupleQuery = rdfModel.createTupleQuery(query);
				tupleQuery.setBinding(creGraph.getEntryPointPlaceholder(codaCore).substring(1), resource);
				TupleBindingsIterator tupleBindings = tupleQuery.evaluate(false);
				//iterate over the results and create a map <userPrompt, value>
				while (tupleBindings.streamOpen()) {
					TupleBindings tp = tupleBindings.getNext();
					for (Entry<String, String> phUserPrompt : phUserPromptMap.entrySet()) {
						String phId = phUserPrompt.getKey();
						String userPrompt = phUserPrompt.getValue();
						if (tp.hasBinding(phId)) {
							promptValuesMap.put(userPrompt, tp.getBoundValue(phId));
						}
					}
				}
				
				//fill the predicate object list structure
				for (Entry<String, ARTNode> promptValue : promptValuesMap.entrySet()){
					//create a "fake" predicate to represent the userPrompt label
					ARTURIResource predResource = rdfModel.createURIResource(rdfModel.getDefaultNamespace() + promptValue.getKey());
					STRDFURI stPred = STRDFNodeFactory.createSTRDFURI(predResource, null, true, promptValue.getKey());
					
					art2STRDFPredicates.put(predResource, stPred);
					STRDFNode stNode = STRDFNodeFactory.createSTRDFNode(rdfModel, promptValue.getValue(), true, true, true);
					resultPredicateObjectValues.put(predResource, stNode);
				}
				if (!promptValuesMap.isEmpty()) {
					Element propertiesElem = XMLHelp.newElement(dataElement, "properties");
					RDFXMLHelp.addPredicateObjectList(propertiesElem, predicateObjectsList);
				}
			}
		} finally {
			shutDownCodaCore(codaCore);
		}
		return response;
	}
	
	/**
	 * Removes a reified resource according to the CustomRangeEntryGraph that generated it
	 * @param subject
	 * @param predicate
	 * @param resource
	 * @return
	 * @throws ModelUpdateException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 */
	@GenerateSTServiceController
	public Response removeReifiedResource(ARTURIResource subject, ARTURIResource predicate, ARTURIResource resource)
			throws ModelUpdateException, UnsupportedQueryLanguageException, ModelAccessException,
			MalformedQueryException, QueryEvaluationException, UnavailableResourceException, 
			ProjectInconsistentException, PRParserException, RDFModelNotSetException {
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		
		logger.debug("deleting reified resource " + resource.getNominalValue());
		
		RDFModel model = getOWLModel();
		//remove resource as object in the triple <s, p, o> for the given subject and predicate
		model.deleteTriple(subject, predicate, resource, getWorkingGraph());

		CODACore codaCore = getInitializedCodaCore(model);
		CustomRangeEntryGraph cre = getCREGraphSeed(resource, predicate, codaCore);
		if (cre == null) { //
			/* If property hasn't a CRE simply delete all triples where resource occurs.
			 * note: this case should never be verified cause this service should be called only 
			 * when the predicate has a CustomRangeEntry */
			model.deleteTriple(NodeFilters.ANY, NodeFilters.ANY, resource, getWorkingGraph());
			model.deleteTriple(resource, NodeFilters.ANY, NodeFilters.ANY, getWorkingGraph());
		} else { //otherwise remove with a SPARQL delete the graph defined by the CRE graph
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append("delete { ");
			queryBuilder.append(cre.getGraphSectionAsString(codaCore, false));
			queryBuilder.append(" } where { ");
			queryBuilder.append(cre.getGraphSectionAsString(codaCore, true));
			queryBuilder.append(" }");
			Update update = model.createUpdateQuery(queryBuilder.toString());
			update.setBinding(cre.getEntryPointPlaceholder(codaCore).substring(1), resource);
			update.evaluate(false);
		}
		shutDownCodaCore(codaCore);
		return response;
	}
	
	/**
	 * Returns the CustomRangeEntryGraph that probably generated the reified resource.
	 * If for the given property there is no CRE available returns null,
	 * if there's just one CRE then return it, otherwise if there are multiple CRE returns the one 
	 * which its PEARL fits better the given reified resource description. 
	 * @param resource
	 * @param predicate
	 * @param codaCore
	 * @return
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 * @throws UnsupportedQueryLanguageException
	 * @throws ModelAccessException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	private CustomRangeEntryGraph getCREGraphSeed(ARTResource resource, ARTURIResource predicate, CODACore codaCore)
			throws RDFModelNotSetException, UnsupportedQueryLanguageException, 
			ModelAccessException, MalformedQueryException, QueryEvaluationException {
		Collection<CustomRangeEntryGraph> crEntries = crProvider.getCustomRangeEntriesGraphForProperty(predicate.getURI());
		if (crEntries.isEmpty()){
			return null;
		} else if (crEntries.size() == 1){
			return crEntries.iterator().next();
		} else { //crEntries.size() > 1
			//return the CRE whose graph section matches more triples in the model
			OWLModel model = getOWLModel();
			int maxStats = 0;
			CustomRangeEntryGraph bestCre = null;
			for (CustomRangeEntryGraph cre : crEntries) {
				try {
					//creating the construct query
					StringBuilder queryBuilder = new StringBuilder();
					queryBuilder.append("construct { ");
					queryBuilder.append(cre.getGraphSectionAsString(codaCore, false));
					queryBuilder.append(" } where { ");
					queryBuilder.append(cre.getGraphSectionAsString(codaCore, true));
					queryBuilder.append(" }");
					String query = queryBuilder.toString();
					GraphQuery gq = model.createGraphQuery(query);
					gq.setBinding(cre.getEntryPointPlaceholder(codaCore).substring(1), resource);
					int nStats = Iterators.size(gq.evaluate(false));
					if (nStats > maxStats) {
						maxStats = nStats;
						bestCre = cre;
					}
				} catch (PRParserException e) {
					//if one of the CRE contains an error, catch the exception and continue checking the other CREs
					System.out.println("Parsing error in PEARL rule of CustomRangeEntry with ID " + cre.getId() + ". "
							+ "The CustomRangeEntry will be ignored, please fix its PEARL rule.");
				}
			}
			return bestCre;
		}
	}
	
	@GenerateSTServiceController
	public Response executeURIConverter(String converter, @Optional String value) throws ComponentProvisioningException, 
			ConverterException, UnavailableResourceException, ProjectInconsistentException {
		String result = "";
		CODACore codaCore = getInitializedCodaCore(getOWLModel());
		if (value != null){
			result = codaCore.executeURIConverter(converter, value).stringValue();
		} else {
			result = codaCore.executeURIConverter(converter).stringValue();
		}
		shutDownCodaCore(codaCore);
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
		CODACore codaCore = getInitializedCodaCore(getOWLModel());
		if (datatype != null && datatype.equals(""))
			datatype = null;
		if (lang != null && lang.equals(""))
			lang = null;
		String result = codaCore.executeLiteralConverter(converter, value, datatype, lang).stringValue();
		shutDownCodaCore(codaCore);
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element valueElement = XMLHelp.newElement(dataElement, "value");
		valueElement.setAttribute("converter", converter);
		valueElement.setTextContent(result);
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
		CustomRange cr = crProvider.getCustomRangeById(id);
		if (cr != null){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElem = response.getDataElement();
			Element crElem = XMLHelp.newElement(dataElem, "customRange");
			crElem.setAttribute("id", cr.getId());
			for (String creId : cr.getEntriesId()){
				Element entryElem = XMLHelp.newElement(crElem, "entry");
				entryElem.setAttribute("id", creId);
			}
			return response;
		} else {
			return createReplyFAIL("CustomRange with id " + id + " not found");
		}
	}
	
	/**
	 * Returns all the CustomRange available
	 * @return
	 */
	@GenerateSTServiceController
	public Response getAllCustomRanges(){
		Collection<CustomRange> crColl = crProvider.getAllCustomRanges();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		for (CustomRange cr : crColl){
			XMLHelp.newElement(dataElem, "customRange", cr.getId());
		}
		return response;
	}
	
	/**
	 * Creates an empty CustomRange (a CR without CREntries related)
	 * @param id
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response createCustomRange(String id) throws CustomRangeException{
		crProvider.createCustomRange(id);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Deletes the CustomRange with the given id
	 * @param id
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response deleteCustomRange(String id) throws CustomRangeException{
		crProvider.deleteCustomRange(id);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Returns all the CustomRangeEntry available
	 * @return
	 */
	@GenerateSTServiceController
	public Response getAllCustomRangeEntries(){
		Collection<CustomRangeEntry> creColl = crProvider.getAllCustomRangeEntries();
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElem = response.getDataElement();
		for (CustomRangeEntry cre : creColl){
			XMLHelp.newElement(dataElem, "customRangeEntry", cre.getId());
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
		CustomRangeEntry crEntry = crProvider.getCustomRangeEntryById(id);
		if (crEntry != null){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElem = response.getDataElement();
			Element creElem = XMLHelp.newElement(dataElem, "customRangeEntry");
			creElem.setAttribute("id", crEntry.getId());
			creElem.setAttribute("name", crEntry.getName());
			creElem.setAttribute("type", crEntry.getType());
			Element descriptionElem = XMLHelp.newElement(creElem, "description");
			descriptionElem.setTextContent(crEntry.getDescription());
			Element refElem = XMLHelp.newElement(creElem, "ref");
			refElem.setTextContent(crEntry.getRef());
			if (crEntry.isTypeGraph()){
				String propertyChain = crEntry.asCustomRangeEntryGraph().serializePropertyChain();
				if (!propertyChain.isEmpty()) {
					refElem.setAttribute("showPropertyChain", propertyChain);
				}
			}
			return response;
		} else {
			return createReplyFAIL("CustomRangeEntry with id " + id + " not found");
		}
	}
	
	/**
	 * Returns a serialization representing the form of the CurtomRangeEntry with the given id
	 * @param id
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 * @throws ModelAccessException 
	 */
	@GenerateSTServiceController
	public Response getCustomRangeEntryForm(String id) throws UnavailableResourceException,
			ProjectInconsistentException, PRParserException, RDFModelNotSetException, ModelAccessException {
		CustomRangeEntry crEntry = crProvider.getCustomRangeEntryById(id);
		if (crEntry != null){
			XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
			Element dataElem = response.getDataElement();
			Element formElem = XMLHelp.newElement(dataElem, "form");
			
			CODACore codaCore = getInitializedCodaCore(getOWLModel());
			Collection<UserPromptStruct> form = crEntry.getForm(codaCore);
			shutDownCodaCore(codaCore);
			if (!form.isEmpty()){
				for (UserPromptStruct formEntry : form){
					Element formEntryElem = XMLHelp.newElement(formElem, "formEntry");
					formEntryElem.setAttribute("placeholderId", formEntry.getPlaceholderId());
					formEntryElem.setAttribute("userPrompt", formEntry.getUserPromptName());
					formEntryElem.setAttribute("type", formEntry.getRdfType());
					formEntryElem.setAttribute("mandatory", formEntry.isMandatory()+"");
					if (formEntry.hasConverter()) {
						ConverterMention converter = formEntry.getConverter();
						Element converterElem = XMLHelp.newElement(formEntryElem, "converter");
						converterElem.setAttribute("uri", converter.getURI());
						//for special case langString, specify the converter argument too
						if (formEntry.getConverterArgPhId() != null) {
							String phLangId = formEntry.getConverterArgPhId();
							/* the language placeholder (arguments of langString converter)
							 * is already added to the xml as formEntry element since in
							 * PEARL it must be defined before it's used as argument,
							 * so remove the element from formEntry and add it as argument 
							 * of converter xml element */
							NodeList fe = formElem.getElementsByTagName("formEntry");
							for (int i=0; i<fe.getLength(); i++) {
								Node feItem = fe.item(i);
								if (feItem.getAttributes().getNamedItem("placeholderId").getNodeValue().equals(phLangId)) {
									Element convArgElem = XMLHelp.newElement(converterElem, "arg");
									convArgElem.setAttribute("userPrompt", feItem.getAttributes().getNamedItem("userPrompt").getNodeValue());
									break;
								}
							}
						}
					}
					if (formEntry.isLiteral()){
						if (formEntry.hasDatatype())
							formEntryElem.setAttribute("datatype", formEntry.getLiteralDatatype());
						if (formEntry.hasLanguage())
							formEntryElem.setAttribute("lang", formEntry.getLiteralLang());
					}
				}
			} else {
				formElem.setAttribute("exception", "No userPrompt/ features found");
			}
			return response;
		} else {
			return createReplyFAIL("CustomRangeEntry with id " + id + " not found");
		}
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
	 * This service is thought to create a custom range entry from client.
	 * To be useful is necessary a proper UI for client support (e.g. a wizard)
	 * This is a POST because ref and description could be quite long for a GET parameter
	 * @param type
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @param showProp Useful only if type is "graph"
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response createCustomRangeEntry(String type, String id, String name, String description, String ref, @Optional List<IRI> showPropChain)
			throws CustomRangeException {
		//avoid proliferation of new line in saved pearl (carriage return character "\r" are added to ref when calling this service
		crProvider.createCustomRangeEntry(type, id, name, description, ref, showPropChain);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Creates a new CRE cloning an existing CRE
	 * @param sourceId id of the CRE to clone
	 * @param targetId id of the CRE to create
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response cloneCustomRangeEntry(String sourceId, String targetId) throws CustomRangeException {
		CustomRangeEntry sourceCRE = crProvider.getCustomRangeEntryById(sourceId);
		if (sourceCRE == null) {
			throw new CustomRangeException("Impossible to clone '" + sourceId + 
					"'. A CustomRangeEntry with this ID doesn't exists");
		}
		if (sourceCRE.isTypeGraph()) {
			String ref = sourceCRE.getRef();
			//replace in pearl rule the namespace
			ref = ref.replace(sourceId, targetId);
			//and the rule ID
			String sourceRuleId = sourceId.replace(CustomRangeEntry.PREFIX, "id:");
			String targetRuleId = targetId.replace(CustomRangeEntry.PREFIX, "id:");
			ref = ref.replace(sourceRuleId, targetRuleId);
			crProvider.createCustomRangeEntry(sourceCRE.getType(), targetId, sourceCRE.getName(), sourceCRE.getDescription(),
					ref, sourceCRE.asCustomRangeEntryGraph().getShowPropertyChain());
		} else { //type "node"
			crProvider.createCustomRangeEntry(sourceCRE.getType(), targetId, sourceCRE.getName(), sourceCRE.getDescription(),
					sourceCRE.getRef(), null);
		}
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Deletes the CustomRangeEntry with the given id. Removes also the CRE on cascade from the CR
	 * that contain it. 
	 * @param id
	 * @param deleteEmptyCr if true deletes CustomRange that are left empty after the deletion
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response deleteCustomRangeEntry(String id, @Optional (defaultValue = "false") boolean deleteEmptyCr) throws CustomRangeException{
		crProvider.deleteCustomRangeEntry(id, deleteEmptyCr);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Given the id of a CustomRangeEntry tells if it belong to a CustomRange
	 * @param id
	 * @return
	 */
	@GenerateSTServiceController
	public Response isEntryLinkedToCustomRange(String id) {
		Collection<CustomRange> crColl = crProvider.getAllCustomRanges();
		for (CustomRange cr: crColl) {
			if (cr.getEntriesId().contains(id)) {
				return createBooleanResponse(true);
			}
		}
		return createBooleanResponse(false);
	}
	
	/**
	 * Updates a CustomRangeEntry. Allows to change the name, the description, the ref and eventually
	 * (if the CRE is graph) the showProp of the CRE with the given id. It doesn't allow to change 
	 * the type.
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @param showProp
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response updateCustomRangeEntry(String id, String name, String description, String ref, @Optional List<IRI> showPropChain) throws CustomRangeException{
		//avoid proliferation of new line in saved pearl (carriage return character "\r" are added to ref when calling this service
		ref = ref.replace("\r", "");
		crProvider.updateCustomRangeEntry(id, name, description, ref, showPropChain);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Used to check if a property chain (manually created by the user client-side) is correct.
	 * This service doesn't do anything, it's enough to know if it can parse the input IRI list   
	 * @param propChain
	 * @return
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response checkShowPropertyChain(List<IRI> propChain) {
		return createReplyResponse(RepliesStatus.ok);
	}
	
	
	/**
	 * Adds an existing CustomRangeEntry to an existing CustomRange entry
	 * @param customRangeId
	 * @param customRangeEntryId
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response addEntryToCustomRange(String customRangeId, String customRangeEntryId) throws CustomRangeException{
		crProvider.addEntryToCustomRange(customRangeId, customRangeEntryId);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Removes a CustomRangeEntry from an existing CustomRange
	 * @param customRangeId
	 * @param customRangeEntryId
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response removeEntryFromCustomRange(String customRangeId, String customRangeEntryId) throws CustomRangeException{
		crProvider.removeEntryFromCustomRange(customRangeId, customRangeEntryId);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Returns all the property-CustomRange pairs defined in the CustomRange configuration
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response getCustomRangeConfigMap() throws CustomRangeException{
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
	 * Adds a CustomRange to a property 
	 * @param property
	 * @param customRangeId
	 * @param replaceRanges
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response addCustomRangeToProperty(String customRangeId, ARTURIResource property,
			@Optional (defaultValue = "false") boolean replaceRanges) throws CustomRangeException{
		crProvider.addCustomRangeToProperty(customRangeId, property, replaceRanges);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Remove the CustomRange of the given property
	 * @param property
	 * @return
	 * @throws CustomRangeException 
	 */
	@GenerateSTServiceController
	public Response removeCustomRangeFromProperty(ARTURIResource property) throws CustomRangeException{
		crProvider.removeCustomRangeFromProperty(property);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Update the replaceRanges attribute of a property-CR mapping for the given property
	 * @param property
	 * @param replaceRanges
	 * @return
	 */
	@GenerateSTServiceController
	public Response updateReplaceRanges(ARTURIResource property, boolean replaceRanges) {
		crProvider.setReplaceRanges(property, replaceRanges);
		return createReplyResponse(RepliesStatus.ok);
	}
	
	/**
	 * Tries to validate a pearl code.
	 * @param pearl rule to be parsed, it should be a whole pearl rule if the CRE is a graph entry
     * or a converter if the CRE is a node entry
	 * @param creType tells if the CRE is type "node" or "graph".
     * Determines also the nature of the pearl parameter
	 * @return
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws ModelAccessException 
	 * @throws RDFModelNotSetException 
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response validatePearl(String pearl, String creType) throws UnavailableResourceException, ProjectInconsistentException, RDFModelNotSetException, ModelAccessException{
		if (creType.equals(CustomRangeEntry.Types.graph.toString())) {
			try {
				InputStream pearlStream = new ByteArrayInputStream(pearl.getBytes(StandardCharsets.UTF_8));
				CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
				codaCore.setProjectionRulesModelAndParseIt(pearlStream);
				//setProjectionRulesModelAndParseIt didn't throw exception, so pearl is valid
				return createReplyResponse(RepliesStatus.ok);
			} catch (PRParserException e) {
				return createReplyFAIL("Invalid pearl rule: " + e.getErrorAsString());
			} catch (AntlrParserRuntimeException e) {
				return createReplyFAIL("Invalid pearl rule: " + e.getMsg());
			}
		} else { //type node
			try {
				//Treat separately the case where pearl is simply "literal", because parser.projectionOperator()
				//called in createProjectionOperatorTree()
				//inexplicably throws an exception (no viable alternative at input <EOF>)
				if (pearl.equals("literal")) {
					return createReplyResponse(RepliesStatus.ok); 
				}
				CustomRangeEntryParseUtils.createProjectionOperatorTree(pearl);
				//parser didn't throw exception, so pearl is valid
				return createReplyResponse(RepliesStatus.ok);
			} catch (RecognitionException e) {
				return createReplyFAIL("Invalid projection operator");
			} catch (AntlrParserRuntimeException e) {
				return createReplyFAIL("Invalid projection operator: " + e.getMsg());
			}
		}
	}
	
	private CODACore getInitializedCodaCore(RDFModel rdfModel) throws UnavailableResourceException, ProjectInconsistentException{
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		codaCore.initialize(RDF4JMigrationUtils.extractRDF4JConnection(rdfModel));
		return codaCore;
	}
	
	private void shutDownCodaCore(CODACore codaCore) {
		codaCore.setRepositoryConnection(null);
		codaCore.stopAndClose();
	}

}
