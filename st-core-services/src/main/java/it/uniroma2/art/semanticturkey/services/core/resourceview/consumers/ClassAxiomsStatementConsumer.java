package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Arrays;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import com.google.common.collect.Sets;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractGroupingPropertyMatchingStatementConsumer;

public class ClassAxiomsStatementConsumer extends AbstractGroupingPropertyMatchingStatementConsumer {

	private static final IRI[] relevantProperties = {
			OWL.EQUIVALENTCLASS,
			RDFS.SUBCLASSOF,
			OWL.DISJOINTWITH,
			// OWL.Res.HASKEY,
			OWL.COMPLEMENTOF,
			OWL.INTERSECTIONOF,
			//OWL.Res.DISJOINTUNIONOF,
			OWL.ONEOF,
			OWL.UNIONOF
		};

	public ClassAxiomsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "classaxioms", Sets.newLinkedHashSet(Arrays.asList(relevantProperties)));
	}

}
