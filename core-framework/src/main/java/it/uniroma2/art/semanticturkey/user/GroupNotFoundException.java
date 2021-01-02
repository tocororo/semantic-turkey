package it.uniroma2.art.semanticturkey.user;

import org.eclipse.rdf4j.model.IRI;

public class GroupNotFoundException extends ProjectBindingException {

	private static final long serialVersionUID = 1L;

	public GroupNotFoundException() {
		super(GroupNotFoundException.class.getName() + ".message", null);
	}
	
	public GroupNotFoundException(IRI group) {
		super(GroupNotFoundException.class.getName() + ".message.with_iri", new Object[] {group.toString()});
	}


}
