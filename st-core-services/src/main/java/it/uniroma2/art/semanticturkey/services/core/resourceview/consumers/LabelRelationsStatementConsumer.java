package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class LabelRelationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public LabelRelationsStatementConsumer(CustomRangeProvider customRangeProvider) {
		super(customRangeProvider, "labelRelations", Collections.singleton(SKOSXL.LABEL_RELATION));
	}

}
