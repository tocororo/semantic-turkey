package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class SubtermsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public SubtermsStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "subterms", Collections.singleton(DECOMP.SUBTERM));
	}

}
