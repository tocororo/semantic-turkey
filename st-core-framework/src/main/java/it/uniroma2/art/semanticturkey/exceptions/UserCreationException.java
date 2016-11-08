package it.uniroma2.art.semanticturkey.exceptions;

public class UserCreationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7826958539875735439L;

	public UserCreationException(Throwable e) {
		super(e);
	}
	
	public UserCreationException(String msg) {
		super(msg);
	}

}
