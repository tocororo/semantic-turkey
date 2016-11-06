package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.SKOS;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class TopConceptOfStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public TopConceptOfStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "topconceptof", Collections.singleton(SKOS.TOP_CONCEPT_OF));
	}

}
