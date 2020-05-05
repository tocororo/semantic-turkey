package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class LexicalSensesStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public LexicalSensesStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "lexicalSenses", Collections.singleton(ONTOLEX.SENSE));
	}

}
