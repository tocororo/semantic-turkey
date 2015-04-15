package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.SKOS;

public class TopConceptOfStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public TopConceptOfStatementConsumer() {
		super("topconceptof", SKOS.Res.TOPCONCEPTOF);
	}

}
