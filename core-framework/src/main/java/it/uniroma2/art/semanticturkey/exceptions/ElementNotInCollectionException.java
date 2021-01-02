package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.Resource;

public class ElementNotInCollectionException extends DeniedOperationException {

	private static final long serialVersionUID = 6040882674934723267L;

	public ElementNotInCollectionException(Resource element, Resource collection) {
		super(ElementNotInCollectionException.class.getName() + ".message",
				new Object[] { element.stringValue(), collection.stringValue() });
	}
}
