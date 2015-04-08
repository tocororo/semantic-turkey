package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.RDF;

public class TypesStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public TypesStatementConsumer() {
		super("types", RDF.Res.TYPE);
	}

}
