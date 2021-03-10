package it.uniroma2.art.semanticturkey.exceptions.shacl;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public abstract class SHACLGenericException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8506678802954106413L;

	public SHACLGenericException(String key, Object[] args) {
		super(key, args);
	}
}
