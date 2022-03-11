package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class DenotationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public DenotationsStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "denotations", Collections.singleton(ONTOLEX.DENOTES));
	}

}
