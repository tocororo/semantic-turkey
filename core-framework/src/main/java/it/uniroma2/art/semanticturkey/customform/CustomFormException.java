package it.uniroma2.art.semanticturkey.customform;

public class CustomFormException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3694637681811302195L;

	public CustomFormException() {
		super();
	}

	public CustomFormException(Throwable e) {
		super(e);
	}
	
	public CustomFormException(String msg) {
		super(msg);
	}
	
	public CustomFormException(String msg, Throwable e) {
		super(msg, e);
	}
	
}
