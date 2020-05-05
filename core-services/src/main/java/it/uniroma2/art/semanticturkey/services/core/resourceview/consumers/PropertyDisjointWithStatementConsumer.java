package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.OWL;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class PropertyDisjointWithStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public PropertyDisjointWithStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "disjointProperties",
				Collections.singleton(OWL.PROPERTYDISJOINTWITH));
	}

}
