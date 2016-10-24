package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.SKOSXL;

// TODO support classification of properties, so that this consumer will also match subproperties of skosxl:labelRelation
public class LabelRelationStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public LabelRelationStatementConsumer() {
		super("labelRelations", SKOSXL.Res.LABELRELATION);
	}

}
