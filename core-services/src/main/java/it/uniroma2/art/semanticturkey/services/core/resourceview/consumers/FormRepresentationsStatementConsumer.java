package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class FormRepresentationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public FormRepresentationsStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "formRepresentations", Collections.singleton(ONTOLEX.REPRESENTATION));
	}

}
