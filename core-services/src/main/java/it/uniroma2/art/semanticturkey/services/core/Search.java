package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.query.QueryStringUtil;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.sparql.SPARQLParameterizationStore;
import it.uniroma2.art.semanticturkey.config.sparql.SPARQLStore;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLOperation;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLParameterization;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLParameterization.ConstraintVariableBinding;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLParameterization.VariableBinding;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy.StatusFilter;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.TripleForSearch;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.search.ServiceForSearches;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.JsonSerialized;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.FormRenderer;
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexicalEntryRenderer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

import javax.validation.constraints.Min;

@STService
public class Search extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(Search.class);

	// private static String CLASS_ROLE = "class";
	// private static String CONCEPT_ROLE = "concept";
	// private static String INSTANCE_ROLE = "instance";

	//@formatter:off
	/*protected SearchStrategy instantiateSearchStrategy() {
		SearchStrategies searchStrategy = STRepositoryInfoUtils
				.getSearchStrategy(getProject().getRepositoryManager()
						.getSTRepositoryInfo(STServiceContextUtils.getRepostoryId(stServiceContext)));

		return SearchStrategyUtils.instantiateSearchStrategy(searchStrategy);
	}*/
	//@formatter:on

	/**
	 * Creates the Indexes. Only useful in GraphDB repositories
	 * @throws Exception
	 */
	@STServiceOperation
	@Write
	// TODO decide the @PreAuthorize
	// #@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'w')")
	public void createIndexes() throws Exception {
		ValidationUtilities.executeWithoutValidation(
				ValidationUtilities.isValidationEnabled(stServiceContext), getManagedConnection(), conn -> {
					instantiateSearchStrategy().initialize(conn, true);
				});
	}

	/**
	 * Updates the indexes. Only useful in GraphDB repositories
	 * @throws Exception
	 */
	@STServiceOperation
	@Write
	// TODO decide the @PreAuthorize
	// #@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'w')")
	public void updateIndexes() throws Exception {
		ValidationUtilities.executeWithoutValidation(
				ValidationUtilities.isValidationEnabled(stServiceContext), getManagedConnection(), conn -> {
					instantiateSearchStrategy().update(getManagedConnection());
				});
	}

	/**
	 * Perform a custom search by passing a SPARQL query and a map of variable-value to assign to such
	 * SPARQL query
	 * @param searchParameterizationReference the SPARQL query to execute
	 * @param boundValues the map of variable-value to assign to the the SPARQL query
	 * @return The list of AnnotatedValue<Resource> with all the infos about the desired resources
	 * @throws IOException
	 * @throws ConfigurationNotFoundException
	 * @throws WrongPropertiesException
	 * @throws NoSuchConfigurationManager
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation(method = RequestMethod.GET)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> customSearch(String searchParameterizationReference,
			@JsonSerialized Map<String, Value> boundValues)
			throws IOException, ConfigurationNotFoundException, WrongPropertiesException,
			NoSuchConfigurationManager, STPropertyAccessException {
		StoredSPARQLParameterization storedSparqlParameterization = (StoredSPARQLParameterization) exptManager
				.getConfiguration(SPARQLParameterizationStore.class.getName(),
						parseReference(searchParameterizationReference));
		String relativeReference2quey = storedSparqlParameterization.relativeReference;
		Map<String, VariableBinding> variableBindings = storedSparqlParameterization.variableBindings;
		Set<String> variablesRequiringValues = variableBindings.entrySet().stream()
				.filter(entry -> entry.getValue() instanceof ConstraintVariableBinding).map(Map.Entry::getKey)
				.collect(toSet());

		Reference reference2query = parseReference(relativeReference2quey);
		StoredSPARQLOperation storedQuery = (StoredSPARQLOperation) exptManager
				.getConfiguration(SPARQLStore.class.getName(), reference2query);

		Set<String> boundVariables = boundValues.keySet();

		Set<String> boundVariablesNotRequiringValue = Sets.difference(boundVariables,
				variablesRequiringValues);

//		if (!boundVariablesNotRequiringValue.isEmpty()) {
//			throw new IllegalArgumentException(
//					"It has been provided a value for variables that do not require one: "
//							+ boundVariablesNotRequiringValue);
//		}

		Set<String> valueMissingVariables = Sets.difference(variablesRequiringValues, boundVariables);

		if (!valueMissingVariables.isEmpty()) {
			throw new IllegalArgumentException(
					"It has not been provided a value for some variables requiring one: "
							+ valueMissingVariables);
		}

		String queryString = storedQuery.sparql;
		boolean includeInferred = storedQuery.includeInferred;

		String queryStringWithoutProlog = QueryParserUtil.removeSPARQLQueryProlog(queryString);
		String queryProlog = queryString.substring(0, queryString.indexOf(queryStringWithoutProlog));

		// skos, owl, skosxl, rdfs, rdf

		StringBuilder newQueryPrologBuilder = new StringBuilder(queryProlog);

		// add prefixes required by the nature computation pattern
		for (Namespace ns : Arrays.asList(SKOS.NS, org.eclipse.rdf4j.model.vocabulary.SKOSXL.NS, RDF.NS,
				RDFS.NS, OWL.NS)) {
			if (queryProlog.indexOf(ns.getPrefix() + ":") == -1) {
				newQueryPrologBuilder.append("prefix " + ns.getPrefix() + ":");
				RenderUtils.toSPARQL(SimpleValueFactory.getInstance().createIRI(ns.getName()),
						newQueryPrologBuilder);
				newQueryPrologBuilder.append("\n");
			}
		}

		MapBindingSet bindingSet = new MapBindingSet();
		boundValues.forEach(bindingSet::addBinding);
		String groundQueryStringWithoutProlog = QueryStringUtil.getTupleQueryString(queryStringWithoutProlog,
				bindingSet);

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, queryString,
				null);
		Set<String> returnedBindingNames = parsedQuery.getTupleExpr().getBindingNames();

		if (returnedBindingNames.size() != 1) {
			throw new IllegalArgumentException("The parameterized query does not return a variable only");
		}

		String resourceVariableName = returnedBindingNames.iterator().next();

		QueryBuilder qb = createQueryBuilder(newQueryPrologBuilder.toString() + "\nSELECT DISTINCT ?"
				+ resourceVariableName + " " + generateNatureSPARQLSelectPart() + " WHERE {{"
				+ groundQueryStringWithoutProlog + "}\n" + generateNatureSPARQLWherePart(resourceVariableName)
				+ "} GROUP BY ?" + resourceVariableName + " ");
		qb.setIncludeInferred(includeInferred);
		qb.setResourceVariable(resourceVariableName);
		qb.processRendering();
		qb.processQName();

		return qb.runQuery();
	}


	/**
	 * Perform a complext search with many custom input parameters
	 * If parameter useURI is true and searchMode is startsWith, prefixes in qname (in the searchString) are expanded
	 * @param searchString the text to search
	 * @param useLexicalizations true to search in the lexicalizations, false otherwise. Optional and default value is true
	 * @param useLocalName true to search in the localName of the resources, false otherwise
	 * @param useURI true to search in the URI of the resources, false otherwise
	 * @param searchMode the searchMode to use during the search. Check {@link it.uniroma2.art.semanticturkey.search.SearchMode} to get the
	 * 	 *                   possible SerchMode
	 * @param useNotes true to search in all the values of the skos:note and its subProperties, false otherwise. Can be set to true only if
	 * 	 *                 the parameter useLocalName is true as well. Optional and default value is false
	 * @param langs the languages of the Literals in which to search the input text. Optional
	 * @param includeLocales true to includes locales in the search
	 * @param statusFilter the status of the desired resources. Check {@link it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy.StatusFilter}
	 *                        to get the possible Statuses
	 * @param types the list of
	 * @param schemes the list of schemes in which the skos:Concept will be searched. Useful only in SKOS/SKOSXL.
	 *               This list is composed of List of Schemes considered in AND (so an OR list of AND schemes)   Optional
	 * @param outgoingLinks a list of outgoing predicate and List of Values for the desired resources
	 * @param outgoingSearch a list of outgoing predicate and a search function (stringSearch and SearchMode)
	 *                          for the object of such predicate    for the desired resources
	 * @param ingoingLinks a list of incoming predicate and List of Values for the desired resources
	 * @param searchInRDFSLabel true to search in rdfs:label, false otherwise. Optional and default value is false
	 * @param searchInSKOSLabel true to search in SKOS lexicalizations, false otherwise. Optional and default value is false
	 * @param searchInSKOSXLLabel true to search in SKOSXL Lericalizations, false otherwise. Optional and default value is false
	 * @param searchInOntolex true to search in ONTOLEX lexicalizations, false otherwise. Optional and default value is false
	 * @return The list of AnnotatedValue<Resource> with all the infos about the desired resources
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 */
	//@formatter:off
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> advancedSearch(@Optional String searchString,
			@Optional(defaultValue = "true") boolean  useLexicalizations,
			@Optional(defaultValue="false") boolean useLocalName, 
			@Optional(defaultValue="false") boolean useURI, 
			SearchMode searchMode,
			@Optional(defaultValue="false") boolean useNotes,
			@Optional List<String> langs, @Optional(defaultValue="false") boolean includeLocales,
			StatusFilter statusFilter,
			@Optional @JsonSerialized List<List<IRI>> types,
			@Optional @JsonSerialized List<List<IRI>> schemes,
			@Optional @JsonSerialized List<Pair<IRI, List<Value>>> outgoingLinks,
			@Optional @JsonSerialized List<TripleForSearch<IRI, String, SearchMode>> outgoingSearch,
			@Optional @JsonSerialized List<Pair<IRI, List<Value>>> ingoingLinks,
			@Optional(defaultValue="false") boolean searchInRDFSLabel,
			@Optional(defaultValue="false") boolean searchInSKOSLabel,
			@Optional(defaultValue="false") boolean searchInSKOSXLLabel,
			@Optional(defaultValue="false") boolean searchInOntolex) 
					throws IllegalStateException, STPropertyAccessException {
		IRI lexModel = getProject().getLexicalizationModel();
		if(lexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
			searchInRDFSLabel = true;
		}
		if (!ValidationUtilities.isValidationEnabled(stServiceContext)) {
			if (statusFilter == StatusFilter.UNDER_VALIDATION
					|| statusFilter == StatusFilter.UNDER_VALIDATION_FOR_DEPRECATION) {
				throw new IllegalArgumentException(
						"Invalid status filter for a project without validation: " + statusFilter);
			}
		}

		//prepare the namespace map
		Map<String, String> prefixToNamespaceMap = getProject().getOntologyManager().getNSPrefixMappings(false);

		String query= ServiceForSearches.getPrefixes() +
				"\nSELECT DISTINCT ?resource ?attr_nature ?attr_scheme" +
				"\nWHERE{" +
				"\n{";
		
		//use the searchInstancesOfClass to construct the first part of the query (the subquery)
		query += instantiateSearchStrategy().searchInstancesOfClass(stServiceContext, types, searchString, useLexicalizations,
				useLocalName, useURI, useNotes, searchMode, langs, includeLocales, true, true, lexModel,
				searchInRDFSLabel, searchInSKOSLabel, searchInSKOSXLLabel, searchInOntolex, schemes, 
				statusFilter, outgoingLinks, outgoingSearch, ingoingLinks, instantiateSearchStrategy(), 
				getProject().getBaseURI(), prefixToNamespaceMap);
		
		
		query+="\n}";

		
		query+= "\nFILTER(BOUND(?resource))" + //used only to not have problem with the OPTIONAL in qb.processRendering(); 
				"\n}" +
			"\nGROUP BY ?resource ?attr_nature ?attr_scheme";
		logger.debug("query = " + query);

		
		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processRendering();
		qb.process(LexicalEntryRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_lexicalEntryRendering");
		qb.process(FormRenderer.INSTANCE_WITHOUT_FALLBACK, "resource", "attr_formRendering");
		
		Collection<AnnotatedValue<Resource>> annotatedValues = qb.runQuery();
		Iterator<AnnotatedValue<Resource>> it = annotatedValues.iterator();
		while (it.hasNext()) {
			fixShowAttribute(it.next());
		}
		return annotatedValues;
	}
	//@formatter:on
	
	private void fixShowAttribute(AnnotatedValue<Resource> annotatedResource) {
		Map<String, Value> attrs = annotatedResource.getAttributes();
		Literal lexicalEntryRendering = (Literal) attrs.remove("lexicalEntryRendering");
		Literal formRendering = (Literal) attrs.remove("formRendering");

		if (lexicalEntryRendering != null) {
			attrs.put("show", lexicalEntryRendering);
		} else {
			if (formRendering != null) {
				attrs.put("show", formRendering);
			}
		}
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchAlignedResources(
					String searchString,
					boolean useLocalName,
					boolean useURI,
					SearchMode searchMode,
					@Optional(defaultValue = "false") boolean useNotes,
					@Optional List<String> langs,
					@Optional(defaultValue = "false") boolean includeLocales,
					@Optional List<IRI> predList,
					@Optional (defaultValue = "30") int maxNumOfResPerQuery) throws STPropertyAccessException {

		// do a search on the ct_project to get the list of annotated value
		IRI lexModel = getProject().getLexicalizationModel();

		// prepare the namespace map
		Map <String, String> prefixToNamespaceMap = getProject().getOntologyManager().getNSPrefixMappings(false);

		// prepare the query
		String query = ServiceForSearches.getPrefixes() + "\n"
				+ instantiateSearchStrategy().searchResource(stServiceContext, searchString, null, true,
				useLocalName, useURI, useNotes, searchMode, null, "or", langs, includeLocales, lexModel,
				false, false, false, false, prefixToNamespaceMap);

		logger.debug("query = " + query);

		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processQName();
		qb.processRendering();
		//execute the query and save the result in initialAnnValueList (the final result of this service will be a
		// subset of this Collection)
		Collection<AnnotatedValue<Resource>> initialAnnValueList = qb.runQuery();

		// do a SPARQL query to get all the resource explicitly defined
		Set<IRI> refDefinedSet = new HashSet<>();
		try(RepositoryConnection conn = getProject().getRepository().getConnection()){
			Resource wg = getWorkingGraph();
			query = "SELECT ?resource " +
					"\nWHERE {" +
					"\nGRAPH "+NTriplesUtil.toNTriplesString(wg) +" { "+
					"\n?resource a ?type ." +
					"\n}" +
					"\n}";
			executeQueryOneVar(query, "resource", conn, refDefinedSet);
		}

		// prepare a list of IRI from initialAnnValueList (and filter it using refDefinedSet)
		List<IRI> initialIriResList = new ArrayList<>();
		for(AnnotatedValue<Resource> annotatedValue : initialAnnValueList){
			if(annotatedValue.getValue() instanceof IRI) {
				IRI resource = (IRI) annotatedValue.getValue();
				if(refDefinedSet.contains(resource)) {
					initialIriResList.add((IRI) annotatedValue.getValue());
				}
			}
		}

		// do a query on the consumer project to filter the initialAnnValueList elements as being objects in such dataset
		Project consumerPrj = ProjectManager.getProject(stServiceContext.getProjectConsumer().getName());
		String queryBefore, queryAfter;
		queryBefore = "SELECT ?otherRes "+
				"\nWHERE {"+
				"\nVALUES ?otherRes {";

		String predPart = "";
		if(predList==null || predList.isEmpty()) {
			predPart = "?pred ";
		} else {
			boolean first = true;
			for(IRI pred : predList){
				if(!first){
					predPart += " | ";
				}
				first = false;
				predPart += NTriplesUtil.toNTriplesString(pred);
			}
		}
		queryAfter = " } " +
				"\n?resource "+predPart+" ?otherRes . "+
				"\n}";

		Set<IRI> objResInDatasetSet = new HashSet<>();
		List<IRI> reducedIriList = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		try(RepositoryConnection conn = consumerPrj.getRepository().getConnection()){
			//prepare the query to execute
			while(!initialIriResList.isEmpty()) {
				reducedIriList.add(initialIriResList.remove(0));
				if(reducedIriList.size()>=maxNumOfResPerQuery){
					//create and execute the query
					for(IRI iri : reducedIriList){
						sb.append(" ").append(NTriplesUtil.toNTriplesString(iri)).append(" ");
					}
					query = queryBefore+sb.toString()+queryAfter;
					executeQueryOneVar(query, "otherRes", conn, objResInDatasetSet);
					//clear reducedIriList
					reducedIriList.clear();
				}
			}
			//if reducedIriList is not empty, then create and execute the query
			for(IRI iri : reducedIriList){
				sb.append(" ").append(NTriplesUtil.toNTriplesString(iri)).append(" ");
			}
			query = queryBefore+sb.toString()+queryAfter;
			executeQueryOneVar(query, "otherRes", conn, objResInDatasetSet);
		}

		// iterate over the initialAnnValueList and keep only the initialAnnValueList being present in objResInDataset
		Collection<AnnotatedValue<Resource>> annotatedValueList = new ArrayList<>();
		for(AnnotatedValue<Resource> annotatedValue : initialAnnValueList){
			Value value = annotatedValue.getValue();
			if(value instanceof  IRI) {
				if(objResInDatasetSet.contains((IRI)value)) {
					annotatedValueList.add(annotatedValue);
				}
			}
		}
		return  annotatedValueList;
	}

	private void executeQueryOneVar(String query, String var, RepositoryConnection conn,
									Collection<IRI> collection){

		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			while(tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				Value value = bindingSet.getValue(var);
				if(value instanceof IRI) {
					collection.add((IRI) value);
				}
			}
		}
	}

	/**
	 * The standard search in all resources. Its input can be a URI (part of it) or a text (part of a Lexicalization and/or notes)
	 * depending on the other input parameters. It returns all relevant info about the desired resources (such as the show).
	 * If parameter useURI is true and searchMode is startsWith, prefixes in qname (in the searchString) are expanded
	 * @param searchString the text to search
	 * @param rolesArray the role(s) the returned resources should belong to
	 * @param useLexicalizations true to search in the lexicalizations, false otherwise. Optional and default value is true
	 * @param useLocalName true to search in the localName of the resources, false otherwise
	 * @param useURI true to search in the URI of the resources, false otherwise
	 * @param searchMode the searchMode to use during the search. Check {@link it.uniroma2.art.semanticturkey.search.SearchMode} to get the
	 *                   possible SerchMode
	 * @param useNotes true to search in all the values of the skos:note and its subProperties, false otherwise. Can be set to true only if
	 *                 the parameter useLocalName is true as well. Optional and default value is false
	 * @param schemes the list of schemes in which the skos:Concept will be searched. Useful only in SKOS/SKOSXL. Optional
	 * @param schemeFilter if the returned concepts should belong to just one of the input sheme ()or value or to all of them (and value).
	 *                     Optional and default value is or
	 * @param langs the languages of the Literals in which to search the input text. Optional
	 * @param includeLocales true to includes locales in the search (e.g. searching in 'en' will include results from 'en-us' as well).
	 *                       Optional and default value is false
	 * @param searchInRDFSLabel true to search in rdfs:label, false otherwise. Optional and default value is false
	 * @param searchInSKOSLabel true to search in SKOS lexicalizations, false otherwise. Optional and default value is false
	 * @param searchInSKOSXLLabel true to search in SKOSXL Lericalizations, false otherwise. Optional and default value is false
	 * @param searchInOntolex true to search in ONTOLEX lexicalizations, false otherwise. Optional and default value is false
	 * @return The list of AnnotatedValue<Resource> with all the infos about the desired resources
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResource(String searchString, String[] rolesArray,
			@Optional(defaultValue = "true") boolean  useLexicalizations,
			boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional(defaultValue = "false") boolean useNotes, @Optional List<IRI> schemes,
			@Optional(defaultValue="or") String schemeFilter,
			@Optional List<String> langs, @Optional(defaultValue = "false") boolean includeLocales,
			@Optional(defaultValue = "false") boolean searchInRDFSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSXLLabel,
			@Optional(defaultValue = "false") boolean searchInOntolex)
			throws IllegalStateException, STPropertyAccessException {
		IRI lexModel = getProject().getLexicalizationModel();
		if(lexModel.equals(Project.ONTOLEXLEMON_LEXICALIZATION_MODEL)) {
			searchInRDFSLabel = true;
		}
		//check that at least one of the different type of use* is set to true
		if(!useLexicalizations && !useLocalName && !useURI && !useNotes){
			throw  new IllegalArgumentException("All these parameters could not be false: useLexicalizations, useLocalName, useURI, useNotes");
		}
		if(!useLexicalizations && useNotes){
			throw  new IllegalArgumentException("useNodes cannot be true while useLexicalizations is false");
		}

		//prepare the namespace map
		Map <String, String> prefixToNamespaceMap = getProject().getOntologyManager().getNSPrefixMappings(false);

		String query = ServiceForSearches.getPrefixes() + "\n"
				+ instantiateSearchStrategy().searchResource(stServiceContext, searchString, rolesArray, useLexicalizations,
						useLocalName, useURI, useNotes, searchMode, schemes, schemeFilter, langs, includeLocales, lexModel,
						searchInRDFSLabel, searchInSKOSLabel, searchInSKOSXLLabel, searchInOntolex, prefixToNamespaceMap);

		logger.debug("query = " + query);

		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processQName();
		qb.processRendering();
		return qb.runQuery();

	}

	/**
	 * Searches the Resource (and returns their labels), according to the input parameters, and return their IRIs (as String)
	 * @param searchString the text to search
	 * @param rolesArray the role(s) the returned resources should belong to. Optional
	 * @param useLocalName true to search in the localName of the resources, false otherwise
	 * @param searchMode the searchMode to use during the search. Check {@link it.uniroma2.art.semanticturkey.search.SearchMode} to get the
	 * 	 *                   possible SerchMode
	 * @param schemes the list of schemes in which the skos:Concept will be searched. Useful only in SKOS/SKOSXL. Optional
	 * @param schemeFilter if the returned concepts should belong to just one of the input sheme ()or value or to all of them (and value).
	 * 	 *                     Optional and default value is or
	 * @param langs the languages of the Literals in which to search the input text. Optional
	 * @param cls the iri of the class in which to search Instances matching the input text. Optional
	 * @param includeLocales true to includes locales in the search (e.g. searching in 'en' will include results from 'en-us' as well).
	 * 	 *                       Optional and default value is false
	 * @return the list of Labels of the desired resources
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<String> searchStringList(String searchString, @Optional String[] rolesArray,
			boolean useLocalName, SearchMode searchMode, @Optional List<IRI> schemes,
			@Optional(defaultValue="or") String schemeFilter,
			@Optional List<String> langs, @Optional IRI cls,
			@Optional(defaultValue = "false") boolean includeLocales)
			throws IllegalStateException, STPropertyAccessException {

		return instantiateSearchStrategy().searchStringList(stServiceContext, searchString, rolesArray,
				useLocalName, searchMode, schemes, schemeFilter, langs, cls, includeLocales);
	}

	/**
	 * Search the resources, according to the specified input parameters
	 * If searchMode is startsWith, prefixes in qname (in the searchString) are expanded
	 * @param searchString the text to search
	 * @param rolesArray the role(s) the returned resources should belong to. Optional
	 * @param searchMode the searchMode to use during the search. Check {@link it.uniroma2.art.semanticturkey.search.SearchMode} to get the
	 * 	 *                   possible SerchMode
	 * @param schemes the list of schemes in which the skos:Concept will be searched. Useful only in SKOS/SKOSXL. Optional
	 * @param schemeFilter if the returned concepts should belong to just one of the input sheme ()or value or to all of them (and value).
	 * 	 *                     Optional and default value is or
	 * @param cls the iri of the classes in which to search Instances matching the input text. Optional
	 * @param maxNumResults the maximum number of results the service will return. Only positive numbers are accepted.
	 *                         0 means no limit in the number of results. Optional and default value is 0 (no limit)
	 * @return List of Local names of the retrieved resources
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<String> searchURIList(String searchString, @Optional String[] rolesArray,
			SearchMode searchMode, @Optional List<IRI> schemes, @Optional(defaultValue="or") String schemeFilter, @Optional IRI cls,
			@Optional(defaultValue="0") @Min(0) int maxNumResults)
			throws IllegalStateException, STPropertyAccessException {

		//prepare the namespace map
		Map <String, String> prefixToNamespaceMap = getProject().getOntologyManager().getNSPrefixMappings(false);

		return instantiateSearchStrategy().searchURIList(stServiceContext, searchString, rolesArray,
				searchMode, schemes, schemeFilter, cls, prefixToNamespaceMap, maxNumResults);
	}

	/**
	 * Searched the Instances of the given class and return all relevant info about the desired resources
	 * (such as the show).
	 * @param cls the iri of the classes in which to search Instances matching the input text
	 * @param searchString the text to search
	 * @param useLexicalizations true to search in the lexicalizations, false otherwise. Optional and default value is true
	 * @param useLocalName true to search in the localName of the resources, false otherwise
	 * @param useURI true to search in the URI of the resources, false otherwise
	 * @param searchMode the searchMode to use during the search. Check {@link it.uniroma2.art.semanticturkey.search.SearchMode} to get the
	 * 	 *                   possible SerchMode
	 * @param useNotes true to search in all the values of the skos:note and its subProperties, false otherwise. Can be set to true only if
	 * 	 *                 the parameter useLocalName is true as well. Optional and default value is false
	 * @param langs the languages of the Literals in which to search the input text. Optional
	 * @param includeLocales true to includes locales in the search (e.g. searching in 'en' will include results from 'en-us' as well).
	 * 	 *                       Optional and default value is false
	 * @return he list of AnnotatedValue<Resource> with all the infos about the desired instances
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(IRI cls, String searchString,
			@Optional(defaultValue = "true") boolean  useLexicalizations,
			boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional(defaultValue = "false") boolean useNotes, @Optional List<String> langs,
			@Optional(defaultValue = "false") boolean includeLocales)
			throws IllegalStateException, STPropertyAccessException {

		//prepare the namespace map
		Map <String, String> prefixToNamespaceMap = getProject().getOntologyManager().getNSPrefixMappings(false);

		IRI lexModel = getProject().getLexicalizationModel();
		List<IRI> clsList = new ArrayList<>();
		clsList.add(cls);
		List<List<IRI>> clsListList = new ArrayList<>();
		clsListList.add(clsList);
		String query = ServiceForSearches.getPrefixes() + "\n"
				+ instantiateSearchStrategy().searchInstancesOfClass(stServiceContext, clsListList,
						searchString, useLexicalizations, useLocalName, useURI, useNotes, searchMode, langs, includeLocales,
						false, false, lexModel, false, false, false, false, null, null, null, null, null, 
						instantiateSearchStrategy(), getProject().getBaseURI(), prefixToNamespaceMap);

		logger.debug("query = " + query);

		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processQName();
		qb.processRendering();
		return qb.runQuery();
	}

	/**
	 * Search in the Lexical Entries
	 * @param searchString the text to search
	 * @param useLexicalizations true to search in the lexicalizations, false otherwise. Optional and default value is true
	 * @param useLocalName  true to search in the localName of the resources, false otherwise
	 * @param useURI true to search in the URI of the resources, false otherwise
	 * @param searchMode the searchMode to use during the search. Check {@link it.uniroma2.art.semanticturkey.search.SearchMode} to get the
	 * 	 *                   possible SerchMode
	 * @param useNotes true to search in all the values of the skos:note and its subProperties, false otherwise. Can be set to true only if
	 * 	 *                 the parameter useLocalName is true as well. Optional and default value is false
	 * @param lexicons the Lexicons the Lexical Entries should belong to (in OR)
	 * @param langs the languages of the Literals in which to search the input text. Optional
	 * @param includeLocales true to includes locales in the search (e.g. searching in 'en' will include results from 'en-us' as well).
	 * 	 *                       Optional and default value is false
	 * @param searchInRDFSLabel true to search in rdfs:label, false otherwise. Optional and default value is false
	 * @param searchInSKOSLabel  true to search in SKOS lexicalizations, false otherwise. Optional and default value is false
	 * @param searchInSKOSXLLabel true to search in SKOSXL Lericalizations, false otherwise. Optional and default value is false
	 * @param searchInOntolex true to search in ONTOLEX lexicalizations, false otherwise. Optional and default value is false
	 * 	 * @return The list of AnnotatedValue<Resource> with all the infos about the desired resources
	 * @return The list of  AnnotatedValue<Resource> with all the infos about the desired Lexical Entries
	 * @throws IllegalStateException
	 * @throws STPropertyAccessException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchLexicalEntry(String searchString,
			@Optional(defaultValue = "true") boolean  useLexicalizations,
			boolean useLocalName,
			boolean useURI, SearchMode searchMode, @Optional(defaultValue = "false") boolean useNotes,
			@Optional List<IRI> lexicons, @Optional List<String> langs,
			@Optional(defaultValue = "false") boolean includeLocales,
			@Optional(defaultValue = "false") boolean searchInRDFSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSXLLabel,
			@Optional(defaultValue = "false") boolean searchInOntolex)
			throws IllegalStateException, STPropertyAccessException {

		//prepare the namespace map
		Map <String, String> prefixToNamespaceMap = getProject().getOntologyManager().getNSPrefixMappings(false);

		String query = ServiceForSearches.getPrefixes() + "\n"
				+ instantiateSearchStrategy().searchLexicalEntry(stServiceContext, searchString,
						useLexicalizations, useLocalName,
						useURI, useNotes, searchMode, lexicons, langs, includeLocales,
						getProject().getLexicalizationModel(), searchInRDFSLabel, searchInSKOSLabel,
						searchInSKOSXLLabel, searchInOntolex, prefixToNamespaceMap);

		logger.debug("query = " + query);

		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.process(LexicalEntryRenderer.INSTANCE, "resource", "attr_show");
		qb.processQName();
		return qb.runQuery();
	}

	/**
	 * Returns the shortest path from the Root Element to the desired resource
	 * @param role the role of the input resource
	 * @param resourceURI the input resource to which the path is searched
	 * @param schemesIRI the list of schemes in which the skos:Concept will be searched. Useful only in SKOS/SKOSXL. Optional
	 * @param schemeFilter if the returned concepts should belong to just one of the input sheme ()or value or to all of them (and value).
	 * 	 *                     Optional and default value is or
	 * @param root the Root element (if a single root is present). Optional
	 * @param broaderProps the list of broader property to use (when role is concept)
	 * @param narrowerProps the list of broader property to use (when role is concept)
	 * @param includeSubProperties true to include subproperty of the broader/narrower passed properties
	 *                                (when role is concept)
	 * @return the shortest path to reach the desired concept
	 * @throws InvalidParameterException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resourceURI)+ ')', 'R')")
	public Collection<AnnotatedValue<Resource>> getPathFromRoot(RDFResourceRole role, IRI resourceURI,
			@Optional List<IRI> schemesIRI,
			@Optional(defaultValue="or") String schemeFilter,
			@Optional(defaultValue = "<http://www.w3.org/2002/07/owl#Thing>") IRI root,
			@Optional @LocallyDefinedResources List<IRI> broaderProps,
			@Optional @LocallyDefinedResources List<IRI> narrowerProps,
			@Optional(defaultValue = "true") boolean includeSubProperties) throws InvalidParameterException {


		//if at least one scheme is passed, then get all top concept of such scheme(s) and then later uses 
		// this information to remove path not going to a topConcept+
		List<String> topConceptList = new ArrayList<>();
		if(schemesIRI!=null && schemesIRI.size()>0) {
			String topConceptAndInverse = "<" + SKOS.TOP_CONCEPT_OF+ "> | ^<"+SKOS.HAS_TOP_CONCEPT+">";
			String query = "SELECT DISTINCT ?topConcept ?scheme" +
					"\nWHERE{" +
					"\n?topConcept "+topConceptAndInverse+" ?scheme ." +
					"\n}";
			logger.debug("query: " + query);

			TupleQuery tupleQuery = getManagedConnection().prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);

			// set the dataset to search just in the UserNamedGraphs
			SimpleDataset dataset = new SimpleDataset();
			Resource[] namedGraphs = getUserNamedGraphs();
			for (Resource namedGraph : namedGraphs) {
				if (namedGraph instanceof IRI) {
					dataset.addDefaultGraph((IRI) namedGraph);
				}
			}
			tupleQuery.setDataset(dataset);

			// execute the query
			TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
			Map<String, Integer> conceptToSchemeTempMap = new HashMap<>();
			while(tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				String topConcept = bindingSet.getValue("topConcept").stringValue();
				IRI scheme = (IRI) bindingSet.getValue("scheme");
				//check that the scheme belong to the input scheme
				for(IRI inputScheme : schemesIRI) {
					if (scheme.equals(inputScheme)) {
						if (schemeFilter.equals("and")) {
							if (!conceptToSchemeTempMap.containsKey(topConcept)) {
								conceptToSchemeTempMap.put(topConcept, 0);
							}
							conceptToSchemeTempMap.put(topConcept, conceptToSchemeTempMap.get(topConcept)+1);
						} else { // 'or' case
							topConceptList.add(topConcept);
						}
					}
				}
			}
			//if the schemeFilter was 'and' get from the map all concepts belonging to ALL the scheme (just check the number of schemes)
			for(String topConcept : conceptToSchemeTempMap.keySet()){
				if(conceptToSchemeTempMap.get(topConcept).equals(schemesIRI.size())){
					topConceptList.add(topConcept);
				}
			}
		}

		String query = null;
		String superResourceVar = null, superSuperResourceVar = null;
		if (role.equals(RDFResourceRole.concept)) {
			// check if the client passed a hierachicalProp, otherwise, set it as skos:broader
			List<IRI> broaderPropsToUse = it.uniroma2.art.semanticturkey.services.core.SKOS
					.getHierachicalProps(broaderProps, narrowerProps);
			// inversHierachicalProp could be null if the hierachicalProp has no inverse
			List<IRI> narrowerPropsToUse = it.uniroma2.art.semanticturkey.services.core.SKOS
					.getInverseOfHierachicalProp(broaderProps, narrowerProps);

			String broaderNarrowerPath = it.uniroma2.art.semanticturkey.services.core.SKOS
					.preparePropPathForHierarchicalForQuery(broaderPropsToUse, narrowerPropsToUse,
							getManagedConnection(), includeSubProperties);
			superResourceVar = "broader";
			superSuperResourceVar = "broaderOfBroader";
			String inSchemeOrTopConcept = "<" + SKOS.IN_SCHEME + ">|<" + SKOS.TOP_CONCEPT_OF
					+ "> | ^<"+SKOS.HAS_TOP_CONCEPT+">";
			
			//@formatter:off
			query = "SELECT DISTINCT ?broader ?broaderOfBroader ?isTopConcept ?isTop" +
					"\nWHERE{" +

					"\nBIND("+ NTriplesUtil.toNTriplesString(resourceURI)+" AS ?resource )" +
					"\n?subConceptClass <"+RDFS.SUBCLASSOF+">* <"+SKOS.CONCEPT+">.";
					
					
			//if a scheme is passed, check that the ?resource belong to such scheme(s)
			if(schemesIRI != null && schemesIRI.size()>0) {
				if(schemesIRI.size()==1) {
					query += "\n?resource " + inSchemeOrTopConcept + " " + NTriplesUtil.toNTriplesString(schemesIRI.get(0)) + " .";
				} else { // schemesIRI.size()>1
					if(schemeFilter.equals("and")) {
						for (IRI scheme : schemesIRI) {
							query += "\n?resource (" + inSchemeOrTopConcept + ") "+NTriplesUtil.toNTriplesString(scheme)+" .";
						}
					} else { // 'or' case
						query += "\n?resource (" + inSchemeOrTopConcept + ") ?schemeRes ." +
								ServiceForSearches.filterWithOrValues(schemesIRI, "?schemeRes");
					}
				}
			}
			query += "\n{" +
					"\n"+it.uniroma2.art.semanticturkey.services.core.SKOS
					.combinePathWithVarOrIri("?resource", "?broader", broaderNarrowerPath, true)+
					"\n?broader a ?typeB ."; //to get only those resources defined in this project
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broader " + inSchemeOrTopConcept + " <" + schemesIRI.get(0).stringValue() + "> ."+
						"\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\n?broader (<"+SKOS.TOP_CONCEPT_OF+"> | ^<"+SKOS.HAS_TOP_CONCEPT+">) <"+schemesIRI.get(0)+"> ." +
						"\n}";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				if(schemeFilter.equals("and")) {
					for(IRI scheme : schemesIRI){
						query += "\n?broader " + inSchemeOrTopConcept + " "+NTriplesUtil.toNTriplesString(scheme)+" .";
					}
					query += "\nOPTIONAL{" +
							"\nBIND (\"true\" AS ?isTopConcept)";
					for(IRI scheme : schemesIRI){
						query += "\n?broader (<" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + ">) "+NTriplesUtil.toNTriplesString(scheme)+" .";
					}
					query += "\n}";
				} else { // 'or' case
					query += "\n?broader " + inSchemeOrTopConcept + " ?scheme1 ." +
							ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme1") +
							"\nOPTIONAL{" +
							"\nBIND (\"true\" AS ?isTopConcept)" +
							"\n?broader (<" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + ">) ?scheme2 ." +
							ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme2") +
							"\n}";
				}
			} else { // (schemesIRI==null || schemesIRI.size()==0)  //the schemes is either null or an empty list
				//check if the selected broader has no brother itself, in this case it is consider a topConcept
				query +="\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\nMINUS{" +
						"\n"+it.uniroma2.art.semanticturkey.services.core.SKOS
						.combinePathWithVarOrIri("?broader", "?broader2", broaderNarrowerPath, false)+
						"\n}" +
						"\n}";
			}
			query += "\nOPTIONAL{" +
					"\n"+it.uniroma2.art.semanticturkey.services.core.SKOS
					.combinePathWithVarOrIri("?broader", "?broaderOfBroader", broaderNarrowerPath, false) +
					"\n?broaderOfBroader a ?typeBB ."; //to get only those resources defined in this project
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " <" + schemesIRI.get(0).stringValue() + "> . ";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				if(schemeFilter.equals("and")){
					for(IRI scheme : schemesIRI){
						query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " "+NTriplesUtil.toNTriplesString(scheme)+" . ";
					}
				} else {// 'or' case
					query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " ?scheme3 . " +
							ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme3");
				}
			}
			query +="\n}" +
					"\n}" +
					"\nUNION" +
					"\n{";
			//this union is used when the first part does not return anything, so when the desired concept
			// does not have any broader, but it is defined as topConcept (to either a specified scheme or
			// to at least one)
			query+= "\n?resource a ?subConceptClass .";
			if(schemesIRI != null && schemesIRI.size()==1){
					query+="\n?resource " +
							"(<"+SKOS.TOP_CONCEPT_OF+"> | ^<"+SKOS.HAS_TOP_CONCEPT+">) <"+schemesIRI.get(0)+"> .";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				if(schemeFilter.equals("and")){
					for(IRI scheme : schemesIRI){
						query += "\n?resource (<" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + ">)"
								+ NTriplesUtil.toNTriplesString(scheme)+" . ";
					}
				} else {// 'or' case
					query += "\n?resource " +
							"(<" + SKOS.TOP_CONCEPT_OF + "> | ^<" + SKOS.HAS_TOP_CONCEPT + ">) ?scheme4 ." +
							ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme4");
				}
			} else{
				query+="\n?resource " +
						"(<"+SKOS.TOP_CONCEPT_OF+"> | ^<"+SKOS.HAS_TOP_CONCEPT+">) _:b1";
			}
			query+="\nBIND(\"true\" AS ?isTop )" +
					"\n}";
					
			// this part, used only when no scheme is selected, is used when the concept does not have any
			// broader and it is not topConcept of any scheme
			if(schemesIRI == null || schemesIRI.size()==0){
				query+="\nUNION" +
						"\n{" +
						"\n?resource a ?subConceptClass ." +
						"\nMINUS{" +
						it.uniroma2.art.semanticturkey.services.core.SKOS
								.combinePathWithVarOrIri("?resource", "?genericConcept", broaderNarrowerPath, false)+"\n" +
						"\n ?genericConcept a/<"+RDFS.SUBCLASSOF+">* <"+SKOS.CONCEPT+"> ." +
						"\n}" +
						"\nFILTER (NOT EXISTS{ ?resource "
								+ "(<"+SKOS.TOP_CONCEPT_OF+"> | ^<"+SKOS.HAS_TOP_CONCEPT+"> ) ?genericScheme})" +
						"\nBIND(\"true\" AS ?isTop )" +
						"\n}";
			}
					
			query+="\n}";
			//@formatter:on
		} else if (role.equals(RDFResourceRole.property)) {
			superResourceVar = "superProperty";
			superSuperResourceVar = "superSuperProperty";
			//@formatter:off
			query = "SELECT DISTINCT ?superProperty ?superSuperProperty ?isTop" +
					"\nWHERE{" +
					"\n{" +
					"\n<" + resourceURI + "> <" + RDFS.SUBPROPERTYOF + ">* ?superProperty ." +
					"\n?superProperty a ?type ."+ //to get only those properties defined in this project
					"\nOPTIONAL{" +
					"\n?superProperty <" + RDFS.SUBPROPERTYOF + "> ?superSuperProperty ." +
					"\n?superSuperProperty a ?type2 ."+ //to get only those properties defined in this project
					"\n}" +
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI+"> a ?type ." +
					"\nFILTER( " +
					"?type = <"+RDF.PROPERTY+"> || " +
					"?type = <"+OWL.OBJECTPROPERTY+"> || " +
					"?type = <"+OWL.DATATYPEPROPERTY+"> || " +
					"?type = <"+OWL.ANNOTATIONPROPERTY+"> || " +
					"?type = <"+OWL.ONTOLOGYPROPERTY+"> )" +
					"\nFILTER NOT EXISTS{<"+resourceURI+"> <"+RDFS.SUBPROPERTYOF+"> ?tempProp ."+
					"\n?tempProp a ?type2 .}" +
					"\nBIND(\"true\" AS ?isTop )" +
					"\n}" +
					"\n}";
			//@formatter:on
		} else if (role.equals(RDFResourceRole.cls)) {
			superResourceVar = "superClass";
			superSuperResourceVar = "superSuperClass";
			//@formatter:off
			query = "SELECT DISTINCT ?superClass ?superSuperClass ?isTop" +
					"\nWHERE{" +
					"\n{" +
					"\n<" + resourceURI + "> <" + RDFS.SUBCLASSOF + ">* ?superClass ." +
					"\nFILTER(isIRI(?superClass))";
			
			//if the input root is different from owl:Thing e rdfs:Resource the ?superClass should be 
			// rdfs:subClass* of such root
			if(!root.equals(OWL.THING) && !root.equals(RDFS.RESOURCE)) {
				query += "\n?superClass <" + RDFS.SUBCLASSOF + ">* <"+root+"> .";
			}
			
					//check that the superClass belong to the default graph
			query +="\n?metaClass1 <" + RDFS.SUBCLASSOF + ">* <"+RDFS.CLASS+"> ." +
					"\n?superClass a ?metaClass1 ."+

					"\nOPTIONAL{" +
					"\n?superClass <" + RDFS.SUBCLASSOF + "> ?superSuperClass ." +
					"\nFILTER(isIRI(?superSuperClass))";
			//if the input root is different from owl:Thing and rdfs:Resource the ?superClass, in this OPTIONAL,
			// should be different from the input root
			if(!root.equals(OWL.THING) && !root.equals(RDFS.RESOURCE)) {
				query += "\n FILTER(?superClass != <"+root+"> )";
			}
			
					//check that the superSuperClass belong to the default graph
			query +="\n?metaClass2 <" + RDFS.SUBCLASSOF + ">* <"+RDFS.CLASS+"> ." +
					"\n?superSuperClass a ?metaClass2 ."+

					"\n}" +
					"\n}" +
					"\nUNION" +
					"\n{";
			if(!root.equals(OWL.THING) && !root.equals(RDFS.RESOURCE)) {
				query+=
						"\n<"+resourceURI+"> a <"+OWL.CLASS+">." +
						"\nFILTER (<"+resourceURI+"> = <"+root+">) " +
						"\nBIND(\"true\" AS ?isTop )";
			} else {
				query+=
						"\n<"+resourceURI+"> a <"+OWL.CLASS+">." +
						"\nFILTER NOT EXISTS{<"+resourceURI+"> <"+RDFS.SUBCLASSOF+"> _:b1}" +
						"\nBIND(\"true\" AS ?isTop )";
			}
			query+="\n}" +
					"\n}";
			//@formatter:on
		} else if (role.equals(RDFResourceRole.skosCollection)) {
			superResourceVar = "superCollection";
			superSuperResourceVar = "superSuperCollection";
			String complexPropPath = "(<" + SKOS.MEMBER + "> | (<"
					+ SKOS.MEMBER_LIST + ">/<" + RDF.REST + ">*/<"
					+ RDF.FIRST + ">))";
			//@formatter:off
			query = "SELECT DISTINCT ?superCollection ?superSuperCollection ?isTop" +
					"\nWHERE {"+
					"\n{"+
					"\n?superCollection "+complexPropPath+"* <"+resourceURI+"> ." +
					"\nOPTIONAL {"+
					"?superSuperCollection "+complexPropPath+" ?superCollection ." +
					"\n}" +
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI+"> a ?type ." +
					"\nFILTER(?type = <"+SKOS.COLLECTION+"> ||  ?type = <"+SKOS.ORDERED_COLLECTION+"> )"+
					"\nFILTER NOT EXISTS{ _:b1 "+complexPropPath+" <"+resourceURI+"> }" +
					"\nBIND(\"true\" AS ?isTop )" +
					"\n}" +
					"\n}";
			//@formatter:on
		} else {
			throw new IllegalArgumentException("Invalid input role: " + role);
		}
		logger.debug("query: " + query);

		TupleQuery tupleQuery = getManagedConnection().prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);

		// set the dataset to search just in the UserNamedGraphs
		SimpleDataset dataset = new SimpleDataset();
		Resource[] namedGraphs = getUserNamedGraphs();
		for (Resource namedGraph : namedGraphs) {
			if (namedGraph instanceof IRI) {
				dataset.addDefaultGraph((IRI) namedGraph);
			}
		}
		tupleQuery.setDataset(dataset);

		// execute the query
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		// the map containing the resource with all the added values taken from the response of the query
		Map<String, ResourceForHierarchy> resourceToResourceForHierarchyMap = new HashMap<String, ResourceForHierarchy>();
		boolean isTopResource = false;
		while (tupleQueryResult.hasNext()) {
			BindingSet bindingSet = tupleQueryResult.next();
			// get the value of the superResource (broader for concepts, superClass for classes, etc). This is
			// not just the direct super type, but it uses the transitive closure in SPARQL
			if (bindingSet.hasBinding(superResourceVar)) {
				Value superNode = bindingSet.getBinding(superResourceVar).getValue();
				boolean isResNotURI = false;
				String superResourceShow = null;
				String superResourceId;
				if (superNode instanceof IRI) {
					superResourceId = superNode.stringValue();
					superResourceShow = ((IRI) superNode).getLocalName();
				} else { // BNode or Literal
					superResourceId = "NOT URI " + superNode.stringValue();
					isResNotURI = true;
				}

				// get the superSuperResource
				String superSuperResourceId = null;
				String superSuperResourceShow = null;
				Value superSuperResNode = null;
				boolean isSuperResABNode = false;
				if (bindingSet.hasBinding(superSuperResourceVar)) {
					superSuperResNode = bindingSet.getBinding(superSuperResourceVar).getValue();
					if (superSuperResNode instanceof IRI) {
						superSuperResourceId = superSuperResNode.stringValue();
						superSuperResourceShow = ((IRI) superSuperResNode).getLocalName();
					} else { // BNode or Literal
						superSuperResourceId = "NOT URI " + superSuperResNode.stringValue();
						isSuperResABNode = true;
					}
				}

				// now add the information about superResource and superSuperResource to the map
				if (!resourceToResourceForHierarchyMap.containsKey(superResourceId)) {
					resourceToResourceForHierarchyMap.put(superResourceId,
							new ResourceForHierarchy(superNode, superResourceShow, isResNotURI));
				}
				if (!bindingSet.hasBinding("isTopConcept")) { // use only for concept
					resourceToResourceForHierarchyMap.get(superResourceId).setTopConcept(false);
				}

				if (superSuperResNode != null) {
					if (!resourceToResourceForHierarchyMap.containsKey(superSuperResourceId)) {
						resourceToResourceForHierarchyMap.put(superSuperResourceId, new ResourceForHierarchy(
								superSuperResNode, superSuperResourceShow, isSuperResABNode));
					}
					// get the structure in the map for the superResource to add the superSuperResource
					// (the superResource is added to the structure containing the superSuperResource as
					// its subResource)
					ResourceForHierarchy resourceForHierarchy = resourceToResourceForHierarchyMap
							.get(superSuperResourceId);
					resourceForHierarchy.addSubResource(superResourceId);

					resourceToResourceForHierarchyMap.get(superResourceId).setHasNoSuperResource(false);
				}
			}
			if (bindingSet.hasBinding("isTop")) {
				isTopResource = true;
			}

		}
		tupleQueryResult.close();

		// iterate over the resourceToResourceForHierarchyMap and look for the topConcept
		// and construct a list of list containing all the possible paths
		// exclude all the path having at least one element which is not a URI (so a BNode or a Literal)
		List<List<String>> pathList = new ArrayList<List<String>>();
		for (ResourceForHierarchy resourceForHierarchy : resourceToResourceForHierarchyMap.values()) {
			if (!resourceForHierarchy.hasNoSuperResource) {
				// since it has at least one superElement (superClass, broader concept or superProperty)
				// it cannot be the first element of a path
				continue;
			}
			if (role.equals(RDFResourceRole.concept)) {
				// the role is a concept, so check if an input scheme was passed, if so, if it is not a
				// top concept (for that particular scheme) then pass to the next concept
				if (schemesIRI != null && schemesIRI.size()>0 && !resourceForHierarchy.isTopConcept) {
					continue;
				}
			}
			List<String> currentList = new ArrayList<String>();
			// currentList.add(resourceForHierarchy.getValue().stringValue());
			addSubResourcesListUsingResourceFroHierarchy(resourceURI.stringValue(), resourceForHierarchy,
					currentList, pathList, resourceToResourceForHierarchyMap);
		}

		// if the input resource is a topResource, then add it to the pathList as a list containing just one
		// element
		if (isTopResource || (pathList.isEmpty() && !resourceToResourceForHierarchyMap.isEmpty())) {
			List<String> listWithOneElem = new ArrayList<>();
			listWithOneElem.add(resourceURI.stringValue());
			pathList.add(listWithOneElem);
		}
		
		//if the input schemesIRI is not null and contains at least one scheme, remove all path not
		// starting with a topConcept (check if the list of topConcepts contains at least one element)
		if(topConceptList.size()>0 && pathList.size()>0) {
			Iterator<List<String>> iter = pathList.iterator();
			if(!topConceptList.contains(iter.next().get(0))){
				//the current path does not start with a topConcept, so remove it
				iter.remove();
			}
		}

		// now construct the response
		// to order the path (from the shortest to the longest) first find the maximum length
		int maxLength = -1;
		for (List<String> path : pathList) {
			int currentLength = path.size();
			if (maxLength == -1 || maxLength < currentLength) {
				maxLength = currentLength;
			}
		}

		boolean pathFound = false;
		Collection<AnnotatedValue<Resource>> results = new ArrayList<AnnotatedValue<Resource>>();

		// if it is explicitly a topResource or if no path is returned while there was at least one
		// result from the SPARQL query (this mean that all the paths contained at least one non-URI resource)
		//@formatter:off
		/*if (isTopResource || (pathList.isEmpty() && !resourceToResourceForHierarchyMap.isEmpty())) {
			// the input resource is a top resource for its role (concept, class or property)
			pathFound = true;
			AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>((IRI) resourceURI);
			annotatedValue.setAttribute("explicit", true);
			annotatedValue.setAttribute("show", resourceURI.getLocalName());
			results.add(annotatedValue);
		}*/
		//@formatter:on

		// iterate over all possible found path
		for (int currentLength = 1; currentLength <= maxLength && !pathFound; ++currentLength) {
			// for the given path length, get all the path (having such length)
			for (List<String> path : pathList) {
				boolean targetResNotPresent = true;
				if (currentLength != path.size()) {
					// it is not the right iteration to add this path
					continue;
				}

				boolean first = true;
				for (String resourceInPath : path) {
					// if it is the first element, the role is cls, and the desired root is either
					// rdfs:Resource or owl:Thing, a special check should be perform,
					// since it could be necessary to add rdfs:Resource and even owl:Thing
					boolean addRdfsResource = false, addOwlThing = false;
					if (first && role.equals(RDFResourceRole.cls)
							&& (root.equals(OWL.THING) || root.equals(RDFS.RESOURCE))) {
						if (root.equals(RDFS.RESOURCE)
								&& !resourceInPath.equals(RDFS.RESOURCE.stringValue())) {
							// the desired first element should be rdfs:Resource, but it is not,
							// so add rdfs:Resource as first element
							addRdfsResource = true;
							if (!resourceInPath.equals(OWL.THING.stringValue())) {
								// the first element in the list is not Thing, but it should be, since under
								// rdfs:Resource there should be owl:Thing, so add it (after adding
								// rdfs:Resource)
								addOwlThing = true;
							}
						} else if (root.equals(OWL.THING)
								&& resourceInPath.equals(RDFS.RESOURCE.stringValue())) {
							// do not consider this path, since the root should be owl:Thing and the current
							// found root is rdfs:Resource, which is a superClass of owl:Thing

							// analyze the next path
							break;
						} else if (root.equals(OWL.THING)
								&& !resourceInPath.equals(OWL.THING.stringValue())) {
							// the desired first element should be owl:Thing, but it is not,
							// so add owl:Thing as first element
							addOwlThing = true;
						}

						if (addRdfsResource) {
							AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(
									RDFS.RESOURCE);
							annotatedValue.setAttribute("explicit", true);
							annotatedValue.setAttribute("show", RDFS.RESOURCE.getLocalName());
							results.add(annotatedValue);
						}
						if (addOwlThing) {
							AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(OWL.THING);
							annotatedValue.setAttribute("explicit", true);
							annotatedValue.setAttribute("show", OWL.THING.getLocalName());
							results.add(annotatedValue);
						}
					}

					first = false;
					if (resourceURI.stringValue().equals(resourceInPath)) {
						targetResNotPresent = false;
					}
					AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(
							(Resource) resourceToResourceForHierarchyMap.get(resourceInPath).getValue());
					annotatedValue.setAttribute("explicit", true);
					annotatedValue.setAttribute("show",
							resourceToResourceForHierarchyMap.get(resourceInPath).getShow());
					results.add(annotatedValue);
				}
				// add, if necessary, at the end, the input concept
				if (results.size() != 0 && targetResNotPresent) {
					AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>((IRI) resourceURI);
					annotatedValue.setAttribute("explicit", true);
					annotatedValue.setAttribute("show", resourceURI.getLocalName());
					results.add(annotatedValue);
				}

				// the first path having such length was found, so, do not do the next iteration
				pathFound = true;

				// since a minimal path was found, stop looking for another minimal path
				break;

			}
		}

		return results;
	}

	// private String addFilterForRsourseType(String variable, boolean isClassWanted,
	// boolean isInstanceWanted, boolean isPropertyWanted, boolean isConceptWanted) {
	// boolean otherWanted = false;
	// String filterQuery = "\nFILTER( ";
	// if(isClassWanted){
	// filterQuery += variable+" = <"+OWL.CLASS+">";
	// otherWanted = true;
	// }
	// if(isPropertyWanted){
	// if(otherWanted){
	// filterQuery += " || ";
	// }
	// otherWanted = true;
//			//@formatter:off
//			filterQuery += variable+ " = <"+RDF.PROPERTY+"> || "+
//					variable+" = <"+OWL.OBJECTPROPERTY+"> || "+
//					variable+" = <"+OWL.DATATYPEPROPERTY+"> || "+
//					variable+" = <"+OWL.ANNOTATIONPROPERTY+"> || " +
//					variable+" = <"+OWL.ONTOLOGYPROPERTY+"> ";
//			//@formatter:on
	// }
	// if(isConceptWanted){
	// if(otherWanted){
	// filterQuery += " || ";
	// }
	// otherWanted = true;
	// filterQuery += variable+" = <"+SKOS.CONCEPT+">";
	// }
	// if(isInstanceWanted){
	// if(otherWanted){
	// filterQuery += " || ( ";
	// }
//			//@formatter:off
//			filterQuery+="EXISTS{"+variable+" a <"+OWL.CLASS+">}";
//			
//			//old version
//			/*filterQuery+=variable+"!= <"+OWL.CLASS+"> && "+
//					variable+"!=<"+RDFS.CLASS+"> && "+
//					variable+"!=<"+RDFS.RESOURCE+"> && "+
//					variable+"!=<"+RDF.PROPERTY+"> && "+
//					variable+"!=<"+OWL.OBJECTPROPERTY+"> && "+
//					variable+"!=<"+OWL.DATATYPEPROPERTY+"> && "+
//					variable+"!=<"+OWL.ANNOTATIONPROPERTY+"> && "+
//					variable+"!=<"+OWL.ONTOLOGYPROPERTY+"> && "+
//					variable+"!=<"+SKOS.CONCEPT+"> && "+
//					variable+"!=<"+SKOS.CONCEPTSCHEME+"> && "+
//					variable+"!=<"+SKOSXL.LABEL+">";*/
//			//@formatter:on
	// if(otherWanted){
	// filterQuery += " ) ";
	// }
	// otherWanted = true;
	// }
	//
	// filterQuery += ")";
	// return filterQuery;
	// }

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<String> searchPrefix(String searchString, SearchMode searchMode){
		List<String> prefixList = new ArrayList<>();

		//prepare the namespace map
		Map <String, String> prefixToNamespaceMap = getProject().getOntologyManager().getNSPrefixMappings(false);

		//iterate over the prefixToNamespaceMap to get the desired prefixes according to the
		// searchString and searchMode
		for(String prefix : prefixToNamespaceMap.keySet()){
			if(searchMode.equals(SearchMode.contains)){
				if(prefix.contains(searchString)){
					prefixList.add(prefix);
				}
			} else if(searchMode.equals(SearchMode.startsWith)){
				if (prefix.startsWith(searchString)) {
					prefixList.add(prefix);
				}
			} else if (searchMode.equals(SearchMode.endsWith)) {
				if (prefix.endsWith(searchString)) {
					prefixList.add(prefix);
				}
			} else {
				throw new IllegalArgumentException("The only accepted SearchMode are: " + SearchMode.contains +
						", " + SearchMode.startsWith + " and " + SearchMode.endsWith);
			}
		}

		return prefixList;
	}

	private void addSubResourcesListUsingResourceFroHierarchy(String targetRes, ResourceForHierarchy resource,
			List<String> currentPathList, List<List<String>> pathList,
			Map<String, ResourceForHierarchy> resourceToResourceForHierarchyMap) {

		if (resource.isNotURI) {
			// since this resource is not a URI, then the path to which this resource belong to must not be
			// consider, so, do not add to the possible paths
			return;
		}

		// check if the current element is already in the path, in this case do nothing and return, since
		// it is a cycle
		if (currentPathList.contains(resource.getValue().stringValue())) {
			return;
		}

		// add the current resource to the current path
		currentPathList.add(resource.getValue().stringValue());

		// check if the current resource (the one just added) is the target element, in this case add the
		// current path to the list of the possible path and return
		if (targetRes.equals(resource.getValue().stringValue())) {
			pathList.add(currentPathList);
			return;
		}

		// iterate over subResources of the current resource
		for (String subResource : resource.getSubResourcesList()) {
			// create a copy of the currentList
			List<String> updatedPath = new ArrayList<String>(currentPathList);
			// call getSubResourcesListUsingResourceFroHierarchy on subResource
			addSubResourcesListUsingResourceFroHierarchy(targetRes,
					resourceToResourceForHierarchyMap.get(subResource), updatedPath, pathList,
					resourceToResourceForHierarchyMap);
		}
	}

	private class ResourceForHierarchy {
		private boolean isTopConcept; // used only for concept
		private boolean hasNoSuperResource;
		private List<String> subResourcesList;
		// private String resourceString;
		private Value value;
		private String show;
		private boolean isNotURI;

		public ResourceForHierarchy(Value value, String show, boolean isNotURI) {
			// this.resourceString = resource.stringValue();
			this.value = value;
			this.show = show;
			this.isNotURI = isNotURI;
			isTopConcept = true;
			hasNoSuperResource = true;
			subResourcesList = new ArrayList<String>();
		}

		public boolean isNotURI() {
			return isNotURI;
		}

		/*
		 * public String getResourceString(){ return resourceString; }
		 */

		public Value getValue() {
			return value;
		}

		public String getShow() {
			return show;
		}

		public boolean isTopConcept() {
			return isTopConcept;
		}

		public void setTopConcept(boolean isTopConcept) {
			this.isTopConcept = isTopConcept;
		}

		public boolean hasNoSuperResource() {
			return hasNoSuperResource;
		}

		public void setHasNoSuperResource(boolean haNoSuperResource) {
			this.hasNoSuperResource = haNoSuperResource;
		}

		public List<String> getSubResourcesList() {
			return subResourcesList;
		}

		public void addSubResource(String subResource) {
			if (!subResourcesList.contains(subResource)) {
				subResourcesList.add(subResource);
			}
		}
	}

}
