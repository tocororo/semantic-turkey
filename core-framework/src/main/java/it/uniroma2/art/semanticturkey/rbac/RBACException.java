package it.uniroma2.art.semanticturkey.rbac;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class RBACException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -619227838879011063L;
	
	public RBACException(Exception e) {
		super(e);
	}
	
	public RBACException(String key, Object[] args) {
		this(key, args, null);
	}

	public RBACException(String key, Object[] args, Throwable cause) {
		super(key, args, cause);
	}

}
