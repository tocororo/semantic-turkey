package it.uniroma2.art.semanticturkey.services.core;

/**
 * Exception thrown by
 * {@link Export#export(javax.servlet.http.HttpServletResponse, org.eclipse.rdf4j.model.IRI[], it.uniroma2.art.semanticturkey.services.core.export.FilteringPipeline, boolean, org.eclipse.rdf4j.rio.RDFFormat, boolean)}
 * when export preconditions are not met: non-empty null-context or graph named via bnodes.
 * 
 */
public class ExportPreconditionViolationException extends Exception {

	private static final long serialVersionUID = 1L;

	public ExportPreconditionViolationException() {
	}

	public ExportPreconditionViolationException(String message) {
		super(message);
	}

	public ExportPreconditionViolationException(Throwable cause) {
		super(cause);
	}

	public ExportPreconditionViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExportPreconditionViolationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
