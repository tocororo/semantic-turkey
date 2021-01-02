package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class UserException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7826958539875735439L;

	public UserException(Throwable e) {
		super(e);
	}
	
	public UserException(String key, Object[] args) {
		super(key, args);
	}

}
