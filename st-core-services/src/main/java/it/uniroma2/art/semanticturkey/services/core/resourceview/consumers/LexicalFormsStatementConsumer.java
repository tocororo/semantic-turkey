package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class LexicalFormsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public LexicalFormsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "lexicalForms", Collections.singleton(ONTOLEX.LEXICAL_FORM),
				RootProprertiesBehavior.SHOW_IF_INFORMATIVE, CollectionBehavior.IGNORE,
				SubpropertiesBehavior.INCLUDE);
	}

}
