package it.uniroma2.art.semanticturkey.exceptions;

public class CustomRangeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8917035857816097923L;

	public CustomRangeException(Throwable e) {
		super(e);
	}
	
	public CustomRangeException(String msg) {
		super(msg);
	}
	
}
