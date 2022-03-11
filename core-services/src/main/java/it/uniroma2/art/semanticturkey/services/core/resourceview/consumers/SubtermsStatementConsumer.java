package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class SubtermsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public SubtermsStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "subterms", Collections.singleton(DECOMP.SUBTERM));
	}

}
