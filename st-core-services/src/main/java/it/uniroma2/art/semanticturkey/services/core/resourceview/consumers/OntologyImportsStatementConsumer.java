package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.OWL;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class OntologyImportsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public OntologyImportsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "imports", Collections.singleton(OWL.IMPORTS));
	}

}
