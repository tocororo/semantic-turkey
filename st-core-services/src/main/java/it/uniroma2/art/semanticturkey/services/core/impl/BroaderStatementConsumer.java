package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.SKOS;

public class BroaderStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public BroaderStatementConsumer() {
		super("broaders", SKOS.Res.BROADER);
	}

}
