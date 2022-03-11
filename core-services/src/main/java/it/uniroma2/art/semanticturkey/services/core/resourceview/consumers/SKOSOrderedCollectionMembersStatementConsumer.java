package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.CollectionBehavior;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import java.util.Collections;

public class SKOSOrderedCollectionMembersStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public SKOSOrderedCollectionMembersStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "membersOrdered", Collections.singleton(SKOS.MEMBER_LIST),
				new BehaviorOptions().setCollectionBehavior(CollectionBehavior.ALWAYS_ASSUME_COLLECTION));
	}

}
