package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.RDFS;

public class SubPropertyOfStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public SubPropertyOfStatementConsumer() {
		super("superproperties", RDFS.Res.SUBPROPERTYOF);
	}

}
