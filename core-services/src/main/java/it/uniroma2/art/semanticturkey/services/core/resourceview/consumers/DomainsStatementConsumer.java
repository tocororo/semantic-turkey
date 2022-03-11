package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.Collections;

public class DomainsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public DomainsStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "domains", Collections.singleton(RDFS.DOMAIN));
	}

}
