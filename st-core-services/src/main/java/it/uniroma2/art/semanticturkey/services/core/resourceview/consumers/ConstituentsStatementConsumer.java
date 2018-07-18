package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class ConstituentsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public ConstituentsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "constituents", Collections.singleton(DECOMP.CONSTITUENT));
	}

}
