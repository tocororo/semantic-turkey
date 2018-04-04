package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class EvokedLexicalConcepts extends AbstractPropertyMatchingStatementConsumer {

	public EvokedLexicalConcepts(CustomFormManager customFormManager) {
		super(customFormManager, "evokedLexicalConcepts", Collections.singleton(ONTOLEX.EVOKES));
	}

}
