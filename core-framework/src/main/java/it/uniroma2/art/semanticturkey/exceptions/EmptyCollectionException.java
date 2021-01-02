package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.Resource;

public class EmptyCollectionException extends DeniedOperationException {

	private static final long serialVersionUID = 6040882674934723267L;

	public EmptyCollectionException(Resource collection) {
		super(EmptyCollectionException.class.getName() + ".message", new Object[] {collection.stringValue()});
	}
}
