package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.SubpropertiesBehavior;
import org.eclipse.rdf4j.model.vocabulary.OWL;

import java.util.Collections;

public class OntologyImportsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public OntologyImportsStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "imports", Collections.singleton(OWL.IMPORTS),
				new BehaviorOptions().setSubpropertiesBehavior(SubpropertiesBehavior.EXCLUDE));
	}

}
