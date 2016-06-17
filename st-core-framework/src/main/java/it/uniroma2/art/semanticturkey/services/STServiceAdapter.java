package it.uniroma2.art.semanticturkey.services;

import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTResource;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.exceptions.NonExistingRDFResourceException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.SerializationType;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;

public class STServiceAdapter implements STService, NewStyleService {

	@Autowired
	private STServiceContext stServiceContext;
	
	protected ServletUtilities servletUtilities = ServletUtilities.getService();
	
	public OWLModel getOWLModel() {
		return stServiceContext.getProject().getOWLModel();
	}
	
	public Project<? extends RDFModel> getProject() {
		return stServiceContext.getProject();
	}

	public ARTResource[] getUserNamedGraphs() {
		return stServiceContext.getRGraphs();
	}
	
	public ARTResource getWorkingGraph() {
		return stServiceContext.getWGraph();
	}
	
	public ARTResource getMetadataGraph() {
		return stServiceContext.getProject().getMetadataGraph(stServiceContext.getExtensionPathComponent());
	}
	
	/**
	 * This should not be used (since there are the Converters). It can be used in particular cases to avoid
	 * controls performed by the Converter (see Refactor#renameResource())
	 */
	protected ARTURIResource retrieveExistingURIResource(RDFModel model, String qname)
			throws NonExistingRDFResourceException, ModelAccessException {
		ARTURIResource res = RDFNodeSerializer.createURI(model, qname);
		ARTResource[] graphs = stServiceContext.getRGraphs();
		if (model.existsResource(res, graphs))
			return res;
		throw new NonExistingRDFResourceException(res, graphs);
	}

	private String getRequest() {
		return this.getClass().getSimpleName();
	}

	protected Response logAndSendException(Exception e) {
		return logAndSendException(getRequest(), e);
	}

	protected Response logAndSendException(Exception e, String msg) {
		return logAndSendException(getRequest(), e, msg);
	}

	protected Response logAndSendException(String msg) {
		return logAndSendException(getRequest(), msg);
	}

	/**
	 * this convenience method prepares an exception response initialized with the given arguments, logs the
	 * occurred exception with level "error" and prints the stack trace
	 * 
	 * @param request
	 * @param e
	 * @return
	 */
	protected Response logAndSendException(String request, Exception e) {
		return logAndSendException(request, e.toString());
	}

	protected Response logAndSendException(String request, String msg) {
		// getLogger().error(msg);
		return servletUtilities.createExceptionResponse(request, msg);
	}

	/**
	 * this convenience method prepares an exception response initialized with the given arguments and logs
	 * the occurred exception with level "error"
	 * 
	 * @param request
	 * @param e
	 * @param msg
	 * @return
	 */
	protected Response logAndSendException(String request, Exception e, String msg) {
		e.printStackTrace(System.err);
		// getLogger().error(e.toString());
		return servletUtilities.createExceptionResponse(request, msg);
	}

	protected Response logAndSendException(String request, String msg, SerializationType sertype) {
		// getLogger().error(msg);
		return servletUtilities.createExceptionResponse(request, msg, sertype);
	}
	
	protected XMLResponseREPLY createReplyResponse(RepliesStatus status) {
		return servletUtilities.createReplyResponse(stServiceContext.getRequest().getServiceMethod(), status);
	}
	
	protected XMLResponseREPLY createReplyFAIL(String message) {
		return servletUtilities.createReplyFAIL(stServiceContext.getRequest().getServiceMethod(), message);
	}
	
	protected XMLResponseREPLY createBooleanResponse(boolean resp) {
		return servletUtilities.createBooleanResponse(stServiceContext.getRequest().getServiceMethod(), resp);
	}

}
