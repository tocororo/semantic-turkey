package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.SKOS;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class SKOSOrderedCollectionMembersStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public SKOSOrderedCollectionMembersStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "membersOrdered", Collections.singleton(SKOS.MEMBER_LIST), CollectionBehavior.ALWAYS_ASSUME_COLLECTION);
	}

}
