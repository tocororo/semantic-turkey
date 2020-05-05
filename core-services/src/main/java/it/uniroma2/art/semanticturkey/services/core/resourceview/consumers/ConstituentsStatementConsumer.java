package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import it.uniroma2.art.lime.model.vocabulary.DECOMP;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;

public class ConstituentsStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	private static final Pattern uriPattern = Pattern
			.compile("^http://www\\.w3\\.org/1999/02/22-rdf-syntax-ns#_\\d+$");

	public ConstituentsStatementConsumer(CustomFormManager customFormManager) {
		super(customFormManager, "constituents", Collections.singleton(DECOMP.CONSTITUENT));
	}

	@Override
	protected void sortPredicateValues(List<AnnotatedValue<?>> values, Model statements,
			Resource subjectResource) {

		boolean allOrdered = true;

		Map<Value, Long> value2Position = new LinkedHashMap<>();

		for (AnnotatedValue<?> annotValue : values) {
			Value value = annotValue.getValue();

			/*
			 * if a value is bound to different rdf:_n, then reduce those value to just one by considering the
			 * following priority: add graph > remove graph > other graph. If there are multiple positions
			 * with the same priority, just pick one arbitrarily.
			 */
			Optional<Long> positionHolder = statements.filter(subjectResource, null, value).stream()
					.filter(stmt -> uriPattern.matcher(stmt.getPredicate().stringValue()).matches())
					.reduce((s1, s2) -> {
						Resource c1 = s1.getContext();
						Resource c2 = s2.getContext();

						if (VALIDATION.isAddGraph(c2)) {
							return s2;
						} else if (VALIDATION.isRemoveGraph(c2) && !VALIDATION.isAddGraph(c1)) {
							return s2;
						} else {
							return s1;
						}
					}).map(stmt -> (Long) Long.parseLong(stmt.getPredicate().stringValue()
							.substring("http://www.w3.org/1999/02/22-rdf-syntax-ns#_".length())));

			if (positionHolder.isPresent()) {
				value2Position.put(value, positionHolder.get());
			} else {
				allOrdered = false;
			}
		}

		// if every constituent has an associated position, use it to sort the constituents. Otherwise, sort
		// them by their show (if any)
		if (allOrdered) {
			values.sort((a1, a2) -> Objects.compare(value2Position.get(a1.getValue()),
					value2Position.get(a2.getValue()), Comparator.naturalOrder()));
		} else {
			values.sort((a1, a2) -> Objects.compare(a1.getAttributes().get("show"),
					a2.getAttributes().get("show"),
					Comparator.nullsFirst(Comparator.comparing(Value::stringValue))));
		}
	}

}
