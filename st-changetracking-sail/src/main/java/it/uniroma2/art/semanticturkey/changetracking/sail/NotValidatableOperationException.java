package it.uniroma2.art.semanticturkey.changetracking.sail;

import org.eclipse.rdf4j.sail.SailException;

/**
 * Exception thrown to indicate that an operation can not undergo validation.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class NotValidatableOperationException extends SailException {

	private static final long serialVersionUID = 9044605473158419217L;

	public NotValidatableOperationException(String msg) {
		super(msg);
	}

}
