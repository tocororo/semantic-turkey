package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;

import java.util.Collections;

public class LabelRelationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public LabelRelationsStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "labelRelations", Collections.singleton(SKOSXL.LABEL_RELATION));
	}

}
