package it.uniroma2.art.semanticturkey.exceptions;

public class CustomRangeInitializationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8917035857816097923L;

	public CustomRangeInitializationException(Throwable e) {
		super(e);
	}
	
	public CustomRangeInitializationException(String msg) {
		super(msg);
	}
	
}
