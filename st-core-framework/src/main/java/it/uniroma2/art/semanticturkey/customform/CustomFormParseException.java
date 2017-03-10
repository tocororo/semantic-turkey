package it.uniroma2.art.semanticturkey.customform;

public class CustomFormParseException extends CustomFormInitializationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5999524090015403565L;

	public CustomFormParseException() {
		super();
	}

	public CustomFormParseException(Throwable e) {
		super(e);
	}
	
	public CustomFormParseException(String msg) {
		super(msg);
	}
	
	public CustomFormParseException(String msg, Throwable e) {
		super(msg, e);
	}
}
