package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.CollectionBehavior;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.RenderingEngineBehavior;
import org.eclipse.rdf4j.model.vocabulary.OWL;

import java.util.Collections;

public class PropertyChainStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public PropertyChainStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "subPropertyChains",
				Collections.singleton(OWL.PROPERTYCHAINAXIOM),
				new BehaviorOptions().setCollectionBehavior(CollectionBehavior.ALWAYS_ASSUME_COLLECTION)
						.setRenderingEngineBehavior(RenderingEngineBehavior.EXCLUDE));
	}

}
