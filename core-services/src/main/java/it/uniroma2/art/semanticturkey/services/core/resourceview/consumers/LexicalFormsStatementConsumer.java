package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.RootPropertiesBehavior;

import java.util.Collections;

public class LexicalFormsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public LexicalFormsStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "lexicalForms", Collections.singleton(ONTOLEX.LEXICAL_FORM),
				new BehaviorOptions().setRootPropertiesBehavior(RootPropertiesBehavior.SHOW_IF_INFORMATIVE));
	}

}
