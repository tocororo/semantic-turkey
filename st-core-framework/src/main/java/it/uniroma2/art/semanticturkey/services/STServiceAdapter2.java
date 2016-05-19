package it.uniroma2.art.semanticturkey.services;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openrdf.model.IRI;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.owlart.sesame4impl.Sesame4ARTResourceFactory;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.tx.STServiceAspect;
import it.uniroma2.art.semanticturkey.tx.STServiceInvocaton;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.tx.TransactionAwareRDF4JRepostoryConnection;
import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

/**
 * Base class of Semantic Turkey services.
 * 
 * @author <a href="mailto:manuel.fiorelli@gmail.com">Manuel Fiorelli</a>
 *
 */
public class STServiceAdapter2 implements STService {

	@Autowired
	private STServiceContext stServiceContext;

	@Autowired
	private PlatformTransactionManager txManager;

	private final ValueFactory sesVf;
	private final Sesame4ARTResourceFactory ses2artFact;
	private final ServletUtilities servletUtilities;

	protected STServiceAdapter2() {
		sesVf = SimpleValueFactory.getInstance();
		ses2artFact = new Sesame4ARTResourceFactory(sesVf);
		servletUtilities = ServletUtilities.getService();
	}

	protected PlatformTransactionManager getPlatformTransactionManager() {
		return txManager;
	}

	public Project<? extends RDFModel> getProject() {
		return stServiceContext.getProject();
	}

	public Resource[] getUserNamedGraphs() {
		return Arrays.stream(stServiceContext.getRGraphs()).map(ses2artFact::aRTResource2SesameResource)
				.toArray(Resource[]::new);
	}

	public Resource getWorkingGraph() {
		return ses2artFact.aRTResource2SesameResource(stServiceContext.getWGraph());
	}

	public Resource getMetadataGraph() {
		return ses2artFact.aRTResource2SesameResource(
				stServiceContext.getProject().getMetadataGraph(stServiceContext.getExtensionPathComponent()));
	}

	public RepositoryConnection getRepositoryConnection() {
		Repository repo = getProject().getRepository();
		RepositoryConnection rawRepoConn = RDF4JRepositoryUtils.getConnection(repo);
		return new TransactionAwareRDF4JRepostoryConnection(repo, rawRepoConn);
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
					.toMap(Map.Entry::getKey, (entry) -> ses2artFact.sesameValue2ARTNode(entry.getValue())));

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
