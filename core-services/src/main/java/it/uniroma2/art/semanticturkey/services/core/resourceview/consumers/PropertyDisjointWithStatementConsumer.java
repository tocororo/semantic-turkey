package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.vocabulary.OWL;

import java.util.Collections;

public class PropertyDisjointWithStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public PropertyDisjointWithStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "disjointProperties",
				Collections.singleton(OWL.PROPERTYDISJOINTWITH));
	}

}
