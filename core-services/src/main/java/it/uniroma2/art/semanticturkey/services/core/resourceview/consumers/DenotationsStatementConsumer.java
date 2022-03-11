package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class DenotationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public DenotationsStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "denotations", Collections.singleton(ONTOLEX.DENOTES));
	}

}
