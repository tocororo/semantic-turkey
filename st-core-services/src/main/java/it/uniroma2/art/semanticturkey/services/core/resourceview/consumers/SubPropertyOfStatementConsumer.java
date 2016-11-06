package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.RDFS;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class SubPropertyOfStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public SubPropertyOfStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "superproperties", Collections.singleton(RDFS.SUBPROPERTYOF));
	}

}
