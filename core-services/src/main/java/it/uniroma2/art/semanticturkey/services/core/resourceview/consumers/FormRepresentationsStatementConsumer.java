package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class FormRepresentationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public FormRepresentationsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "formRepresentations", Collections.singleton(ONTOLEX.REPRESENTATION));
	}

}
