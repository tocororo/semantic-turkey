package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.SKOS;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class SKOSNotesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public SKOSNotesStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "notes", Collections.singleton(SKOS.NOTE));
	}

}
