package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.RDFS;

public class SubClassOfStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public SubClassOfStatementConsumer() {
		super("supertypes", RDFS.Res.SUBCLASSOF);
	}

}
