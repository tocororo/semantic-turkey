package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Arrays;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;

import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.services.core.ResourceView2;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractGroupingPropertyMatchingStatementConsumer;

public class LexicalizationsStatementConsumer extends AbstractGroupingPropertyMatchingStatementConsumer {

	public LexicalizationsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "lexicalizations",
				Sets.newLinkedHashSet(Arrays.asList(RDFS.LABEL, SKOS.PREF_LABEL, SKOS.ALT_LABEL,
						SKOS.HIDDEN_LABEL, SKOSXL.PREF_LABEL, SKOSXL.ALT_LABEL, SKOSXL.HIDDEN_LABEL)));
	}

	@Override
	protected boolean shouldRetainEmptyOuterGroup(IRI superProp, Resource resource,
			ResourcePosition resourcePosition) {
		return ResourceView2.getLexicalizationPropertiesHelper(resource, resourcePosition)
				.contains(superProp);
	}

}
