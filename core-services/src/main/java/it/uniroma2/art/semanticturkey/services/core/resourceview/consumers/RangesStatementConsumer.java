package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.Collections;

public class RangesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public RangesStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "ranges", Collections.singleton(RDFS.RANGE));
	}

}
