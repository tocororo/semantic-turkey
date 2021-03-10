package it.uniroma2.art.semanticturkey.ontology;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class InvalidPrefixException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1982488408050083713L;

	public InvalidPrefixException(String prefix) {
		super(InvalidPrefixException.class.getName() + ".message", new Object[] { prefix });
	}
}
