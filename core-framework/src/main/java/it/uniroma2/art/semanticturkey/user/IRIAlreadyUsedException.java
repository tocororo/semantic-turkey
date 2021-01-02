package it.uniroma2.art.semanticturkey.user;

import org.eclipse.rdf4j.model.IRI;

public class IRIAlreadyUsedException extends UserException {

	private static final long serialVersionUID = 7167056496677759423L;

	public IRIAlreadyUsedException(IRI iri) {
		super(IRIAlreadyUsedException.class.getName() + ".message", new Object[] {iri.stringValue()});
	}
}
