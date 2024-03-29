package it.uniroma2.art.semanticturkey.services;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.exception.ValueNotPresentDueToConfigurationException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ProjectionOperator;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.CODATriple;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.CustomFormParseUtils;
import it.uniroma2.art.semanticturkey.customform.CustomFormValue;
import it.uniroma2.art.semanticturkey.customform.SessionFormData;
import it.uniroma2.art.semanticturkey.customform.SpecialValue;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.customform.UpdateTripleSet;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.PrefAltLabelClashException;
import it.uniroma2.art.semanticturkey.exceptions.PrefPrefLabelClashException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerationException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfoUtils;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.search.SearchStrategyUtils;
import it.uniroma2.art.semanticturkey.services.aspects.ResourceLevelChangeMetadata;
import it.uniroma2.art.semanticturkey.services.aspects.ResourceLevelChangeMetadataSupport;
import it.uniroma2.art.semanticturkey.services.events.ResourceDeleted;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.sparql.SPARQLUtilities;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.tx.STServiceAspect;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * Base class of Semantic Turkey services.
 * 
 * @author <a href="mailto:manuel.fiorelli@gmail.com">Manuel Fiorelli</a>
 *
 */
public class STServiceAdapter implements STService, NewerNewStyleService {

	@Autowired
	protected STServiceContext stServiceContext;

	@Autowired
	protected ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private PlatformTransactionManager txManager;

	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	@Autowired
	protected CustomFormManager cfManager;

	@Autowired
	protected ExtensionPointManager exptManager;

	protected PlatformTransactionManager getPlatformTransactionManager() {
		return txManager;
	}

	public Project getProject() {
		return stServiceContext.getProject();
	}

	public Resource[] getUserNamedGraphs() {
		List<Resource> rGraphs = Arrays.asList(stServiceContext.getRGraphs());

		// if (rGraphs.contains(RDF4JMigrationUtils.convert2rdf4j(NodeFilters.ANY))) {
		// return new Resource[0];
		// }

		return rGraphs.toArray(new Resource[rGraphs.size()]);

		// return rGraphs.stream().map(rdf4j2artFact::aRTResource2RDF4JResource).toArray(Resource[]::new);
	}

	public Resource getWorkingGraph() {
		// return
		// rdf4j2artFact.aRTResource2RDF4JResource(RDF4JMigrationUtils.convert2art(stServiceContext.getWGraph()));
		return stServiceContext.getWGraph();
	}

	public Resource getDeleteGraph() {
		// TODO check this method
		return getWorkingGraph();
	}

	public Resource getMetadataGraph() {
		return stServiceContext.getProject().getMetadataGraph(stServiceContext.getExtensionPathComponent());
	}

	public RepositoryConnection getManagedConnection() {
		return RDF4JRepositoryUtils.getConnection(getRepository(), false);
	}

	/**
	 * Returns the repository for servicing a data request. This operation is aware of different mechanisms
	 * that alter the repository, e.g. version dumps.
	 * 
	 * @return
	 */
	public Repository getRepository() {
		return STServiceContextUtils.getRepostory(stServiceContext);
	}

	protected Collection<AnnotatedValue<Resource>> retrieveResources(String queryString) {
		RepositoryConnection repoConn = getManagedConnection();

		TupleQuery query = repoConn.prepareTupleQuery(queryString);

		Set<String> queryVariables = SPARQLUtilities.getVariables(queryString);

		Method currentMethod = STServiceAspect.getCurrentServiceInvocation().getMethod();
		Parameter[] currentMethodParameters = currentMethod.getParameters();
		Object[] currentMethodArguments = STServiceAspect.getCurrentServiceInvocation().getArguments();

		for (int i = 0; i < currentMethodParameters.length; i++) {
			String parameterName = currentMethodParameters[i].getName();

			if (queryVariables.contains(parameterName)) {
				query.setBinding(parameterName, SPARQLUtilities.java2node(currentMethodArguments[i]));
			}
		}

		return QueryResults.stream(query.evaluate())
				.map(bindingSet -> new AnnotatedValue<>((Resource) bindingSet.getValue("resource")))
				.collect(toList());
	}

	protected QueryBuilder createQueryBuilder(String resourceQuery) {
		return new QueryBuilder(stServiceContext, resourceQuery);
	}

	/**
	 * Returns a new IRI for a resource. The parameter {@code xRole} holds the nature of the resource that
	 * will be identified with the given URI. Depending on the value of the parameter {@code xRole}, a
	 * conforming converter may generate differently shaped URIs, possibly using specific arguments passed via
	 * the map {@code args}.
	 * 
	 * @param xRole
	 * @param valueMapping
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateIRI(String xRole, Map<String, Value> valueMapping) throws URIGenerationException {
		return getProject().getURIGenerator().generateIRI(stServiceContext, xRole, valueMapping);
	}

	/**
	 * Returns an instance of {@link CODACore} to use in services.
	 * 
	 * @param repoConnection
	 * @return
	 * @throws ProjectInconsistentException
	 */
	protected CODACore getInitializedCodaCore(RepositoryConnection repoConnection) {
		CODACore codaCore = codaCoreProviderFactory.getObject().getCODACore();
		codaCore.initialize(repoConnection);
		return codaCore;
	}

	/**
	 * Stops and shutdown the instance of CODACore. This method should be called when it is finished using the
	 * instance of CODACore returned by {@link #getInitializedCodaCore(RepositoryConnection)}
	 * 
	 * @param codaCore
	 */
	protected void shutDownCodaCore(CODACore codaCore) {
		codaCore.setRepositoryConnection(null);
		codaCore.stopAndClose();
	}
	
	protected void addValue(RepositoryConnection repoConn, Resource subject, IRI predicate,
			SpecialValue value) throws CODAException {
		if (value.isRdf4jValue()) {
			repoConn.add(subject, predicate, value.getRdf4jValue(), getWorkingGraph());
		} else { // value.isCustomFormValue()
			CustomFormValue cfValue = value.getCustomFormValue();
			CODACore codaCore = getInitializedCodaCore(repoConn);
			try {
				Model modelAdditions = new LinkedHashModel();
				Model modelRemovals = new LinkedHashModel();

				CustomForm cForm = cfManager.getCustomForm(getProject(), cfValue.getCustomFormId());
				if (cForm.isTypeGraph()) {
					CustomFormGraph cfGraph = cForm.asCustomFormGraph();
					SessionFormData sessionData = new SessionFormData();
					sessionData.addSessionParameter(SessionFormData.Data.user,
							UsersManager.getLoggedUser().getIRI().stringValue());
					StandardForm stdForm = new StandardForm();
					for (Map.Entry<String, Object> stdFormEntry : cfValue.getStdFormMap().entrySet()) {
						stdForm.addFormEntry(stdFormEntry.getKey(), stdFormEntry.getValue().toString());
					}

					UpdateTripleSet updates = cfGraph.executePearl(codaCore, cfValue.getUserPromptMap(), stdForm, sessionData);

					// link the generated graph with the resource
					List<CODATriple> insertTriples = updates.getInsertTriples();
					if (!insertTriples.isEmpty()) {
						Resource graphEntry = detectGraphEntry(insertTriples);
						// set created for versioning
						ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(graphEntry);
						modelAdditions.add(subject, predicate, graphEntry);
						for (CODATriple t : insertTriples) {
							modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject());
						}
					}
					for (CODATriple t : updates.getDeleteTriples()) {
						modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject());
					}
				} else if (cForm.isTypeNode()) {
					String nodeValue = cfValue.getUserPromptMap().entrySet().iterator().next().getValue()
							.toString();// get the only value
					ProjectionOperator projOperator = CustomFormParseUtils.getProjectionOperator(codaCore,
							cForm.getRef());
					Value generatedValue = codaCore.executeProjectionOperator(projOperator, nodeValue);
					// link the generated value with the resource
					modelAdditions.add(subject, predicate, generatedValue);
				}
				repoConn.add(modelAdditions, getWorkingGraph());
				repoConn.remove(modelRemovals, getWorkingGraph());
			} catch (PRParserException | ComponentProvisioningException | ConverterException
					| ProjectionRuleModelNotSet | UnassignableFeaturePathException e) {
				throw new CODAException(e);
			} finally {
				shutDownCodaCore(codaCore);
			}
		}
	}

	protected void removeReifiedValue(RepositoryConnection repoConn, Resource subject, IRI predicate,
			Resource value) throws PRParserException {
		// remove resource as object in the triple <s, p, o> for the given subject and predicate
		repoConn.remove(subject, predicate, value, getWorkingGraph());

		CODACore codaCore = getInitializedCodaCore(repoConn);
		CustomFormGraph cf = cfManager.getCustomFormGraphSeed(getProject(), codaCore, repoConn,
				value, Collections.singleton(predicate), false);

		Update update;
		if (cf != null) {
			String query = "delete { " +
					"	graph " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {" +
					cf.getGraphSectionAsString(codaCore, false) +
					" 	}" + //close graph {}
					"} where { " +
					cf.getGraphSectionAsString(codaCore, true) +
					"}";
			update = repoConn.prepareUpdate(query);
			update.setBinding(cf.getEntryPointPlaceholder(codaCore).substring(1), value);
		} else {
			/*
			If property hasn't a CustomForm simply delete all triples where resource occurs.
			note: this case should never happen since this service should be called only when the predicate has a CF.
			*/
			String query = "delete { " +
					"	graph ?g {" +
					"		?value ?p1 ?o1 . " +
					" 		?s2 ?p2 ?value . " +
					" 	}" + //close graph {}
					"} where { " +
					"	?value ?p1 ?o1 . " +
					"	?s2 ?p2 ?value . " +
					"}";
			update = repoConn.prepareUpdate(query);
			update.setBinding("g", getWorkingGraph());
			update.setBinding("value", value);
		}
		update.setIncludeInferred(false);
		update.execute();
		shutDownCodaCore(codaCore);

		//remove also all the outgoing triples from the deleting value (prevents pending triple added "outside" the CF)
		String query =
				"delete { " +
				"	graph ?g {" +
				"		?value ?p1 ?o1 . " +
				"	}" + //close graph {}
				"} where { " +
				"	?value ?p1 ?o1 . " +
				"}";
		update = repoConn.prepareUpdate(query);
		update.setBinding("g", getWorkingGraph());
		update.setBinding("value", value);
		update.execute();
	}

	/**
	 * Initialize the new created resource (and the xLabel in case of SKOSXL lex model),
	 * by running the CF provided within the {@code customFormValue}.
	 * The updates produced by the CF execution are added to {@code modelAdditions} and {@code modelRemovals}
	 *
	 * Returns the IRI of the creating resource, which is:
	 *     - generated through the CC, in case it is delegated, OR
	 *     - the same provided in input ({@code newResource})
	 * Note that if {@code newResource} is not provided and the CC is not delegated to create the resource,
	 * a runtime Exception is thrown
	 *
	 * @param repoConnection
	 * @param newResource the resource being created.
	 *              It is the one provided/generated in the calling service (e.g. createConcept, createCollection, ...)
	 *              and represents the fallback IRI in case the CC is not delegated to the resource IRI creation
	 * @param customFormValue
	 * @param stdForm
	 * @param modelAdditions
	 * @param modelRemovals
	 * @return
	 * @throws CustomFormException
	 * @throws CODAException
	 * @throws PrefPrefLabelClashException
	 * @throws URIGenerationException
	 * @throws PrefAltLabelClashException
	 * @throws UnsupportedLexicalizationModelException
	 */
	protected IRI generateResourceWithCustomConstructor(RepositoryConnection repoConnection,
			@Nullable IRI newResource, CustomFormValue customFormValue, StandardForm stdForm, Model modelAdditions, Model modelRemovals)
			throws CustomFormException, CODAException {
		CustomForm cForm = cfManager.getCustomForm(getProject(), customFormValue.getCustomFormId());
		if (cForm.isTypeGraph()) {
			CustomFormGraph cfGraph = (CustomFormGraph) cForm;
			CODACore codaCore = getInitializedCodaCore(repoConnection);

			boolean cfResCreationDelegated = cfGraph.isResourceCreationDelegated(codaCore);
			if (cfResCreationDelegated) {
                /* if CC is delegated to create the resource IRI (e.g. through "resource uri(coda:randIdGen())" or
                "resource uri userPrompt/customIriField") set newResource to null, so it will not be set
                 into the stdForm (it will not be used by the CC anyway) */
				newResource = null;
			} else if (newResource == null) {
                /*
				- resource not provided (as argument) or generated (randomly) in-service
				AND
                - CF not delegated to generate the resource,
                => Resource cannot be created
                */
				throw new IllegalStateException("Cannot create a resource without providing its IRI or without using a CustomForm with the delegation");
			}

			if (newResource != null) {
				stdForm.addFormEntry(StandardForm.Prompt.resource, newResource.stringValue());
			}

			SessionFormData sessionData = new SessionFormData();
			sessionData.addSessionParameter(SessionFormData.Data.user, UsersManager.getLoggedUser().getIRI().stringValue());

			try {
				UpdateTripleSet updateTripleSet = cfGraph.executePearl(codaCore, customFormValue.getUserPromptMap(), stdForm, sessionData);
				shutDownCodaCore(codaCore);

				if (cfResCreationDelegated) {
					//CC was delegated to create resource IRI => get it now that the CF has been executed
					newResource = (IRI) detectGraphEntry(updateTripleSet.getInsertTriples());
					checkNotLocallyDefined(repoConnection, newResource);
				}

				for (CODATriple t : updateTripleSet.getInsertTriples()) {
					modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
				for (CODATriple t : updateTripleSet.getDeleteTriples()) {
					modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
			} catch (ProjectionRuleModelNotSet | UnassignableFeaturePathException e) {
				throw new CODAException(e);
			} finally {
				shutDownCodaCore(codaCore);
			}

			return newResource;
		} else {
			throw new CustomFormException("Cannot execute CustomForm with id '" + cForm.getId()
					+ "' as constructor since it is not of type 'graph'");
		}
	}

	/**
	 * This method detects the entry of a graph (list of triples) generated by coda.
	 *
	 * @param triples
	 * @return
	 */
	private Resource detectGraphEntry(List<CODATriple> triples) {
		/* detect the entry by finding that subject resource which was produced by a node identified
		in the graph section of the pearl with the reserved variable $resource */
		for (CODATriple t1 : triples) {
			try {
				if (t1.getSubjectNameInGraph().equals("resource")) {
					return t1.getSubject();
				}
			} catch (ValueNotPresentDueToConfigurationException e) {
				//this should never be thrown since processNextAnnotation in CustomFormGraph is invoked with the proper argument
				e.printStackTrace();
			}
		}
		//alternative attempt using a heuristic: entry is that subject that never appears as object
		for (CODATriple t1 : triples) {
			Resource subj = t1.getSubject();
			boolean neverObj = true;
			for (CODATriple t2 : triples) {
				if (subj.equals(t2.getObject()))
					neverObj = false;
			}
			if (neverObj) {
				return subj;
			}
		}
		/*
		if this code is reached, none of the two cases above are met, so returns null.
		Note that this case must never happen since it would lead to an exception related to an incomplete statement
		(the addition of the triple ?s ?p ?o, where ?o is the Resource returned here, will have ?o null)
		 */
		return null;
	}


	// TEMP SERVICE, WHICH WILL BE REPLACED BY THE APPROPRIATE PROCESSOR IN QueryBuilder
	// variables being used: $st, $go, $dep and ?attr_nature
	protected String generateNatureSPARQLSelectPart() {
		return NatureRecognitionOrchestrator.getNatureSPARQLSelectPart();
	}

	// TEMP SERVICE, WHICH WILL BE REPLACED BY THE APPROPRIATE PROCESSOR IN QueryBuilder
	// variables being used: $st, $go, $t, $dep
	// prefixes needed: skos, owl, skosxl, rdfs, rdf
	protected String generateNatureSPARQLWherePart(String varName) {
		return NatureRecognitionOrchestrator.getNatureSPARQLWherePart(varName);
	}

	public static RDFResourceRole getRoleFromNature(String nature) {
		String roleRaw = nature.split(",")[0];

		if (roleRaw.isEmpty()) {
			return RDFResourceRole.undetermined;
		} else {
			return RDFResourceRole.valueOf(roleRaw);
		}
	}

	public static Optional<IRI> getGraphFromNature(String nature) {
		String[] parts = nature.split(",");

		if (parts.length < 2) {
			return Optional.empty();
		}

		if (parts[1].isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(SimpleValueFactory.getInstance().createIRI(parts[1]));
		}
	}

	protected SearchStrategy instantiateSearchStrategy() {
		SearchStrategies searchStrategy = STRepositoryInfoUtils
				.getSearchStrategy(getProject().getRepositoryManager()
						.getSTRepositoryInfo(STServiceContextUtils.getRepostoryId(stServiceContext)));

		return SearchStrategyUtils.instantiateSearchStrategy(exptManager, searchStrategy);
	}

	protected SearchStrategy instantiateSearchStrategy(SearchStrategies searchStrategy) {
		return SearchStrategyUtils.instantiateSearchStrategy(exptManager, searchStrategy);
	}

	protected Reference parseReference(String relativeReference) {
		int colonPos = relativeReference.indexOf(":");

		if (colonPos == -1)
			throw new IllegalArgumentException("Invalid reference: " + relativeReference);

		Scope scope = Scope.deserializeScope(relativeReference.substring(0, colonPos));
		String identifier = relativeReference.substring(colonPos + 1);

		switch (scope) {
		case SYSTEM:
			return new Reference(null, null, identifier);
		case PROJECT:
			return new Reference(getProject(), null, identifier);
		case USER:
			return new Reference(null, UsersManager.getLoggedUser(), identifier);
		case PROJECT_USER:
			return new Reference(getProject(), UsersManager.getLoggedUser(), identifier);
		default:
			throw new IllegalArgumentException("Unsupported scope: " + scope);
		}
	}

	protected void publishResourceDeleted(Resource resource, RDFResourceRole role) {
		ResourceLevelChangeMetadata resourceLevelChangeMetadata = ResourceLevelChangeMetadataSupport.currentVersioningMetadata();

		if (resourceLevelChangeMetadata.getDeletedResources().stream().noneMatch(p -> Objects.equals(p.left, resource))) {
			Project project = getProject();
			STUser user = UsersManager.getLoggedUser();

			Repository repository = getRepository();
			RepositoryConnection repConn = getManagedConnection();

			Pair<Resource, RDFResourceRole> enhancedPair = ResourceLevelChangeMetadataSupport.enhanceResourceChangeInfo(repConn, ImmutablePair.of(resource, role));

			resourceLevelChangeMetadata.addCreatedResource(resource, role);
			applicationEventPublisher
					.publishEvent(new ResourceDeleted(enhancedPair.getLeft(), enhancedPair.getRight(),
							stServiceContext.getWGraph(), repository, project, user));
		}
	}

	protected void publishResourceDeleted(Resource resource) {
		publishResourceDeleted(resource, RDFResourceRole.undetermined);
	}

	/**
	 * This is used to manually replicate the NotLocallyDefined constraint behaviour whereas a creation service
	 * (e.g. createInstance, createProperty, ...) receives a null IRI
	 *
	 * (copied from {@link it.uniroma2.art.semanticturkey.mdr.core.impl.MetadataRegistryBackendImpl}
	 *
	 * @param conn
	 * @param resource
	 * @throws IllegalArgumentException
	 */
	protected void checkNotLocallyDefined(RepositoryConnection conn, @Nullable IRI resource) throws IllegalArgumentException {
		if (resource == null)
			return;
		if (conn.hasStatement(resource, null, null, false)) {
			throw new IllegalArgumentException("Resource already defined: " + NTriplesUtil.toNTriplesString(resource));
		}
	}


}
