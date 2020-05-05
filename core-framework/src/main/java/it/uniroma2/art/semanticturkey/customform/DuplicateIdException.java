package it.uniroma2.art.semanticturkey.customform;

public class DuplicateIdException extends CustomFormException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1905157426339276666L;

	public DuplicateIdException() {
		super();
	}

	public DuplicateIdException(Throwable e) {
		super(e);
	}
	
	public DuplicateIdException(String msg) {
		super(msg);
	}
	
	public DuplicateIdException(String msg, Throwable e) {
		super(msg, e);
	}
}
