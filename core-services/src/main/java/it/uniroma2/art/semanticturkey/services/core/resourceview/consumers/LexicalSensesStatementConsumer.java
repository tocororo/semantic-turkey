package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class LexicalSensesStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public LexicalSensesStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "lexicalSenses", Collections.singleton(ONTOLEX.SENSE));
	}

}
