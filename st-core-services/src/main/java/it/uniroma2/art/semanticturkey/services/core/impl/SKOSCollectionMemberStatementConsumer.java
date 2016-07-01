package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.SKOS;

public class SKOSCollectionMemberStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public SKOSCollectionMemberStatementConsumer() {
		super("members", SKOS.Res.MEMBER);
	}

}
