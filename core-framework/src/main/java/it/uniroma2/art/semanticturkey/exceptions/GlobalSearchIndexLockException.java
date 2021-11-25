package it.uniroma2.art.semanticturkey.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class GlobalSearchIndexLockException extends DeniedOperationException {

	private static final long serialVersionUID = 8715723622342721067L;

	public GlobalSearchIndexLockException() {
		super(GlobalSearchIndexLockException.class.getName() + ".message", new Object[] {});
	}
}
