package it.uniroma2.art.semanticturkey.user;

public class UsersGroupException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7826958539875735439L;

	public UsersGroupException(Throwable e) {
		super(e);
	}
	
	public UsersGroupException(String msg) {
		super(msg);
	}

}
