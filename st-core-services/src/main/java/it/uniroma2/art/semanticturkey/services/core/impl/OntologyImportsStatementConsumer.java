package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.OWL;

public class OntologyImportsStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public OntologyImportsStatementConsumer() {
		super("imports", OWL.Res.IMPORTS);
	}

}
