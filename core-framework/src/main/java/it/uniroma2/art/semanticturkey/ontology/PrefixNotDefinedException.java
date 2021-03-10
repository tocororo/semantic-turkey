package it.uniroma2.art.semanticturkey.ontology;

import it.uniroma2.art.semanticturkey.i18n.InternationalizedException;

public class PrefixNotDefinedException extends InternationalizedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1982488408050083713L;

	public PrefixNotDefinedException(String prefix) {
		super(PrefixNotDefinedException.class.getName() + ".message", new Object[] { prefix });
	}
}
