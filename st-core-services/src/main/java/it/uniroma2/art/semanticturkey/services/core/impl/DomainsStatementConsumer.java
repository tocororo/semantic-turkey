package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.services.core.ResourceView;

import java.util.ArrayList;

public class DomainsStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public DomainsStatementConsumer() {
		super("domains", RDFS.Res.DOMAIN);
	}

	@Override
	protected void filterObjectsInPlace(ArrayList<STRDFNode> objects) {
		ResourceView.minimizeDomainRanges(objects);
	}

}
