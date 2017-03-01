package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.SKOS;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class InSchemeStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public InSchemeStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "schemes", Collections.singleton(SKOS.IN_SCHEME));
	}

}
