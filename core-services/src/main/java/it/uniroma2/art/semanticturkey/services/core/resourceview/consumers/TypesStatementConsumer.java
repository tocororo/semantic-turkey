package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.RDF;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class TypesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public TypesStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "types", Collections.singleton(RDF.TYPE));
	}

}
