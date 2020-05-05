package it.uniroma2.art.semanticturkey.customform;

public class CustomFormInitializationException extends CustomFormException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2500541467024828346L;
	
	public CustomFormInitializationException() {
		super();
	}

	public CustomFormInitializationException(Throwable e) {
		super(e);
	}
	
	public CustomFormInitializationException(String msg) {
		super(msg);
	}
	
	public CustomFormInitializationException(String msg, Throwable e) {
		super(msg, e);
	}
}
