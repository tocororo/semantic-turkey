package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.SKOS;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class TopConceptOfStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public TopConceptOfStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "topconceptof", Collections.singleton(SKOS.TOP_CONCEPT_OF));
	}

}
