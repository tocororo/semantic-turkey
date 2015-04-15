package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.SKOS;

public class HasTopConceptStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public HasTopConceptStatementConsumer() {
		super("topconcepts", SKOS.Res.HASTOPCONCEPT);
	}

}
