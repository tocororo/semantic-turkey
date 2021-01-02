package it.uniroma2.art.semanticturkey.user;

import org.eclipse.rdf4j.model.IRI;

public class UserNotFoundException extends UserException {

	private static final long serialVersionUID = 1L;

	public UserNotFoundException(IRI iri) {
		super(UserNotFoundException.class.getName() + ".message.with_iri", new Object[] {iri.stringValue()});
	}
	
	public UserNotFoundException(String email) {
		super(UserNotFoundException.class.getName() + ".message.with_email", new Object[] {email});
	}

}
