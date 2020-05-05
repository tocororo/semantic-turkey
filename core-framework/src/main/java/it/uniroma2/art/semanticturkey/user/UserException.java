package it.uniroma2.art.semanticturkey.user;

public class UserException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7826958539875735439L;

	public UserException(Throwable e) {
		super(e);
	}
	
	public UserException(String msg) {
		super(msg);
	}

}
