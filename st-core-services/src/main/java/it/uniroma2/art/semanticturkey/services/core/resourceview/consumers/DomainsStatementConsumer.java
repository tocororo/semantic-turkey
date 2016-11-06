package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.RDFS;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class DomainsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public DomainsStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "domains", Collections.singleton(RDFS.DOMAIN));
	}

}
