package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class ClassWithSubclassesOrInstancesException extends DeniedOperationException {

	private static final long serialVersionUID = 8755723822322721067L;

	public ClassWithSubclassesOrInstancesException(IRI cls) {
		super(ClassWithSubclassesOrInstancesException.class.getName() + ".message", new Object[] {cls.stringValue()});
	}
}
