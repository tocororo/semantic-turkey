package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.SKOS;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.CollectionBehavior;

public class SKOSOrderedCollectionMembersStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public SKOSOrderedCollectionMembersStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "membersOrdered", Collections.singleton(SKOS.MEMBER_LIST),
				new BehaviorOptions().setCollectionBehavior(CollectionBehavior.ALWAYS_ASSUME_COLLECTION));
	}

}
