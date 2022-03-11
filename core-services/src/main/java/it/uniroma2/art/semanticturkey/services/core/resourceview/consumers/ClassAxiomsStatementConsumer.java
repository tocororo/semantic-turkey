package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import com.google.common.collect.Sets;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.RootPropertiesBehavior;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.Arrays;

public class ClassAxiomsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	private static final IRI[] relevantProperties = { OWL.EQUIVALENTCLASS, RDFS.SUBCLASSOF, OWL.DISJOINTWITH,
			// OWL.Res.HASKEY,
			OWL.COMPLEMENTOF, OWL.INTERSECTIONOF,
			// OWL.Res.DISJOINTUNIONOF,
			OWL.ONEOF, OWL.UNIONOF };

	public ClassAxiomsStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "classaxioms", Sets.newLinkedHashSet(Arrays.asList(relevantProperties)),
				new BehaviorOptions().setRootPropertiesBehavior(RootPropertiesBehavior.SHOW));
	}

}
