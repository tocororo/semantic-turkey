package it.uniroma2.art.semanticturkey.services.core.export;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;
import it.uniroma2.art.semanticturkey.services.core.Export;

/**
 * Exception thrown by
 * {@link Export#export(javax.servlet.http.HttpServletResponse, org.eclipse.rdf4j.model.IRI[], it.uniroma2.art.semanticturkey.services.core.export.FilteringPipeline, boolean, org.eclipse.rdf4j.rio.RDFFormat, boolean)}
 * when export preconditions are not met: non-empty null-context or graph named via bnodes.
 * 
 */
public class ExportPreconditionViolationException extends InternationalizedException {

	private static final long serialVersionUID = 1L;

	public ExportPreconditionViolationException(String key, Object[] args) {
		super(key, args);
	}

}
