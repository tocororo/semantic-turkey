package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customviews.ProjectCustomViewsManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.RootPropertiesBehavior;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RDFSMembersStatementConsumer extends AbstractPropertyMatchingStatementConsumer {

	private static final Pattern uriPattern = Pattern
			.compile("^http://www\\.w3\\.org/1999/02/22-rdf-syntax-ns#_\\d+$");

	public RDFSMembersStatementConsumer(CustomFormManager cfManager, ProjectCustomViewsManager projCvManager) {
		super(cfManager, projCvManager, "rdfsMembers", Collections.singleton(RDFS.MEMBER),
				new BehaviorOptions().setRootPropertiesBehavior(RootPropertiesBehavior.HIDE));
	}

	@Override
	protected List<IRI> computeRelavantProperties(Model propertyModel, Model statements,
			Set<Statement> processedStatements) {
		return Stream.concat(
				statements.predicates().stream()
						.filter(iri -> uriPattern.matcher(iri.stringValue()).matches()),
				super.computeRelavantProperties(propertyModel, statements, processedStatements).stream())
				.sorted((i1, i2) -> {
					String s1 = i1.stringValue();
					String s2 = i2.stringValue();

					if (uriPattern.matcher(s1).matches() && uriPattern.matcher(s2).matches()) {
						int int1 = Integer.parseInt(
								s1.substring("http://www.w3.org/1999/02/22-rdf-syntax-ns#_".length()));
						int int2 = Integer.parseInt(
								s2.substring("http://www.w3.org/1999/02/22-rdf-syntax-ns#_".length()));

						return Integer.compare(int1, int2);
					} else {
						return s1.compareTo(s2);
					}
				}).distinct().collect(Collectors.toList());
	}

	@Override
	protected boolean shouldRetainEmptyResult(String sectionName, RDFResourceRole resourceRole) {
		return RDFResourceRole.subsumes(RDFResourceRole.ontolexLexicalEntry, resourceRole);
	}

}
