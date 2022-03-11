package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import java.util.Collections;

public class BroadersStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public BroadersStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "broaders", Collections.singleton(SKOS.BROADER));
	}

}
