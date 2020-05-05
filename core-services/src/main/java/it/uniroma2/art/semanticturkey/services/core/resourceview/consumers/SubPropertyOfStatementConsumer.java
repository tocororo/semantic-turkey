package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.RDFS;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class SubPropertyOfStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public SubPropertyOfStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "superproperties", Collections.singleton(RDFS.SUBPROPERTYOF));
	}

}
