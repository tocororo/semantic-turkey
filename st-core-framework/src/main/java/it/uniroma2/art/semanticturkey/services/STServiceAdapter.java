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
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.rdf4jimpl.RDF4JARTResourceFactory;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.SessionFormData;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.customform.UpdateTripleSet;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.project.Project;
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
import it.uniroma2.art.semanticturkey.tx.STServiceInvocaton;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

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

	private final ValueFactory sesVf;
	private final RDF4JARTResourceFactory rdf4j2artFact;
	private final ServletUtilities servletUtilities;

	protected STServiceAdapter() {
		sesVf = SimpleValueFactory.getInstance();
		rdf4j2artFact = new RDF4JARTResourceFactory(sesVf);
		servletUtilities = ServletUtilities.getService();
	}

	protected PlatformTransactionManager getPlatformTransactionManager() {
		return txManager;
	}

	public Project getProject() {
		return stServiceContext.getProject();
	}

	public Resource[] getUserNamedGraphs() {
		List<ARTResource> rGraphs = Arrays.asList(stServiceContext.getRGraphs());

		if (rGraphs.contains(NodeFilters.ANY)) {
			return new Resource[0];
		}

		return rGraphs.stream().map(rdf4j2artFact::aRTResource2RDF4JResource).toArray(Resource[]::new);
	}

	public Resource getWorkingGraph() {
		return rdf4j2artFact.aRTResource2RDF4JResource(stServiceContext.getWGraph());
	}

	public Resource getDeleteGraph() {
		// TODO check this method
		return getWorkingGraph();
	}

	public Resource getMetadataGraph() {
		return rdf4j2artFact.aRTResource2RDF4JResource(
				stServiceContext.getProject().getMetadataGraph(stServiceContext.getExtensionPathComponent()));
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

	protected void applyPatch(Model quadAdditions, Model quadRemovals) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

		// java.lang.Thread.getStackTrace() |
		// it.uniroma2.art.semanticturkey.services.STServiceAdapter2.applyPatch(Model, Model) | <actual
		// service>

		if (stackTrace.length < 3)
			throw new IllegalStateException("Unable to estalish the calling service");

		String methodName = stackTrace[2].getMethodName();
		Method serviceMethod = ReflectionUtilities.getMethodByName(getClass(), methodName);

		STServiceInvocaton currentServiceInvocation = STServiceAspect.getCurrentServiceInvocation();

		if (!serviceMethod.equals(currentServiceInvocation.getMethod())) {
			throw new IllegalStateException(STServiceAdapter.class.getSimpleName()
					+ ".applyPath(..) may only invoked within the executon of a service method");
		}

		System.out.println("=== BEGIN PATCH ===");

		System.out.println(currentServiceInvocation);

		System.out.println();

		quadAdditions.forEach(stmt -> System.out.println("+ "
				+ NTriplesUtil.toNTriplesString(stmt.getSubject()) + " "
				+ NTriplesUtil.toNTriplesString(stmt.getPredicate()) + " "
				+ NTriplesUtil.toNTriplesString(stmt.getObject()) + Optional.ofNullable(stmt.getContext())
						.map(c -> NTriplesUtil.toNTriplesString(c)).orElse("")));
		quadRemovals.forEach(stmt -> System.out.println("- "
				+ NTriplesUtil.toNTriplesString(stmt.getSubject()) + " "
				+ NTriplesUtil.toNTriplesString(stmt.getPredicate()) + " "
				+ NTriplesUtil.toNTriplesString(stmt.getObject()) + Optional.ofNullable(stmt.getContext())
						.map(c -> NTriplesUtil.toNTriplesString(c)).orElse("")));
		System.out.println("=== END PATCH ===");

		RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(getProject().getRepository());
		try {
			conn.add(quadAdditions);
			conn.remove(quadRemovals);
		} finally {
			RDF4JRepositoryUtils.releaseConnection(conn, getProject().getRepository());
		}
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
		try {
			Map<String, Value> artValueMapping = valueMapping.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, (entry) -> entry.getValue()));

			IRI iriRes = getProject().getURIGenerator().generateIRI(stServiceContext, xRole, artValueMapping);

			return sesVf.createIRI(iriRes.stringValue());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
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
	 * Enrich the <code>modelAdditions</code> and <code>modelAdditions</code> with the triples to add and remove
	 * suggested by CODA running the PEARL rule defined in the CustomForm with the given <code>cfId</code>  
	 */
	protected void enrichWithCustomForm(RepositoryConnection repoConn, Model modelAdditions, Model modelRemovals,
			CustomForm cForm, Map<String, Object> userPromptMap, StandardForm stdForm)
			throws ProjectInconsistentException, CODAException, CustomFormException {
		CODACore codaCore = getInitializedCodaCore(repoConn);
		try {
			if (cForm.isTypeGraph()) {
				CustomFormGraph cfGraph = cForm.asCustomFormGraph();
				SessionFormData sessionData = new SessionFormData();
				sessionData.addSessionParameter(SessionFormData.Data.user, UsersManager.getLoggedUser().getIRI().stringValue());
				UpdateTripleSet updates = cfGraph.executePearlForConstructor(codaCore, userPromptMap, stdForm, sessionData);
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

	
	//TEMP SERVICE, WHICH WILL BE REPLACED BY THE APPROPRIATE PROCESSOR IN QueryBuilder
	//variables being used: $st, $go, $dep and ?attr_nature
	protected String generateNatureSPARQLSelectPart(){
		
		String sparqlPartText = "(group_concat(concat(str($st), \"|_|\", str($go), \"|_|\", "
				+ "str($dep));separator=\",\") as ?attr_nature) \n";
		
		return sparqlPartText;
	}
	
	//TEMP SERVICE, WHICH WILL BE REPLACED BY THE APPROPRIATE PROCESSOR IN QueryBuilder
	//variables being used: $st, $go, $t, $dep
	//  prefixes needed: skos, owl, skosxl
	protected String generateNatureSPARQLWherePart(String varName){
		String sparqlPartText;
		
		if(!varName.startsWith("?")){
			varName = "?"+varName;
		}
		sparqlPartText = 
		" OPTIONAL { \n" +
		"  values($st) {(skos:Concept)(owl:Class)(skosxl:Label)(skos:ConceptScheme)} \n" +
		"  graph $go { \n" +
		" "+varName+" a $t . \n" +
		"  } \n" +
		"  $t rdfs:subClassOf* $st \n" +
		" } \n" + 
		" OPTIONAL { \n" +
		"	   BIND( \n" +
		"	     IF(EXISTS {"+varName+" owl:deprecated true}, \"true\", \n" +
		"	      IF(EXISTS {"+varName+" a owl:DeprecatedClass}, \"true\", \n" +
		"	       IF(EXISTS {"+varName+" a owl:DeprecatedProperty}, \"true\", \n" +
		"	        \"false\"))) \n" +
		"	   as $dep ) \n" +
		"	 } \n" ;
		
		
		return sparqlPartText;
	}
	
	// Deprecated

	@Override
	public OWLModel getOWLModel() {
		// TODO Auto-generated method stub
		return null;
	}

}
