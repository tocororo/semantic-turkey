package it.uniroma2.art.semanticturkey.services.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.HashMultimap;
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
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormLevel;
import it.uniroma2.art.semanticturkey.customform.CustomFormXMLHelper;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.CustomFormParseException;
import it.uniroma2.art.semanticturkey.customform.CustomFormParseUtils;
import it.uniroma2.art.semanticturkey.customform.DuplicateIdException;
import it.uniroma2.art.semanticturkey.customform.FormCollection;
import it.uniroma2.art.semanticturkey.customform.FormsMapping;
import it.uniroma2.art.semanticturkey.customform.UserPromptStruct;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsListSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;
import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;

@STService
public class CustomForms extends STServiceAdapter {
	
	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;
	@Autowired
	private CustomFormManager cfManager;
	@Autowired
	private ServletRequest request;
	
	/**
	 * This service get as parameters a custom form id and a set of userPrompt key-value pairs
	 * (userPrompt are unknown a priori, so pairs are dynamic and have to be get from the request
	 * TODO: find a better solution), then run CODA on the pearl specified in the CustomForm
	 * (with the given id <code>customFormId</code>) and the features filled following the userPrompt parameters.
	 * Finally, "append" the triples generated by CODA to the subject-property pair
	 * @param subject
	 * @param predicate
	 * @param customFormId
	 * @return
	 * @throws CODAException
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws ModelUpdateException
	 * @throws ProjectionRuleModelNotSet
	 * @throws UnassignableFeaturePathException
	 */
	@SuppressWarnings("unchecked")
	@STServiceOperation
	@Write
	public void executeForm(Resource subject, IRI predicate, String customFormId)
			throws CODAException, ProjectInconsistentException {
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
		userPromptMap.remove("customFormId");
		userPromptMap.remove("subject");
		userPromptMap.remove("predicate");
//		for (Entry<String, String> e : userPromptMap.entrySet()){
//			System.out.println("userPrompt: " + e.getKey() + " = " + e.getValue());
//		}
		RepositoryConnection repoConnection = getManagedConnection();
		CODACore codaCore = getInitializedCodaCore(repoConnection);
		try {
			CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
			if (cForm.isTypeGraph()){
				CustomFormGraph cfGraph = cForm.asCustomFormGraph();
				List<ARTTriple> triples = cfGraph.executePearl(codaCore, userPromptMap);
				String query = createInsertQuery(triples);
				Update update = repoConnection.prepareUpdate(query);
				update.setIncludeInferred(false);
				update.execute();
				//link the generated graph with the resource
				repoConnection.add(subject, predicate, detectGraphEntry(triples), getWorkingGraph());
			} else if (cForm.isTypeNode()){
				String value = userPromptMap.entrySet().iterator().next().getValue();//get the only value
				ProjectionOperator projOperator = CustomFormParseUtils.getProjectionOperator(codaCore, cForm.getRef());
				Value generatedValue = codaCore.executeProjectionOperator(projOperator, value);
				//link the generated value with the resource
				repoConnection.add(subject, predicate, generatedValue, getWorkingGraph());
			}
		} catch (PRParserException | ComponentProvisioningException | ConverterException |
				ProjectionRuleModelNotSet | UnassignableFeaturePathException e){
			throw new CODAException(e);
		} finally {
			shutDownCodaCore(codaCore);
		}
	}
	
	private String createInsertQuery(List<ARTTriple> triples) {
		String query = "INSERT DATA { \n"
				+ "GRAPH " + SPARQLHelp.toSPARQL(getWorkingGraph()) + " {\n";
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
	private Resource detectGraphEntry(List<ARTTriple> triples){
		for (ARTTriple t1 : triples){
//			System.out.println("triple " + t1.getSubject().stringValue() + " " + t1.getPredicate().stringValue() + " " + t1.getObject().stringValue());
			org.eclipse.rdf4j.model.Resource subj = t1.getSubject();
			boolean neverObj = true;
			for (ARTTriple t2 : triples){
				if (subj.equals(t2.getObject()))
					neverObj = false;
			}
			if (neverObj) {
				return subj;
			}
		}
		return null;
	}
	
	/**
	 * Returns a description of a graph created through a CustomForm. The description is 
	 * based on the userPrompt fields of the CF that generated the graph.
	 * If no CustomFormGraph is found for the given predicate, returns an empty description.
	 * 
	 * @param resource
	 * @param predicate
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws ModelAccessException
	 * @throws RDFModelNotSetException
	 * @throws PRParserException
	 */
	@STServiceOperation
	@Read
	public Map<String, ResourceViewSection> getGraphObjectDescription(Resource resource, IRI predicate) 
			throws ProjectInconsistentException, RDFModelNotSetException, PRParserException {
		
		RepositoryConnection repoConnection = getManagedConnection();
		CODACore codaCore = getInitializedCodaCore(repoConnection);
		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();
		
		try {
			//try to identify the CF which has generated the graph
			CustomFormGraph cfGraph = getCustomFormGraphSeed(resource, predicate, codaCore);
			if (cfGraph != null) {
				//retrieves, through a query, the values of those placeholders filled through a userPrompt in the CustomForm
				Map<String, Value> promptValuesMap = new HashMap<String, Value>();
				String graphSection = cfGraph.getGraphSectionAsString(codaCore, true);
				Map<String, String> phUserPromptMap = cfGraph.getRelevantFormPlaceholders(codaCore);
				String bindings = "";
				for (String ph : phUserPromptMap.keySet()) {
					bindings += "?" + ph + " ";
				}
				String query = "SELECT " + bindings + " WHERE { " + graphSection + " }";
				TupleQuery tq = repoConnection.prepareTupleQuery(query);
				tq.setIncludeInferred(false);
				tq.setBinding(cfGraph.getEntryPointPlaceholder(codaCore).substring(1), resource);
				TupleQueryResult result = tq.evaluate();
				//iterate over the results and create a map <userPrompt, value>
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					for (Entry<String, String> phUserPrompt : phUserPromptMap.entrySet()) {
						String phId = phUserPrompt.getKey();
						String userPrompt = phUserPrompt.getValue();
						if (bindingSet.hasBinding(phId)) {
							promptValuesMap.put(userPrompt, bindingSet.getValue(phId));
						}
					}
				}
				
				//fill the predicate object list structure
				Map<IRI, AnnotatedValue<IRI>> propMap = new HashMap<>();
				Multimap<IRI, AnnotatedValue<?>> valueMultiMap = HashMultimap.create();
				
				SimpleValueFactory vf = SimpleValueFactory.getInstance();
				
				for (Entry<String, Value> promptValue : promptValuesMap.entrySet()) {
					//create a "fake" predicate to represent the userPrompt label
					IRI predResource = vf.createIRI(repoConnection.getNamespace(""), promptValue.getKey());
					
					AnnotatedValue<IRI> annotatedPredicate = new AnnotatedValue<IRI>(predResource);
					annotatedPredicate.setAttribute("role", RDFResourceRolesEnum.property.toString());
					annotatedPredicate.setAttribute("explicit", true);
					annotatedPredicate.setAttribute("show", promptValue.getKey());
					
					propMap.put(predResource, annotatedPredicate);
					
					AnnotatedValue<?> annotatedObject = new AnnotatedValue<>(promptValue.getValue());
					//I don't know how to retrieve role and other attributes
//					annotatedObject.setAttribute("role", vf.createLiteral(RDFResourceRolesEnum.undetermined.toString()));
					annotatedObject.setAttribute("explicit", true);
					if (promptValue.getValue() instanceof Literal) {
						annotatedObject.setAttribute("show", NTriplesUtil.toNTriplesString(promptValue.getValue()));
					} else {
						annotatedObject.setAttribute("show", promptValue.getValue().stringValue());
					}
					valueMultiMap.put(predResource, annotatedObject);
				}
				//use a PredicateObjectList (although this is not a pred-obj list, but a "userPrompt"-value) to exploit the serialization
				PredicateObjectsList predicateObjectsList = new PredicateObjectsList(propMap, valueMultiMap);
				rv.put("properties", new PredicateObjectsListSection(predicateObjectsList));
			}
		} finally {
			shutDownCodaCore(codaCore);
		}
		return rv;
	}
	
	/**
	 * Removes a reified resource according to the CustomFormGraph that generated it
	 * @param subject
	 * @param predicate
	 * @param resource
	 * @return
	 * @throws ProjectInconsistentException
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 */
	@STServiceOperation
	@Write
	public void removeReifiedResource(IRI subject, IRI predicate, IRI resource)
			throws ProjectInconsistentException, PRParserException, RDFModelNotSetException {
		
		RepositoryConnection repoConnection = getManagedConnection();
		//remove resource as object in the triple <s, p, o> for the given subject and predicate
		repoConnection.remove(subject, predicate, resource, getWorkingGraph());

		CODACore codaCore = getInitializedCodaCore(repoConnection);
		CustomFormGraph cf = getCustomFormGraphSeed(resource, predicate, codaCore);
		if (cf == null) { //
			/* If property hasn't a CustomForm simply delete all triples where resource occurs.
			 * note: this case should never be verified cause this service should be called only 
			 * when the predicate has a CustomForm */
			String query = "delete where { "
					+ "<" + resource.stringValue() + "> ?p1 ?o1 . "
					+ "?s2 ?p2 <" + resource.stringValue() + "> "
					+ "}";
			Update update = repoConnection.prepareUpdate(query);
			update.setIncludeInferred(false);
			update.execute();
		} else { //otherwise remove with a SPARQL delete the graph defined by the CustomFormGraph
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append("delete { ");
			queryBuilder.append(cf.getGraphSectionAsString(codaCore, false));
			queryBuilder.append(" } where { ");
			queryBuilder.append(cf.getGraphSectionAsString(codaCore, true));
			queryBuilder.append(" }");
			
			Update update = repoConnection.prepareUpdate(queryBuilder.toString());
			update.setBinding(cf.getEntryPointPlaceholder(codaCore).substring(1), resource);
			update.setIncludeInferred(false);
			update.execute();
		}
		shutDownCodaCore(codaCore);
	}
	
	/**
	 * Returns the CustomFormGraph that probably generated the reified resource.
	 * If for the given resource there is no CustomForm available returns null,
	 * if there's just one CustomForm then return it, otherwise if there are multiple CustomForm returns the one 
	 * which its PEARL fits better the given reified resource description. 
	 * @param resource
	 * @param predicate
	 * @param codaCore
	 * @return
	 * @throws RDFModelNotSetException
	 */
	private CustomFormGraph getCustomFormGraphSeed(Resource resource, IRI predicate, CODACore codaCore)
			throws RDFModelNotSetException {
		Collection<CustomFormGraph> cForms = cfManager.getAllCustomFormGraphs(getProject(), predicate);
		if (cForms.isEmpty()){
			return null;
		} else if (cForms.size() == 1){
			return cForms.iterator().next();
		} else { //cForms.size() > 1
			//return the CF whose graph section matches more triples in the model
			RepositoryConnection repoConnection = getManagedConnection();
			
			int maxStats = 0;
			CustomFormGraph bestCF = null;
			for (CustomFormGraph cf : cForms) {
				try {
					//creating the construct query
					StringBuilder queryBuilder = new StringBuilder();
					queryBuilder.append("construct { ");
					queryBuilder.append(cf.getGraphSectionAsString(codaCore, false));
					queryBuilder.append(" } where { ");
					queryBuilder.append(cf.getGraphSectionAsString(codaCore, true));
					queryBuilder.append(" }");
					String query = queryBuilder.toString();
					GraphQuery gq = repoConnection.prepareGraphQuery(query);
					gq.setBinding(cf.getEntryPointPlaceholder(codaCore).substring(1), resource);
					gq.setIncludeInferred(false);
					int nStats = QueryResults.asModel(gq.evaluate()).size();
					if (nStats > maxStats) {
						maxStats = nStats;
						bestCF = cf;
					}
				} catch (PRParserException e) {
					//if one of the CF contains an error, catch the exception and continue checking the other CFs
					System.out.println("Parsing error in PEARL rule of CustomForm with ID " + cf.getId() + ". "
							+ "The CustomForm will be ignored, please fix its PEARL rule.");
				}
			}
			return bestCF;
		}
	}
	
	
	@STServiceOperation
	@Read
	public JsonNode executeURIConverter(String converter, @Optional String value) throws ComponentProvisioningException, 
			ConverterException, ProjectInconsistentException {
		String result = "";
		CODACore codaCore = getInitializedCodaCore(getManagedConnection());
		if (value != null){
			result = codaCore.executeURIConverter(converter, value).stringValue();
		} else {
			result = codaCore.executeURIConverter(converter).stringValue();
		}
		shutDownCodaCore(codaCore);
		return JsonNodeFactory.instance.textNode(result);
	}
	
	@STServiceOperation
	@Read
	public JsonNode executeLiteralConverter(String converter, String value, @Optional String datatype, @Optional String lang) 
			throws ComponentProvisioningException, ConverterException, ProjectInconsistentException {
		CODACore codaCore = getInitializedCodaCore(getManagedConnection());
		if (datatype != null && datatype.equals(""))
			datatype = null;
		if (lang != null && lang.equals(""))
			lang = null;
		String result = codaCore.executeLiteralConverter(converter, value, datatype, lang).stringValue();
		shutDownCodaCore(codaCore);
		
		return JsonNodeFactory.instance.textNode(result);
	}
	
	/*
	 * Custom Form mechanism management services
	 */

	//========== Form Collection =================
	
	/**
	 * Returns the serialization of the FormCollection with the given id
	 * This serialization doesn't contain the effective forms, it contains just the id(s) of the forms.
	 * @param id
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public JsonNode getFormCollection(String id) throws CustomFormException{
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode formCollNode = jsonFactory.objectNode();
		FormCollection formColl = cfManager.getFormCollection(getProject(), id);
		if (formColl != null) {
			formCollNode.set("id", jsonFactory.textNode(formColl.getId()));
			ArrayNode formsColl = jsonFactory.arrayNode();
			for (CustomForm form : formColl.getForms()) {
				ObjectNode formNode = jsonFactory.objectNode();
				formNode.set("id", jsonFactory.textNode(form.getId()));
				formNode.set("level", jsonFactory.textNode(form.getLevel().toString()));
				formsColl.add(formNode);
			}
			formCollNode.set("forms", formsColl);
			return formCollNode;
		} else {
			throw new CustomFormException("FormCollection with id " + id + " not found");
		}
	}
	
	/**
	 * Returns all the FormCollections IDs available at system and (current) project level
	 * @return
	 */
	@STServiceOperation
	public JsonNode getAllFormCollections(){
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode formCollArray = jsonFactory.arrayNode();
		Collection<FormCollection> formCollections = cfManager.getFormCollections(getProject());
		//form collections at system level
		for (FormCollection formColl : formCollections){
			ObjectNode formCollNode = jsonFactory.objectNode();
			formCollNode.set("id", jsonFactory.textNode(formColl.getId()));
			formCollNode.set("level", jsonFactory.textNode(formColl.getLevel().toString()));
			formCollArray.add(formCollNode);
		}
		return formCollArray;
	}
	
	/**
	 * Creates an empty FormCollection (without CustomForms related)
	 * @param id
	 * @return
	 * @throws DuplicateIdException 
	 */
	@STServiceOperation
	public void createFormCollection(String id) throws DuplicateIdException {
		cfManager.createFormCollection(getProject(), id);
	}
	
	/**
	 * Creates a new FormCollection cloning an existing one
	 * @param sourceId id of the FC to clone
	 * @param targetId id of the FC to create
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public void cloneFormCollection(String sourceId, String targetId) throws CustomFormException {
		//look for FC at project level
		FormCollection sourceFC = cfManager.getFormCollection(getProject(), sourceId);
		if (sourceFC == null) { //if doesn't exist, look for it at system level
			throw new CustomFormException("Impossible to clone '" + sourceId + "'. A FormCollection with this ID doesn't exists");
		}
		FormCollection newFC = cfManager.createFormCollection(getProject(), targetId);
		cfManager.addFormsToCollection(getProject(), newFC, sourceFC.getForms());
	}
	
	/**
	 * Exports the {@link FormCollection} with the given id
	 * @param oRes
	 * @param id
	 * @throws CustomFormException
	 * @throws IOException
	 */
	@STServiceOperation
	public void exportFormCollection(HttpServletResponse oRes, String id) throws CustomFormException, IOException {
		FormCollection formCollection = cfManager.getFormCollection(getProject(), id);
		if (formCollection == null) {
			throw new CustomFormException("Impossible to export '" + id + "'. A FormCollection with this ID doesn't exists");
		}
		File tempServerFile = File.createTempFile("cfExport", ".xml");
		try {
			formCollection.save(tempServerFile);
			oRes.setHeader("Content-Disposition", "attachment; filename=" + id + ".xml");
			oRes.setContentType("application/xml");
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}
	
	/**
	 * Imports a {@link FormCollection}
	 * @param newId id of the new FormCollection that will be created. If not provided, the created FC will have the same
	 * ID of the loaded FC
	 * @throws IOException 
	 * @throws CustomFormException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void importFormCollection(MultipartFile inputFile, @Optional String newId) throws IOException, CustomFormException {
		// create a temp file (in karaf data/temp folder) to copy the received file
		File tempServerFile = File.createTempFile("cfImport", inputFile.getOriginalFilename());
		try {
			inputFile.transferTo(tempServerFile);
			try {
				FormCollection parsedFormColl = CustomFormXMLHelper.parseAndCreateFormCollection(
						tempServerFile, cfManager.getCustomForms(getProject()), CustomFormLevel.project);
				String newFormCollId;
				if (newId != null) {
					if (!newId.startsWith(FormCollection.PREFIX) || newId.contains(" ") || newId.trim().isEmpty()) { //check if ID is valid
						throw new CustomFormException("The provided ID '" + newId + "' is not valid."
								+ " It must begin with the prefix '" + FormCollection.PREFIX + "' and it must not contain whitespaces");
					}
					newFormCollId = newId;
				} else {
					newFormCollId = parsedFormColl.getId();
				}
				FormCollection newFormColl = cfManager.createFormCollection(getProject(), newFormCollId);
				cfManager.addFormsToCollection(getProject(), newFormColl, parsedFormColl.getForms());
			} catch (CustomFormParseException e) {
				throw new CustomFormException("Failed to parse the input file, it may contain some errors.");
			}
		} finally {
			tempServerFile.delete();
		}
	}
	
	/**
	 * Deletes the FormCollection with the given id
	 * @param id
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public void deleteFormCollection(String id) throws CustomFormException {
		FormCollection formColl = cfManager.getProjectFormCollection(getProject(),id);
		if (formColl == null) {
			throw new CustomFormException("FormCollection with ID '" + id + "' doesn't exist.");
		} 
		cfManager.deleteFormCollection(getProject(), formColl);
	}
	
	/**
	 * Adds an existing CustomForm to an existing FormCollection
	 * @param formCollectionId
	 * @param customFormId
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	public void addFormToCollection(String formCollectionId, String customFormId) throws CustomFormException{
		FormCollection formColl = cfManager.getProjectFormCollection(getProject(), formCollectionId);
		if (formColl == null) {
			throw new CustomFormException("FormCollection with ID " + formCollectionId + " doesn't exist");
		}
		CustomForm cf = cfManager.getCustomForm(getProject(), customFormId);
		if (cf == null) {
			throw new CustomFormException("CustomForm with ID " + customFormId + " doesn't exist");
		}
		cfManager.addFormToCollection(getProject(), formColl, cf);
	}
	
	/**
	 * Removes a CustomForm from an existing FormCollection
	 * @param formCollectionId
	 * @param customFormId
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	public void removeFormFromCollection(String formCollectionId, String customFormId) throws CustomFormException {
		FormCollection formColl = cfManager.getProjectFormCollection(getProject(), formCollectionId);
		if (formColl == null) {
			throw new CustomFormException("FormCollection with ID " + formCollectionId + " doesn't exist");
		}
		CustomForm cf = cfManager.getCustomForm(getProject(), customFormId);
		if (cf == null) {
			throw new CustomFormException("CustomForm with ID " + customFormId + " doesn't exist");
		}
		cfManager.removeFormFromCollection(getProject(), formColl, cf);
	}
	
	//========== Custom Form =================
	
	/**
	 * Returns all the CustomForms available
	 * @return
	 */
	@STServiceOperation
	public JsonNode getAllCustomForms(){
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode customFormArrayNode = jsonFactory.arrayNode();
		Collection<CustomForm> cForms = cfManager.getCustomForms(getProject());
		for (CustomForm cf : cForms){
			ObjectNode customFormNode = jsonFactory.objectNode();
			customFormNode.set("id", jsonFactory.textNode(cf.getId()));
			customFormNode.set("level", jsonFactory.textNode(cf.getLevel().toString()));
			customFormArrayNode.add(customFormNode);
		}
		return customFormArrayNode;
	}
	
	/**
	 * Returns the serialization of the CurtomForm with the given id
	 * @param id
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public JsonNode getCustomForm(String id) throws CustomFormException{
		CustomForm cf = cfManager.getCustomForm(getProject(), id); //at project level
		if (cf != null){
			JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
			
			ObjectNode customFormNode = jsonFactory.objectNode();
			customFormNode.set("id", jsonFactory.textNode(cf.getId()));
			customFormNode.set("name", jsonFactory.textNode(cf.getName()));
			customFormNode.set("type", jsonFactory.textNode(cf.getType()));
			customFormNode.set("description", jsonFactory.textNode(cf.getDescription()));
			customFormNode.set("ref", jsonFactory.textNode(cf.getRef()));
			if (cf.isTypeGraph()){
				String propertyChain = cf.asCustomFormGraph().serializePropertyChain();
				if (!propertyChain.isEmpty()) {
					customFormNode.set("showPropertyChain", jsonFactory.textNode(propertyChain));
				}
			}
			return customFormNode;
		} else {
			throw new CustomFormException("CurtomForm with id " + id + " not found in project " + getProject().getName());
		}
	}
	
	/**
	 * Returns a serialization representing the form of the CurtomForm with the given id
	 * @param id
	 * @return
	 * @throws UnavailableResourceException
	 * @throws ProjectInconsistentException
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 * @throws ModelAccessException 
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	@Read
	public JsonNode getCustomFormRepresentation(String id) throws 
			ProjectInconsistentException, PRParserException, RDFModelNotSetException,  CustomFormException {
		CustomForm cForm = cfManager.getCustomForm(getProject(), id);
		
		if (cForm != null) {
			CODACore codaCore = getInitializedCodaCore(getManagedConnection());
			Collection<UserPromptStruct> form = cForm.getForm(codaCore);
			shutDownCodaCore(codaCore);
			if (!form.isEmpty()) {
				JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
				
				ArrayNode formArrayNode = jsonFactory.arrayNode();
				
				for (UserPromptStruct formEntry : form){
					ObjectNode formEntryNode = jsonFactory.objectNode();
					formEntryNode.set("placeholderId", jsonFactory.textNode(formEntry.getPlaceholderId()));
					formEntryNode.set("userPrompt", jsonFactory.textNode(formEntry.getUserPromptName()));
					formEntryNode.set("type", jsonFactory.textNode(formEntry.getRdfType()));
					formEntryNode.set("mandatory", jsonFactory.booleanNode(formEntry.isMandatory()));
					
					ConverterMention converter = formEntry.getConverter();
					ObjectNode converterNode = jsonFactory.objectNode();
					converterNode.set("uri", jsonFactory.textNode(converter.getURI()));
					
					// for special case langString, specify the converter argument too
					System.out.println("argPhId " + formEntry.getConverterArgPhId());
					if (formEntry.getConverterArgPhId() != null) {
						String phLangId = formEntry.getConverterArgPhId();
						
						System.out.println("phLangId " + phLangId);
						/*
						 * the language placeholder (arguments of langString converter) is already added to
						 * the xml as formEntry element since in PEARL it must be defined before it's used as
						 * argument, so remove the element from formEntry and add it as argument of converter
						 * xml element
						 */
						for (JsonNode entry : formArrayNode) {
							System.out.println("placeholderId " + entry.get("placeholderId"));
							if (entry.get("placeholderId").textValue().equals(phLangId)) {
								ObjectNode convArgNode = jsonFactory.objectNode();
								convArgNode.set("userPrompt", jsonFactory.textNode(entry.get("userPrompt").textValue()));
								converterNode.set("arg", convArgNode);
								break;
							}
						}
					}
					formEntryNode.set("converter", converterNode);
					
					if (formEntry.isLiteral()){
						if (formEntry.hasDatatype()) {
							formEntryNode.set("datatype", jsonFactory.textNode(formEntry.getLiteralDatatype()));
						}
						if (formEntry.hasLanguage()) {
							formEntryNode.set("lang", jsonFactory.textNode(formEntry.getLiteralLang()));
						}
					}
					
					formArrayNode.add(formEntryNode);
				}
				return formArrayNode;
			} else {
				throw new CustomFormException("No userPrompt/ features found in CustomForm " + id);
			}
			
		} else {
			throw new CustomFormException("CustomForm with id " + id + " not found in project " + getProject().getName());
		}
	}
	
	/**
	 * This service is thought to create a custom form from client.
	 * To be useful is necessary a proper UI for client support (e.g. a wizard)
	 * This is a POST because ref and description could be quite long for a GET parameter
	 * @param type
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @param showProp Useful only if type is "graph"
	 * @return
	 * @throws DuplicateIdException 
	 */
	@STServiceOperation (method = RequestMethod.POST)
	public void createCustomForm(String type, String id, String name, String description, String ref, @Optional List<IRI> showPropChain)
			throws DuplicateIdException {
		//avoid proliferation of new line in saved pearl (carriage return character "\r" are added to ref when calling this service
		cfManager.createCustomForm(getProject(), type, id, name, description, ref, showPropChain);
	}
	
	/**
	 * Creates a new CustomForm cloning an existing one
	 * @param sourceId id of the CF to clone
	 * @param targetId id of the CF to create
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public void cloneCustomForm(String sourceId, String targetId) throws CustomFormException {
		CustomForm sourceCF = cfManager.getCustomForm(getProject(), sourceId);
		if (sourceCF == null) {
			throw new CustomFormException("Impossible to clone '" + sourceId + "'. A CustomForm with this ID doesn't exists");
		}
		if (sourceCF.isTypeGraph()) {
			String ref = sourceCF.getRef();
			//replace in pearl rule the namespace
			ref = ref.replace(sourceId, targetId);
			//and the rule ID
			String sourceRuleId = sourceId.replace(CustomForm.PREFIX, "id:");
			String targetRuleId = targetId.replace(CustomForm.PREFIX, "id:");
			ref = ref.replace(sourceRuleId, targetRuleId);
			cfManager.createCustomForm(getProject(), sourceCF.getType(), targetId, sourceCF.getName(), sourceCF.getDescription(),
					ref, sourceCF.asCustomFormGraph().getShowPropertyChain());
		} else { //type "node"
			cfManager.createCustomForm(getProject(), sourceCF.getType(), targetId, sourceCF.getName(), sourceCF.getDescription(),
					sourceCF.getRef(), null);
		}
	}
	
	/**
	 * Exports the {@link CustomForm} with the given id
	 * @param oRes
	 * @param id
	 * @throws CustomFormException
	 * @throws IOException
	 */
	@STServiceOperation
	public void exportCustomForm(HttpServletResponse oRes, String id) throws CustomFormException, IOException {
		CustomForm customForm = cfManager.getCustomForm(getProject(), id);
		if (customForm == null) {
			throw new CustomFormException("Impossible to export '" + id + "'. A CustomForm with this ID doesn't exists");
		}
		File tempServerFile = File.createTempFile("cfExport", ".xml");
		try {
			customForm.save(tempServerFile);
			oRes.setHeader("Content-Disposition", "attachment; filename=" + id + ".xml");
			oRes.setContentType("application/xml");
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}
	
	/**
	 * Imports a {@link CustomForm}
	 * @param newId id of the new CustomForm that will be created. If not provided, the created CF will have the same
	 * ID of the loaded CF
	 * @throws IOException 
	 * @throws CustomFormException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	public void importCustomForm(MultipartFile inputFile, @Optional String newId) throws IOException, CustomFormException {
		// create a temp file (in karaf data/temp folder) to copy the received file
		File tempServerFile = File.createTempFile("cfImport", inputFile.getOriginalFilename());
		try {
			inputFile.transferTo(tempServerFile);
			try {
				CustomForm parsedCustomForm = CustomFormXMLHelper.parseAndCreateCustomForm(tempServerFile);
				String newCustomFormId;
				String newRef;
				if (newId != null) {
					if (!newId.startsWith(CustomForm.PREFIX) || newId.contains(" ") || newId.trim().isEmpty()) { //check if ID is valid
						throw new CustomFormException("The provided ID '" + newId + "' is not valid."
								+ " It must begin with the prefix '" + CustomForm.PREFIX + "' and it must not contain whitespaces");
					}
					newCustomFormId = newId;
					if (parsedCustomForm.isTypeGraph()) {
						newRef = parsedCustomForm.getRef();
						//replace in pearl rule the id
						newRef = newRef.replace(parsedCustomForm.getId(), newCustomFormId);
						//and the rule ID
						String sourceRuleId = parsedCustomForm.getId().replace(CustomForm.PREFIX, "id:");
						String targetRuleId = newCustomFormId.replace(CustomForm.PREFIX, "id:");
						newRef = newRef.replace(sourceRuleId, targetRuleId);
					} else { //node
						newRef = parsedCustomForm.getRef();
					}
				} else { //keep the old id
					newCustomFormId = parsedCustomForm.getId();
					newRef = parsedCustomForm.getRef();
				}
				if (parsedCustomForm.isTypeGraph()) {
					cfManager.createCustomForm(getProject(), parsedCustomForm.getType(), newCustomFormId,
							parsedCustomForm.getName(), parsedCustomForm.getDescription(),
							newRef, parsedCustomForm.asCustomFormGraph().getShowPropertyChain());
				} else { //type "node"
					cfManager.createCustomForm(getProject(), parsedCustomForm.getType(), newCustomFormId,
							parsedCustomForm.getName(), parsedCustomForm.getDescription(), newRef, null);
				}
			} catch (CustomFormParseException e) {
				throw new CustomFormException("Failed to parse the input file, it may contain some errors.");
			}
		} finally {
			tempServerFile.delete();
		}
	}
	
	/**
	 * Deletes the CustomForm with the given id. Removes also the CustomForm from the FormCollection(s)
	 * that contain it. 
	 * @param id
	 * @param deleteEmptyColl if true deletes FormCollection that are left empty after the deletion
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public void deleteCustomForm(String id, @Optional (defaultValue = "false") boolean deleteEmptyColl) throws CustomFormException {
		CustomForm cf = cfManager.getProjectCustomForm(getProject(), id);
		if (cf == null) {
			throw new CustomFormException("CustomForm with id " + id + " not found in project " + getProject().getName());
		}
		cfManager.deleteCustomForm(getProject(), cf, deleteEmptyColl);
	}
	
	/**
	 * Given the id of a CustomForm tells if it belong to a FormCollection. Useful as pre-check when it's deleting
	 * a CustomForm
	 * @param id
	 * @return
	 */
	@STServiceOperation
	public JsonNode isFormLinkedToCollection(String id) {
		//Since this is used before to delete a CustomForm, look only at project level, since is not possible to delete at system level
		Collection<FormCollection> formCollections = cfManager.getProjectFormCollections(getProject());
		for (FormCollection formColl: formCollections) {
			if (formColl.getFormsId().contains(id)) {
				return JsonNodeFactory.instance.booleanNode(true);
			}
		}
		return JsonNodeFactory.instance.booleanNode(false);
	}
	
	/**
	 * Updates a CustomForm. Allows to change the name, the description, the ref and eventually
	 * (if the CustomForm is graph) the showProp of the CustomForm with the given id. It doesn't allow to change 
	 * the type.
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @param showProp
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation (method = RequestMethod.POST)
	public void updateCustomForm(String id, String name, String description, String ref, @Optional List<IRI> showPropChain) throws CustomFormException {
		CustomForm cf = cfManager.getProjectCustomForm(getProject(), id);
		if (cf == null) {
			throw new CustomFormException("CustomForm with id " + id + " not found in project " + getProject().getName());
		}
		//avoid proliferation of new line in saved pearl (carriage return character "\r" are added to ref when calling this service
		ref = ref.replace("\r", "");
		cfManager.updateCustomForm(getProject(), cf, name, description, ref, showPropChain);
	}
	
	/**
	 * Used to check if a property chain (manually created by the user client-side) is correct.
	 * @param propChain
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public void validateShowPropertyChain(String propChain) throws CustomFormException {
		String[] splitted = propChain.split(",");
		for (String s : splitted) {
			System.out.println("prop " + s);
			if (!URIUtil.isValidURIReference(s.trim())) {
				throw new CustomFormException("'" + s + "' is not a valid URI");
			}
			else {
				System.out.println("valid");
			}
		}
	}
	
	/**
	 * Tries to validate a pearl code.
	 * @param pearl rule to be parsed, it should be a whole pearl rule if the CustomForm is type graph
     * or a converter if type node
	 * @param formType tells if the CF is type "node" or "graph".
     * Determines also the nature of the pearl parameter
	 * @return
	 * @throws ProjectInconsistentException 
	 * @throws UnavailableResourceException 
	 * @throws ModelAccessException 
	 * @throws RDFModelNotSetException 
	 * @throws CustomFormException 
	 */
	@STServiceOperation (method = RequestMethod.POST)
	public JsonNode validatePearl(String pearl, String formType) throws ProjectInconsistentException, RDFModelNotSetException, CustomFormException {
		if (formType.equals(CustomForm.Types.graph.toString())) {
			try {
				InputStream pearlStream = new ByteArrayInputStream(pearl.getBytes(StandardCharsets.UTF_8));
				CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
				codaCore.setProjectionRulesModelAndParseIt(pearlStream);
				//setProjectionRulesModelAndParseIt didn't throw exception, so pearl is valid
				return JsonNodeFactory.instance.booleanNode(true);
			} catch (PRParserException e) {
				throw new CustomFormException("Invalid pearl rule: " + e.getErrorAsString());
			} catch (AntlrParserRuntimeException e) {
				throw new CustomFormException("Invalid pearl rule: " + e.getMsg());
			}
		} else { //type node
			try {
				//Treat separately the case where pearl is simply "literal", because parser.projectionOperator()
				//called in createProjectionOperatorTree()
				//inexplicably throws an exception (no viable alternative at input <EOF>)
				if (pearl.equals("literal")) {
					return JsonNodeFactory.instance.booleanNode(true);
				}
				CustomFormParseUtils.createProjectionOperatorTree(pearl);
				//parser didn't throw exception, so pearl is valid
				return JsonNodeFactory.instance.booleanNode(true);
			} catch (RecognitionException e) {
				throw new CustomFormException("Invalid projection operator");
			} catch (AntlrParserRuntimeException e) {
				throw new CustomFormException("Invalid projection operator: " + e.getMsg());
			}
		}
	}
	
	//============== Forms Mapping ===================
	
	/**
	 * Returns all the resource-FormCollections mapping defined in the CustomForm configuration of the project
	 * @return
	 */
	@STServiceOperation
	public JsonNode getCustomFormConfigMap() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode cfcArrayNode = jsonFactory.arrayNode();
		Collection<FormsMapping> formMappings = cfManager.getProjectFormMappings(getProject());
		for (FormsMapping mapping : formMappings) {
			ObjectNode cfcNode = jsonFactory.objectNode();
			cfcNode.set("resource", jsonFactory.textNode(mapping.getResource()));
			
			ObjectNode formCollNode = jsonFactory.objectNode();
			formCollNode.set("id", jsonFactory.textNode(mapping.getFormCollection().getId()));
			formCollNode.set("level", jsonFactory.textNode(mapping.getFormCollection().getLevel().toString()));
			cfcNode.set("formCollection", formCollNode);

			cfcNode.set("replace", jsonFactory.booleanNode(mapping.getReplace()));
			cfcArrayNode.add(cfcNode);
		}
		return cfcArrayNode;
	}
	
	/**
	 * Adds a FormCollection to a resource, creating a FormsMapping
	 * @param resource
	 * @param formCollId
	 * @param replace
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	public void addFormsMapping(IRI resource, String formCollId,
			@Optional (defaultValue = "false") boolean replace) throws CustomFormException{
		FormCollection formColl = cfManager.getFormCollection(getProject(), formCollId);
		if (formColl == null) {
			throw new CustomFormException("FormCollection with ID " + formCollId + " doesn't exist");
		}
		cfManager.addFormsMapping(getProject(), resource, formColl, replace);
	}
	
	/**
	 * Remove the mapping between the given resource and the FormCollection linked to it
	 * @param resource
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public void removeFormCollectionOfResource(IRI resource) throws CustomFormException{
		cfManager.removeFormsMapping(getProject(), resource);
	}
	
	/**
	 * Update the replace attribute of a FormCollectionMapping mapping for the given property
	 * @param resource
	 * @param replace
	 * @return
	 * @throws CustomFormException 
	 */
	@STServiceOperation
	public void updateReplace(IRI resource, boolean replace) throws CustomFormException {
		cfManager.setReplace(getProject(), resource, replace);
	}
	
	/*
	 * Utilities
	 */
	
	private CODACore getInitializedCodaCore(RepositoryConnection repoConnection) throws ProjectInconsistentException{
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		codaCore.initialize(repoConnection);
		return codaCore;
	}
	
	private void shutDownCodaCore(CODACore codaCore) {
		codaCore.setRepositoryConnection(null);
		codaCore.stopAndClose();
	}

}
