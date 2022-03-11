package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.core.OntoLexLemon;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ConstituentsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	public ConstituentsStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "constituents", Collections.singleton(DECOMP.CONSTITUENT));
	}

	@Override
	protected void sortPredicateValues(List<AnnotatedValue<?>> values, Model statements,
			Resource subjectResource) {

		// if every constituent has an associated position, use it to sort the constituents. Otherwise, sort
		// them by their show (if any)
		boolean allOrdered = OntoLexLemon.sortConstituents(values, av -> (Resource) av.getValue(), statements,
				subjectResource);
		if (!allOrdered) {
			values.sort((a1, a2) -> Objects.compare(a1.getAttributes().get("show"),
					a2.getAttributes().get("show"),
					Comparator.nullsFirst(Comparator.comparing(Value::stringValue))));
		}
	}

}
