package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.OWL;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class OntologyImportsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public OntologyImportsStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "imports", Collections.singleton(OWL.IMPORTS));
	}

}
