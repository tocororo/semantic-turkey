package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.RDF;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class TypesStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public TypesStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "types", Collections.singleton(RDF.TYPE));
	}

}
