package it.uniroma2.art.semanticturkey.user;

import org.eclipse.rdf4j.model.IRI;

public class GroupIRIAlreadyUsedException extends UsersGroupException {

	private static final long serialVersionUID = -7826958539875735439L;

	public GroupIRIAlreadyUsedException(IRI iri) {
		super(GroupIRIAlreadyUsedException.class.getName() + ".message", new Object[] { iri.stringValue() });
	}

}
