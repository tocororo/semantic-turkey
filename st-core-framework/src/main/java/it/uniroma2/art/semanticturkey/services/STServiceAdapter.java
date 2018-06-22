package it.uniroma2.art.semanticturkey.services;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ProjectionOperator;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.coda.structures.ARTTriple;
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
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;
import it.uniroma2.art.semanticturkey.project.STRepositoryInfoUtils;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.search.SearchStrategyUtils;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.sparql.SPARQLUtilities;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.tx.STServiceAspect;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.versioning.VersioningMetadataSupport;

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
	private PlatformTransactionManager txManager;

	@Autowired
	private ObjectFactory<CODACoreProvider> codaCoreProviderFactory;

	@Autowired
	private CustomFormManager cfManager;

	@Autowired
	protected ExtensionPointManager exptManager;

	private final ServletUtilities servletUtilities;

	protected STServiceAdapter() {
		servletUtilities = ServletUtilities.getService();
	}

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
	protected CODACore getInitializedCodaCore(RepositoryConnection repoConnection)
			throws ProjectInconsistentException {
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

	/**
	 * Enrich the <code>modelAdditions</code> and <code>modelAdditions</code> with the triples to add and
	 * remove suggested by CODA running the PEARL rule defined in the CustomForm with the given
	 * <code>cfId</code>
	 */
	protected void enrichWithCustomForm(RepositoryConnection repoConn, Model modelAdditions,
			Model modelRemovals, CustomForm cForm, Map<String, Object> userPromptMap, StandardForm stdForm)
			throws ProjectInconsistentException, CODAException, CustomFormException {
		CODACore codaCore = getInitializedCodaCore(repoConn);
		try {
			if (cForm.isTypeGraph()) {
				CustomFormGraph cfGraph = cForm.asCustomFormGraph();
				SessionFormData sessionData = new SessionFormData();
				sessionData.addSessionParameter(SessionFormData.Data.user,
						UsersManager.getLoggedUser().getIRI().stringValue());
				UpdateTripleSet updates = cfGraph.executePearlForConstructor(codaCore, userPromptMap, stdForm,
						sessionData);
				shutDownCodaCore(codaCore);

				for (ARTTriple t : updates.getInsertTriples()) {
					modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
				for (ARTTriple t : updates.getDeleteTriples()) {
					modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
				}
			} else {
				throw new CustomFormException("Cannot execute CustomForm with id '" + cForm.getId()
						+ "' as constructor since it is not of type 'graph'");
			}
		} catch (ProjectionRuleModelNotSet | UnassignableFeaturePathException e) {
			throw new CODAException(e);
		} finally {
			shutDownCodaCore(codaCore);
		}
	}

	protected void addValue(RepositoryConnection repoConn, Resource subject, IRI predicate,
			SpecialValue value) throws ProjectInconsistentException, CODAException {
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
					UpdateTripleSet updates = cfGraph.executePearlForRange(codaCore,
							cfValue.getUserPromptMap(), sessionData);
					// link the generated graph with the resource
					List<ARTTriple> insertTriples = updates.getInsertTriples();
					if (!insertTriples.isEmpty()) {
						Resource graphEntry = detectGraphEntry(insertTriples);
						VersioningMetadataSupport.currentVersioningMetadata().addCreatedResource(graphEntry); // set
																												// created
																												// for
																												// versioning
						modelAdditions.add(subject, predicate, graphEntry);
						for (ARTTriple t : insertTriples) {
							modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject());
						}
					}
					for (ARTTriple t : updates.getDeleteTriples()) {
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

	/**
	 * This method detects the entry of a graph (list of triples) based on an heuristic: entry is that subject
	 * that never appears as object
	 * 
	 * @param triples
	 * @return
	 */
	private Resource detectGraphEntry(List<ARTTriple> triples) {
		for (ARTTriple t1 : triples) {
			Resource subj = t1.getSubject();
			boolean neverObj = true;
			for (ARTTriple t2 : triples) {
				if (subj.equals(t2.getObject()))
					neverObj = false;
			}
			if (neverObj) {
				return subj;
			}
		}
		return null;
	}

	// Semi-deprecated

	protected XMLResponseREPLY createReplyResponse(RepliesStatus status) {
		return servletUtilities.createReplyResponse(stServiceContext.getRequest().getServiceMethod(), status);
	}

	protected XMLResponseREPLY createReplyFAIL(String message) {
		return servletUtilities.createReplyFAIL(stServiceContext.getRequest().getServiceMethod(), message);
	}

	protected XMLResponseREPLY createBooleanResponse(boolean resp) {
		return servletUtilities.createBooleanResponse(stServiceContext.getRequest().getServiceMethod(), resp);
	}

	protected Response logAndSendException(Exception e, String msg) {
		e.printStackTrace(System.err);
		// getLogger().error(e.toString());
		return servletUtilities.createExceptionResponse(stServiceContext.getRequest().getServiceMethod(),
				msg);
	}

	protected Response logAndSendException(String msg, SerializationType sertype) {
		// getLogger().error(msg);
		return servletUtilities.createExceptionResponse(stServiceContext.getRequest().getServiceMethod(), msg,
				sertype);
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

}
