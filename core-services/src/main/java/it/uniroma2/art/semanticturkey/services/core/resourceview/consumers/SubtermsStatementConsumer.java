package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class SubtermsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public SubtermsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "subterms", Collections.singleton(DECOMP.SUBTERM));
	}

}
