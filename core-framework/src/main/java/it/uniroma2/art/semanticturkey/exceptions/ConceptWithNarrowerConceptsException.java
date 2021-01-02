package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class ConceptWithNarrowerConceptsException extends DeniedOperationException {

	private static final long serialVersionUID = -2186118342349455413L;

	public ConceptWithNarrowerConceptsException(IRI concept) {
		super(ConceptWithNarrowerConceptsException.class.getName() + ".message", new Object[] {concept.stringValue()});
	}
}
