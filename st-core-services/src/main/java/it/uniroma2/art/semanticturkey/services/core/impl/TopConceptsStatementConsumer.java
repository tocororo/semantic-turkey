package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.SKOS;

public class TopConceptsStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public TopConceptsStatementConsumer() {
		super("topconcepts", SKOS.Res.HASTOPCONCEPT);
	}

}
