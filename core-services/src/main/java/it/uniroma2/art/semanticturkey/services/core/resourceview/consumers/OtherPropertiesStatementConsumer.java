package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class OtherPropertiesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public OtherPropertiesStatementConsumer(ProjectCustomViewsManager projCvManager) {
		super(projCvManager, "properties", Collections.emptySet());
	}

}
