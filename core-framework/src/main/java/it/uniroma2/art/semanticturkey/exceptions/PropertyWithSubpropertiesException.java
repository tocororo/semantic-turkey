package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class PropertyWithSubpropertiesException extends DeniedOperationException {

	private static final long serialVersionUID = 8755723822322721067L;

	public PropertyWithSubpropertiesException(IRI prop) {
		super(PropertyWithSubpropertiesException.class.getName() + ".message", new Object[] {prop.stringValue()});
	}
}
