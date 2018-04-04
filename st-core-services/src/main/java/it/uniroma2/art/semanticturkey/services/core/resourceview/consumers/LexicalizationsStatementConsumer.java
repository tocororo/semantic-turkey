package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Arrays;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;

import com.google.common.collect.Sets;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class LexicalizationsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public LexicalizationsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "lexicalizations",
				Sets.newLinkedHashSet(Arrays.asList(RDFS.LABEL, SKOS.PREF_LABEL, SKOS.ALT_LABEL,
						SKOS.HIDDEN_LABEL, SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL,
						ONTOLEX.IS_DENOTED_BY)),
				RootProprertiesBehavior.SHOW, CollectionBehavior.IGNORE);
	}

}
