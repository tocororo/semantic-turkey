package it.uniroma2.art.semanticturkey.exceptions;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class PrefAltLabelClashException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7470247664805672183L;

	public PrefAltLabelClashException(String key, Object[] args) {
		super(key, args);
	}
}
