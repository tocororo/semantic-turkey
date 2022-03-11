package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.vocabulary.OWL;

import java.util.Collections;

public class DatatypeDefinitionsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public DatatypeDefinitionsStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "datatypeDefinitions", Collections.singleton(OWL.EQUIVALENTCLASS));
	}

}
