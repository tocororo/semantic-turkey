package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;

import org.eclipse.rdf4j.model.vocabulary.SKOSXL;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class LabelRelationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer{

	public LabelRelationsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "labelRelations", Collections.singleton(SKOSXL.LABEL_RELATION));
	}

}
