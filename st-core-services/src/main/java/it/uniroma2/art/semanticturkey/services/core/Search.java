package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.eclipse.rdf4j.repository.sparql.query.QueryStringUtil;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.config.sparql.SPARQLParameterizationStore;
import it.uniroma2.art.semanticturkey.config.sparql.SPARQLStore;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLOperation;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLParameterization;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLParameterization.ConstraintVariableBinding;
import it.uniroma2.art.semanticturkey.config.sparql.StoredSPARQLParameterization.VariableBinding;
import it.uniroma2.art.semanticturkey.constraints.HasDatatype;
import it.uniroma2.art.semanticturkey.constraints.HasRole;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefinedResources;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.Required;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;
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
import it.uniroma2.art.semanticturkey.services.core.ontolexlemon.LexicalEntryRenderer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;

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

	@STServiceOperation
	@Write
	// TODO decide the @PreAuthorize
	// #@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'w')")
	public void createIndexes() throws Exception {
		ValidationUtilities.executeWithoutValidation(
				ValidationUtilities.isValidationEnabled(stServiceContext), getManagedConnection(), conn -> {
					instantiateSearchStrategy().initialize(conn);
				});
	}

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

	@STServiceOperation(method = RequestMethod.GET)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public STProperties getCustomSearchForm(String searchParameterizationReference)
			throws IOException, ConfigurationNotFoundException, WrongPropertiesException,
			NoSuchConfigurationManager, STPropertyAccessException {

		StoredSPARQLParameterization storedSparqlParameterization = (StoredSPARQLParameterization) exptManager
				.getConfiguration(SPARQLParameterizationStore.class.getName(),
						parseReference(searchParameterizationReference));

		ReceiverTypeDefinition<STProperties> receiverTypeDefinition = new ByteBuddy()
				.subclass(STProperties.class).defineMethod("getShortName", String.class, Modifier.PUBLIC)
				.intercept(FixedValue.value("Test"))
				.defineMethod("getDescription", String.class, Modifier.PUBLIC)
				.intercept(FixedValue.value("Test Description"));
		net.bytebuddy.dynamic.DynamicType.Builder.FieldDefinition.Optional<STProperties> valuableDefinition = null;

		for (Entry<String, VariableBinding> entry : storedSparqlParameterization.variableBindings.entrySet()
				.stream().filter(entry -> entry.getValue() instanceof ConstraintVariableBinding)
				.collect(toList())) {
			String varName = entry.getKey();
			String propName = "var_" + varName;
			ConstraintVariableBinding constraintVariableBinding = (ConstraintVariableBinding) entry
					.getValue();
			Class<?> propClazz = constraintVariableBinding.getDatatype() != null ? Literal.class : IRI.class;

			List<AnnotationDescription> fieldAnnotations = new ArrayList<>();
			fieldAnnotations.add(AnnotationDescription.Builder.ofType(STProperty.class)
					.define("description", "Value bound to the variable \"" + varName + "\"")
					.define("displayName", varName).build());
			fieldAnnotations.add(AnnotationDescription.Builder.ofType(Required.class).build());

			List<AnnotationDescription> fieldTypeAnnotations = new ArrayList<>();

			if (constraintVariableBinding.getResourceRole() != null) {
				fieldTypeAnnotations.add(AnnotationDescription.Builder.ofType(HasRole.class)
						.define("value", constraintVariableBinding.getResourceRole()).build());
			}

			if (constraintVariableBinding.getDatatype() != null) {
				fieldTypeAnnotations.add(AnnotationDescription.Builder.ofType(HasDatatype.class)
						.define("value", constraintVariableBinding.getDatatype().stringValue()).build());
			}

			TypeDefinition propTypeDefinition = TypeDescription.Generic.Builder.rawType(propClazz).annotate(
					fieldTypeAnnotations.toArray(new AnnotationDescription[fieldTypeAnnotations.size()]))
					.build();

			if (valuableDefinition != null) {
				valuableDefinition = valuableDefinition.defineField(propName, propTypeDefinition,
						Modifier.PUBLIC);
			} else {
				valuableDefinition = receiverTypeDefinition.defineField(propName, propTypeDefinition,
						Modifier.PUBLIC);
			}

			valuableDefinition = valuableDefinition.annotateField(
					fieldAnnotations.toArray(new AnnotationDescription[fieldAnnotations.size()]));
		}

		Unloaded<STProperties> unloadedClass = valuableDefinition != null ? valuableDefinition.make()
				: receiverTypeDefinition.make();

		// By using the wrapper strategy, we obtain that the generated class is unloaded, when its instances
		// and the class loader are no longer reachable
		Class<? extends STProperties> clazz = unloadedClass
				.load(Search.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();

		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

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
		System.out.println("resoure variable = " + resourceVariableName);

		return qb.runQuery();
	}

	public enum StatusFilter {
		NOT_DEPRECATED, ONLY_DEPRECATED, UNDER_VALIDATION, UNDER_VALIDATION_FOR_DEPRECATION, ANYTHING
	}
	
	//@formatter:off
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> advancedSearch(@Optional String searchString,
			@Optional(defaultValue="false") boolean useLocalName, 
			@Optional(defaultValue="false") boolean useURI, 
			@Optional SearchMode searchMode, 
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
		if (!ValidationUtilities.isValidationEnabled(stServiceContext)) {
			if (statusFilter == StatusFilter.UNDER_VALIDATION
					|| statusFilter == StatusFilter.UNDER_VALIDATION_FOR_DEPRECATION) {
				throw new IllegalArgumentException(
						"Invalid status filter for a project without validation: " + statusFilter);
			}
		}

		String query= ServiceForSearches.getPrefixes() +
				"\nSELECT DISTINCT ?resource ?attr_nature ?attr_scheme" +
				"\nWHERE{" +
				"\n{";
		
		//use the searchInstancesOfClasse to construct the first part of the query (the subquery)
		query += instantiateSearchStrategy().searchInstancesOfClass(stServiceContext, types, searchString,
				useLocalName, useURI, useNotes, searchMode, langs, includeLocales, true, true, lexModel,
				searchInRDFSLabel, searchInSKOSLabel, searchInSKOSXLLabel, searchInOntolex);
		//use the other parameters to filter the results
		query+="\n}";
		// the statusFilter
		if(statusFilter.equals(StatusFilter.ANYTHING)) {
			//do nothing in this case
		} else if(statusFilter.equals(StatusFilter.NOT_DEPRECATED)) {
			//check that the resource is not marked as deprecated
			query += "\nFILTER NOT EXISTS{" +
					"\n{?resource "+NTriplesUtil.toNTriplesString(OWL2Fragment.DEPRECATED)+" true }" +
					"\nUNION"+
					"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDCLASS)+" }" +
					"\nUNION"+
					"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDPROPERTY)+" }" +
					"\n}";
					
		} else if(statusFilter.equals(StatusFilter.ONLY_DEPRECATED)) {
			//check that the resource is marked as deprecated
			query += 
				"\n{?resource "+NTriplesUtil.toNTriplesString(OWL2Fragment.DEPRECATED)+" true }" +
				"\nUNION"+
				"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDCLASS)+" }" +
				"\nUNION"+
				"\n{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDPROPERTY)+" }";
		} else if(statusFilter.equals(StatusFilter.UNDER_VALIDATION)) {
			//check that in the validation graph there is the triple 
			// ?resource a ?type
			IRI validationGraph = (IRI) VALIDATION.stagingAddGraph(SimpleValueFactory.getInstance()
					.createIRI(getProject().getBaseURI()));
			query+="\nGRAPH "+NTriplesUtil.toNTriplesString(validationGraph)+" { "+
					"?resource a ?type_for_validation ." +
					"}";
		} else if(statusFilter.equals(StatusFilter.UNDER_VALIDATION_FOR_DEPRECATION)) {
			//check that in the validation graph the resource is marked as deprecated
			IRI validationGraph = (IRI) VALIDATION.stagingAddGraph(SimpleValueFactory.getInstance()
					.createIRI(getProject().getBaseURI()));
			String valGraph = NTriplesUtil.toNTriplesString(validationGraph);
			query +="\n{GRAPH "+valGraph+"{?resource "+NTriplesUtil.toNTriplesString(OWL2Fragment.DEPRECATED)+" true }}" +
					"\nUNION"+
					"\n{GRAPH "+valGraph+"{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDCLASS)+" }}" +
					"\nUNION"+
					"\n{GRAPH "+valGraph+"{?resource a "+NTriplesUtil.toNTriplesString(OWL.DEPRECATEDPROPERTY)+" }}";
		}
		
		//the schemes part
		String schemeOrTopConcept="(<"+SKOS.IN_SCHEME.stringValue()+">|<"+SKOS.TOP_CONCEPT_OF+">|"
				+ "^<"+SKOS.HAS_TOP_CONCEPT+">)";
		query += ServiceForSearches.filterWithOrOfAndValues("?resource", schemeOrTopConcept, schemes);
		
		
		//the outgoingLinks part
		if(outgoingLinks!=null && outgoingLinks.size()>0) {
			query += ServiceForSearches.filterWithOrOfAndPairValues(outgoingLinks, "?resource", "out", false);
		}
		//the outgoingSearch part
		int cont=1;
		if(outgoingSearch!=null && outgoingSearch.size()>0) {
			String valueOfProp = "?valueOfProp_"+cont;
			for(TripleForSearch<IRI, String, SearchMode> tripleForSearch : outgoingSearch) {
				query += "\n?resource "+NTriplesUtil.toNTriplesString(tripleForSearch.getPredicate())+" "+valueOfProp+" ." +
						instantiateSearchStrategy().searchSpecificModePrepareQuery(valueOfProp, 
								tripleForSearch.getSearchString(), tripleForSearch.getMode(), null, null, 
								includeLocales, false);
			}
		}
		
		//the ingoingLinks part	
		if(ingoingLinks!=null && ingoingLinks.size()>0) {
			query += ServiceForSearches.filterWithOrOfAndPairValues(ingoingLinks, "?resource", "in", true);
		}
		query+= "\nFILTER(BOUND(?resource))" + //used only to not have problem with the OPTIONAL in qb.processRendering(); 
				"\n}" +
			"\nGROUP BY ?resource ?attr_nature ?attr_scheme";
		logger.debug("query = " + query);

		
		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processRendering();
		qb.process(LexicalEntryRenderer.INSTANCE, "resource", "attr_show");
		return qb.runQuery();
	}
	//@formatter:on

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchResource(String searchString, String[] rolesArray,
			boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional(defaultValue = "false") boolean useNotes, @Optional List<IRI> schemes,
			@Optional List<String> langs, @Optional(defaultValue = "false") boolean includeLocales,
			@Optional(defaultValue = "false") boolean searchInRDFSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSXLLabel,
			@Optional(defaultValue = "false") boolean searchInOntolex)
			throws IllegalStateException, STPropertyAccessException {
		IRI lexModel = getProject().getLexicalizationModel();
		String query = ServiceForSearches.getPrefixes() + "\n"
				+ instantiateSearchStrategy().searchResource(stServiceContext, searchString, rolesArray,
						useLocalName, useURI, useNotes, searchMode, schemes, langs, includeLocales, lexModel,
						searchInRDFSLabel, searchInSKOSLabel, searchInSKOSXLLabel, searchInOntolex);

		logger.debug("query = " + query);

		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processRendering();
		return qb.runQuery();

	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<String> searchStringList(String searchString, @Optional String[] rolesArray,
			boolean useLocalName, SearchMode searchMode, @Optional List<IRI> schemes,
			@Optional List<String> langs, @Optional IRI cls,
			@Optional(defaultValue = "false") boolean includeLocales)
			throws IllegalStateException, STPropertyAccessException {

		return instantiateSearchStrategy().searchStringList(stServiceContext, searchString, rolesArray,
				useLocalName, searchMode, schemes, langs, cls, includeLocales);
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(resource)', 'R')")
	public Collection<String> searchURIList(String searchString, @Optional String[] rolesArray,
			SearchMode searchMode, @Optional List<IRI> schemes, @Optional IRI cls)
			throws IllegalStateException, STPropertyAccessException {

		return instantiateSearchStrategy().searchURIList(stServiceContext, searchString, rolesArray,
				searchMode, schemes, cls);
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(cls, instances)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchInstancesOfClass(IRI cls, String searchString,
			boolean useLocalName, boolean useURI, SearchMode searchMode,
			@Optional(defaultValue = "false") boolean useNotes, @Optional List<String> langs,
			@Optional(defaultValue = "false") boolean includeLocales)
			throws IllegalStateException, STPropertyAccessException {

		IRI lexModel = getProject().getLexicalizationModel();
		List<IRI> clsList = new ArrayList<>();
		clsList.add(cls);
		List<List<IRI>> clsListList = new ArrayList<>();
		clsListList.add(clsList);
		String query = ServiceForSearches.getPrefixes() + "\n"
				+ instantiateSearchStrategy().searchInstancesOfClass(stServiceContext, clsListList,
						searchString, useLocalName, useURI, useNotes, searchMode, langs, includeLocales,
						false, false, lexModel, false, false, false, false);

		logger.debug("query = " + query);

		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.processRendering();
		return qb.runQuery();

	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(limeLexicon)', 'R')")
	public Collection<AnnotatedValue<Resource>> searchLexicalEntry(String searchString, boolean useLocalName,
			boolean useURI, SearchMode searchMode, @Optional(defaultValue = "false") boolean useNotes,
			@Optional List<IRI> lexicons, @Optional List<String> langs,
			@Optional(defaultValue = "false") boolean includeLocales,
			@Optional(defaultValue = "false") boolean searchInRDFSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSLabel,
			@Optional(defaultValue = "false") boolean searchInSKOSXLLabel,
			@Optional(defaultValue = "false") boolean searchInOntolex)
			throws IllegalStateException, STPropertyAccessException {

		String query = ServiceForSearches.getPrefixes() + "\n"
				+ instantiateSearchStrategy().searchLexicalEntry(stServiceContext, searchString, useLocalName,
						useURI, useNotes, searchMode, lexicons, langs, includeLocales,
						getProject().getLexicalizationModel(), searchInRDFSLabel, searchInSKOSLabel,
						searchInSKOSXLLabel, searchInOntolex);

		logger.debug("query = " + query);

		QueryBuilder qb;
		qb = new QueryBuilder(stServiceContext, query);
		qb.process(LexicalEntryRenderer.INSTANCE, "resource", "attr_show");
		qb.processQName();
		return qb.runQuery();
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#resourceURI)+ ')', 'R')")
	public Collection<AnnotatedValue<Resource>> getPathFromRoot(RDFResourceRole role, IRI resourceURI,
			@Optional List<IRI> schemesIRI,
			@Optional(defaultValue = "<http://www.w3.org/2002/07/owl#Thing>") IRI root,
			@Optional @LocallyDefinedResources List<IRI> broaderProps,
			@Optional @LocallyDefinedResources List<IRI> narrowerProps,
			@Optional(defaultValue = "true") boolean includeSubProperties) throws InvalidParameterException {

		// ARTURIResource inputResource = owlModel.createURIResource(resourceURI);

		// check if the client passed a hierachicalProp, otherwise, set it as skos:broader
		List<IRI> broaderPropsToUse = it.uniroma2.art.semanticturkey.services.core.SKOS
				.getHierachicalProps(broaderProps, narrowerProps);
		// inversHierachicalProp could be null if the hierachicalProp has no inverse
		List<IRI> narrowerPropsToUse = it.uniroma2.art.semanticturkey.services.core.SKOS
				.getInverseOfHierachicalProp(broaderProps, narrowerProps);

		String broaderNarrowerPath = it.uniroma2.art.semanticturkey.services.core.SKOS
				.preparePropPathForHierarchicalForQuery(broaderPropsToUse, narrowerPropsToUse,
						getManagedConnection(), includeSubProperties);

		String query = null;
		String superResourceVar = null, superSuperResourceVar = null;
		if (role.equals(RDFResourceRole.concept)) {
			superResourceVar = "broader";
			superSuperResourceVar = "broaderOfBroader";
			String inSchemeOrTopConcept = "<" + SKOS.IN_SCHEME.stringValue() + ">|<" + SKOS.TOP_CONCEPT_OF
					+ ">";
			//@formatter:off
			query = "SELECT DISTINCT ?broader ?broaderOfBroader ?isTopConcept ?isTop" + 
					"\nWHERE{" +
					"\n{" + 
					//"\n<" + resourceURI.stringValue() + "> (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+"> )* ?broader ."; OLD
					"\n"+it.uniroma2.art.semanticturkey.services.core.SKOS
					.combinePathWithVarOrIri(resourceURI, "?broader", broaderNarrowerPath, true);
					//.preparePropPathForHierarchicalForQuery(broaderProp, narrowerProp,
					//		resourceURI, "?broader", getManagedConnection(), true, includeSubProperties);
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broader " + inSchemeOrTopConcept + " <" + schemesIRI.get(0).stringValue() + "> ."+
						"\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\n?broader (<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) <"+schemesIRI.get(0).stringValue()+"> ." +
						"\n}";
			} else if(schemesIRI != null &&schemesIRI.size()>1){
				query += "\n?broader " + inSchemeOrTopConcept + " ?scheme1 ."+
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme1") +
						"\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						"\n?broader (<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) ?scheme2 ." +
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme2") +
						"\n}";
			}
			else if(schemesIRI==null || schemesIRI.size()==0) { //the schemes is either null or an empty list
				//check if the selected broader has no brother itself, in this case it is consider a topConcept
				query +="\nOPTIONAL{" +
						"\nBIND (\"true\" AS ?isTopConcept)" +
						//OLD
						/*"\nFILTER NOT EXISTS{ " +
						"\n?broader (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+">) ?broaderOfBroader ."+
						"}"+*/
						"\nMINUS{" +
						"\n"+it.uniroma2.art.semanticturkey.services.core.SKOS
						.combinePathWithVarOrIri(resourceURI, "?broader", broaderNarrowerPath, false)+
							//.prepareHierarchicalPartForQuery(broaderProp, narrowerProp, "?broader", 
							//	"?broaderOfBroader", false, includeSubProperties) +
						"\n}" +
						"\n}";
			}
			query += "\nOPTIONAL{" +
					//"\n?broader (<" + SKOS.BROADER.stringValue() + "> | ^<"+SKOS.NARROWER.stringValue()+">) ?broaderOfBroader ."; OLD
					"\n"+it.uniroma2.art.semanticturkey.services.core.SKOS
					.combinePathWithVarOrIri("?broader", "?broaderOfBroader", broaderNarrowerPath, false);
					//	.prepareHierarchicalPartForQuery(broaderProp, narrowerProp, "?broader", 
					//		"?broaderOfBroader", false, includeSubProperties);
			if (schemesIRI != null && schemesIRI.size()==1) {
				query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " <" + schemesIRI.get(0).stringValue() + "> . ";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query += "\n?broaderOfBroader " + inSchemeOrTopConcept + " ?scheme3 . "+
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme3");
			}
			query +="\n}" + 
					"\n}" +
					"\nUNION" +
					"\n{";
			//this union is used when the first part does not return anything, so when the desired concept
			// does not have any broader, but it is defined as topConcept (to either a specified scheme or
			// to at least one)
			query+= "\n<" + resourceURI.stringValue() + "> a <"+SKOS.CONCEPT+"> .";
			if(schemesIRI != null && schemesIRI.size()==1){
					query+="\n<"+resourceURI.stringValue()+"> " +
							"(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) <"+schemesIRI.get(0).stringValue()+"> .";
			} else if(schemesIRI != null && schemesIRI.size()>1){
				query+="\n<"+resourceURI.stringValue()+"> " +
						"(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) ?scheme4 ."+
						ServiceForSearches.filterWithOrValues(schemesIRI, "?scheme4");
			} else{
				query+="\n<"+resourceURI.stringValue()+"> " +
						"(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+">) _:b1";
			}
			query+="\nBIND(\"true\" AS ?isTop )" +
					"\n}";
					
			// this part, used only when no scheme is selected, is used when the concept does not have any
			// broader and it is not topConcept of any scheme
			if(schemesIRI == null){
				query+="\nUNION" +
						"\n{" +
						"\n<" + resourceURI.stringValue() + "> a <"+SKOS.CONCEPT+"> ." +
						//OLD
						/*"\nFILTER(NOT EXISTS{<"+resourceURI.stringValue()+"> "
								+ "(<"+SKOS.BROADER+"> | ^<"+SKOS.NARROWER+">) ?genericConcept })" +*/
						"\nMINUS{" +
						it.uniroma2.art.semanticturkey.services.core.SKOS
						.combinePathWithVarOrIri(resourceURI, "?genericConcept", broaderNarrowerPath, false)+"\n" +
						//	.prepareHierarchicalPartForQuery(broaderProp, narrowerProp, resourceURI, 
						//		"?genericConcept", false, includeSubProperties) +
						"\n}" +
						"\nFILTER (NOT EXISTS{ <"+resourceURI.stringValue()+"> "
								+ "(<"+SKOS.TOP_CONCEPT_OF.stringValue()+"> | ^<"+SKOS.HAS_TOP_CONCEPT.stringValue()+"> ) ?genericScheme})" +
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
					"\n<" + resourceURI.stringValue() + "> <" + RDFS.SUBPROPERTYOF.stringValue() + ">* ?superProperty ." +
					"\nOPTIONAL{" +
					"\n?superProperty <" + RDFS.SUBPROPERTYOF.stringValue() + "> ?superSuperProperty ." +
					"\n}" + 
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI.stringValue()+"> a ?type ." +
					"\nFILTER( " +
					"?type = <"+RDF.PROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.OBJECTPROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.DATATYPEPROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.ANNOTATIONPROPERTY.stringValue()+"> || " +
					"?type = <"+OWL.ONTOLOGYPROPERTY.stringValue()+"> )" +
					"\nFILTER NOT EXISTS{<"+resourceURI.stringValue()+"> <"+RDFS.SUBPROPERTYOF.stringValue()+"> _:b1}" +
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
					"\n<" + resourceURI.stringValue() + "> <" + RDFS.SUBCLASSOF.stringValue() + ">* ?superClass ." +
					"\nFILTER(isIRI(?superClass))"; 
			
			//if the input root is different from owl:Thing e rdfs:Resource the ?superClass should be 
			// rdfs:subClass of such root
			if(!root.equals(OWL.THING) && !root.equals(RDFS.RESOURCE)) {
				query += "\n?superClass <" + RDFS.SUBCLASSOF.stringValue() + ">* <"+root.stringValue()+"> ."; 	
			}
			
					//check that the superClass belong to the default graph
			query +="\n?metaClass1 <" + RDFS.SUBCLASSOF.stringValue() + ">* <"+RDFS.CLASS.stringValue()+"> ." +
					"\n?superClass a ?metaClass1 ."+
					
					"\nOPTIONAL{" +
					"\n?superClass <" + RDFS.SUBCLASSOF.stringValue() + "> ?superSuperClass ." +
					"\nFILTER(isIRI(?superSuperClass))";
			//if the input root is different from owl:Thing e rdfs:Resource the ?superClass, in this OPTIONAL,
			// should be different from the input root
			if(!root.equals(OWL.THING) && !root.equals(RDFS.RESOURCE)) {
				query += "\n FILTER(?superClass != <"+root.stringValue()+"> )"; 	
			}
			
					//check that the superSuperClass belong to the default graph
			query +="\n?metaClass2 <" + RDFS.SUBCLASSOF.stringValue() + ">* <"+RDFS.CLASS.stringValue()+"> ." +
					"\n?superSuperClass a ?metaClass2 ."+
					
					"\n}" + 
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI.stringValue()+"> a <"+OWL.CLASS.stringValue()+">." +
					"\nFILTER NOT EXISTS{<"+resourceURI.stringValue()+"> <"+RDFS.SUBCLASSOF.stringValue()+"> _:b1}" +
					"\nBIND(\"true\" AS ?isTop )" +
					"\n}" +
					"\n}";
			//@formatter:on
		} else if (role.equals(RDFResourceRole.skosCollection)) {
			superResourceVar = "superCollection";
			superSuperResourceVar = "superSuperCollection";
			String complexPropPath = "(<" + SKOS.MEMBER.stringValue() + "> | (<"
					+ SKOS.MEMBER_LIST.stringValue() + ">/<" + RDF.REST.stringValue() + ">*/<"
					+ RDF.FIRST.stringValue() + ">))";
			//@formatter:off
			query = "SELECT DISTINCT ?superCollection ?superSuperCollection ?isTop" +
					"\nWHERE {"+
					"\n{"+
					"\n?superCollection "+complexPropPath+"* <"+resourceURI.stringValue()+"> ." +
					"\nOPTIONAL {"+
					"?superSuperCollection "+complexPropPath+" ?superCollection ." +
					"\n}" +
					"\n}" +
					"\nUNION" +
					"\n{" +
					"\n<"+resourceURI.stringValue()+"> a ?type ." +
					"\nFILTER(?type = <"+SKOS.COLLECTION.stringValue()+"> ||  ?type = <"+SKOS.ORDERED_COLLECTION.stringValue()+"> )"+
					"\nFILTER NOT EXISTS{ _:b1 "+complexPropPath+" <"+resourceURI.stringValue()+"> }" +
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
				if (schemesIRI != null && !resourceForHierarchy.isTopConcept) {
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
