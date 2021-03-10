package it.uniroma2.art.semanticturkey.exceptions;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class AlreadyExistingLiteralFormForResourceException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7470247664805672183L;

	public AlreadyExistingLiteralFormForResourceException(String key, Object[] args) {
		super(key, args);
	}
}
