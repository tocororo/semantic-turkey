package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

import java.util.Collections;

public class OtherPropertiesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public OtherPropertiesStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "properties", Collections.emptySet());
	}

}
