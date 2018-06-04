package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;

public class PropertyChainStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public PropertyChainStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "subPropertyChains",
				Collections.singleton(OWL2Fragment.PROPERTY_CHAIN_AXIOM), RootProprertiesBehavior.HIDE,
				CollectionBehavior.ALWAYS_ASSUME_COLLECTION, SubpropertiesBehavior.INCLUDE);
	}

}