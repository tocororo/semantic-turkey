package it.uniroma2.art.semanticturkey.services;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.SessionFormData;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.customform.UpdateTripleSet;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
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
import it.uniroma2.art.semanticturkey.user.UsersManager;

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
	private final ServletUtilities servletUtilities;

	protected STServiceAdapter() {
		sesVf = SimpleValueFactory.getInstance();
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

//		if (rGraphs.contains(RDF4JMigrationUtils.convert2rdf4j(NodeFilters.ANY))) {
//			return new Resource[0];
//		}

		return rGraphs.toArray(new Resource[rGraphs.size()] );
		
		//return rGraphs.stream().map(rdf4j2artFact::aRTResource2RDF4JResource).toArray(Resource[]::new);
	}

	public Resource getWorkingGraph() {
		//return rdf4j2artFact.aRTResource2RDF4JResource(RDF4JMigrationUtils.convert2art(stServiceContext.getWGraph()));
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
		return NatureRecognitionOrchestrator.getNatureSPARQLSelectPart();
	}
	
	//TEMP SERVICE, WHICH WILL BE REPLACED BY THE APPROPRIATE PROCESSOR IN QueryBuilder
	//variables being used: $st, $go, $t, $dep
	//  prefixes needed: skos, owl, skosxl, rdfs
	protected String generateNatureSPARQLWherePart(String varName){
		return NatureRecognitionOrchestrator.getNatureSPARQLWherePart(varName);
	}
	
	protected RDFResourceRole getRoleFromNature(String nature) {
		String roleRaw = nature.split(",")[0];
		
		if (roleRaw.isEmpty()) {
			return RDFResourceRole.undetermined;
		} else {
			return RDFResourceRole.valueOf(roleRaw);
		}
	}

	
}
