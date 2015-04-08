package it.uniroma2.art.semanticturkey.services.core.impl;

import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNode;
import it.uniroma2.art.semanticturkey.services.core.ResourceView;

import java.util.ArrayList;

public class RangesStatementConsumer extends PropertyMatchingAbstractStatementConsumer {

	public RangesStatementConsumer() {
		super("ranges", RDFS.Res.RANGE);
	}

	@Override
	protected void filterObjectsInPlace(ArrayList<STRDFNode> objects) {
		ResourceView.minimizeDomainRanges(objects);
	}

}
