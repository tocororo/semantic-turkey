package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.SKOS;

public class InSchemeStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public InSchemeStatementConsumer() {
		super("schemes", SKOS.Res.INSCHEME);
	}

}
