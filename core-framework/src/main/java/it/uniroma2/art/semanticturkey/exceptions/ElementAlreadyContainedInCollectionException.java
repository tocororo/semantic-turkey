package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.Resource;

public class ElementAlreadyContainedInCollectionException extends DeniedOperationException {

	private static final long serialVersionUID = -1776967626380427497L;

	public ElementAlreadyContainedInCollectionException(Resource element, Resource collection) {
		super(ElementAlreadyContainedInCollectionException.class.getName() + ".message",
				new Object[] { element.stringValue(), collection.stringValue() });
	}
}
