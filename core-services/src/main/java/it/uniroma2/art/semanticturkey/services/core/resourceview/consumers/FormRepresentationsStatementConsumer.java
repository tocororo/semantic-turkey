package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class FormRepresentationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public FormRepresentationsStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "formRepresentations", Collections.singleton(ONTOLEX.REPRESENTATION));
	}

}
