package it.uniroma2.art.semanticturkey.exceptions;

public class OldPasswordMismatchException extends DeniedOperationException {

	private static final long serialVersionUID = 6469124704284323092L;

	public OldPasswordMismatchException() {
		super(OldPasswordMismatchException.class.getName() + ".message", null);
	}
}
