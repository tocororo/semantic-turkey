package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.Resource;

public class CollectionWithNestedCollectionsException extends DeniedOperationException {

	private static final long serialVersionUID = 6040882674934723267L;

	public CollectionWithNestedCollectionsException(Resource collection) {
		super(CollectionWithNestedCollectionsException.class.getName() + ".message", new Object[] {collection.stringValue()});
	}
}
