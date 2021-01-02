package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class UsersGroupException extends InternationalizedException {

	public UsersGroupException(String key, Object[] args) {
		super(key, args);
	}

	private static final long serialVersionUID = -7826958539875735439L;

	public UsersGroupException(Throwable e) {
		super(e);
	}

	
}
