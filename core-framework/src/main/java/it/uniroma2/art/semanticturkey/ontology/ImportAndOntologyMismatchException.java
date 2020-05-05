package it.uniroma2.art.semanticturkey.ontology;

import org.eclipse.rdf4j.model.IRI;

/**
 * Thrown when the URI contained in an owl:imports statement does not match the declared ontology URI.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ImportAndOntologyMismatchException extends OntologyManagerException {

	private static final long serialVersionUID = 1L;
	private IRI ont;
	private IRI realOnt;

	public ImportAndOntologyMismatchException(IRI ont, IRI realOnt) {
		super();
		this.ont = ont;
		this.realOnt = realOnt;
	}

	public IRI getOnt() {
		return ont;
	}

	public IRI getRealOnt() {
		return realOnt;
	}
}
