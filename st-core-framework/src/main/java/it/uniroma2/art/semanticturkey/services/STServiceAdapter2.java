package it.uniroma2.art.semanticturkey.services;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.rdf4jimpl.RDF4JARTResourceFactory;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.tx.STServiceAspect;
import it.uniroma2.art.semanticturkey.tx.STServiceInvocaton;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

/**
 * Base class of Semantic Turkey services.
 * 
 * @author <a href="mailto:manuel.fiorelli@gmail.com">Manuel Fiorelli</a>
 *
 */
public class STServiceAdapter2 implements STService, NewerNewStyleService {

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	private PlatformTransactionManager txManager;

	private final ValueFactory sesVf;
	private final RDF4JARTResourceFactory rdf4j2artFact;
	private final ServletUtilities servletUtilities;

	protected STServiceAdapter2() {
		sesVf = SimpleValueFactory.getInstance();
		rdf4j2artFact = new RDF4JARTResourceFactory(sesVf);
		servletUtilities = ServletUtilities.getService();
	}

	protected PlatformTransactionManager getPlatformTransactionManager() {
		return txManager;
	}

	public Project<? extends RDFModel> getProject() {
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

	public Resource getMetadataGraph() {
		return rdf4j2artFact.aRTResource2RDF4JResource(
				stServiceContext.getProject().getMetadataGraph(stServiceContext.getExtensionPathComponent()));
	}

	public RepositoryConnection getRepositoryConnection() {
		Repository repo = getProject().getRepository();
		return RDF4JRepositoryUtils.getConnection(repo);
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
			throw new IllegalStateException(STServiceAdapter2.class.getSimpleName()
					+ ".applyPath(..) may only invoked within the executon of a service method");
		}

		System.out.println("=== BEGIN PATCH ===");

		System.out.println(currentServiceInvocation);

		System.out.println();

		quadAdditions.forEach(stmt -> System.out.println("+ " + NTriplesUtil.toNTriplesString(stmt.getSubject())
				+ " " + NTriplesUtil.toNTriplesString(stmt.getPredicate()) + " "
				+ NTriplesUtil.toNTriplesString(stmt.getObject()) + Optional.ofNullable(stmt.getContext())
						.map(c -> NTriplesUtil.toNTriplesString(c)).orElse("")));
		quadAdditions.forEach(stmt -> System.out.println("- " + NTriplesUtil.toNTriplesString(stmt.getSubject())
		+ " " + NTriplesUtil.toNTriplesString(stmt.getPredicate()) + " "
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

	/**
	 * Returns a new URI for a resource. The parameter {@code xRole} holds the nature of the resource that
	 * will be identified with the given URI. Depending on the value of the parameter {@code xRole}, a
	 * conforming converter may generate differently shaped URIs, possibly using specific arguments passed via
	 * the map {@code args}.
	 * 
	 * @param xRole
	 * @param valueMapping
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateURI(String xRole, Map<String, Value> valueMapping) throws URIGenerationException {
		try {
			Map<String, ARTNode> artValueMapping = valueMapping.entrySet().stream().collect(Collectors
					.toMap(Map.Entry::getKey, (entry) -> rdf4j2artFact.rdf4jValue2ARTNode(entry.getValue())));

			ARTURIResource artURIRes = getProject().getURIGenerator().generateURI(stServiceContext, xRole,
					artValueMapping);

			return sesVf.createIRI(artURIRes.getURI());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
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

	// Deprecated

	@Override
	public OWLModel getOWLModel() {
		// TODO Auto-generated method stub
		return null;
	}

}
