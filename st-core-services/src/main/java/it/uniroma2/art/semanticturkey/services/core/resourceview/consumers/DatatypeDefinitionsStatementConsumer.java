package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.OWL;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class DatatypeDefinitionsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public DatatypeDefinitionsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "datatypeDefinitions", Collections.singleton(OWL.EQUIVALENTCLASS));
	}

}
