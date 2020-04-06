package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.OWL;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.CollectionBehavior;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.RenderingEngineBehavior;

public class PropertyChainStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public PropertyChainStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "subPropertyChains",
				Collections.singleton(OWL.PROPERTYCHAINAXIOM),
				new BehaviorOptions().setCollectionBehavior(CollectionBehavior.ALWAYS_ASSUME_COLLECTION)
						.setRenderingEngineBehavior(RenderingEngineBehavior.EXCLUDE));
	}

}
