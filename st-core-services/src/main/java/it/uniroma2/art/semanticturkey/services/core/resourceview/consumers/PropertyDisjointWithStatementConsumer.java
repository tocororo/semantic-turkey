package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;

public class PropertyDisjointWithStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public PropertyDisjointWithStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "disjointProperties",
				Collections.singleton(OWL2Fragment.PROPERTY_DISJOINT_WITH));
	}

}
