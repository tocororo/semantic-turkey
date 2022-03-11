package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import java.util.Collections;

public class TopConceptOfStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public TopConceptOfStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "topconceptof", Collections.singleton(SKOS.TOP_CONCEPT_OF));
	}

}
