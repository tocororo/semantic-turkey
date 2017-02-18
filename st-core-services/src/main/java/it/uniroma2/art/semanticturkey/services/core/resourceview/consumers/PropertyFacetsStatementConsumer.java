package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import static java.util.stream.Collectors.groupingBy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsListSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;

public class PropertyFacetsStatementConsumer extends AbstractStatementConsumer {

	private AbstractPropertyMatchingStatementConsumer inverseOfMatcher;

	public PropertyFacetsStatementConsumer(CustomRangeProvider customRangeProvider) {
		inverseOfMatcher = new AbstractPropertyMatchingStatementConsumer(customRangeProvider, "inverseOf",
				Collections.singleton(OWL.INVERSEOF)) {
			@Override
			protected boolean shouldRetainEmptyGroup(IRI prop, Resource resource,
					ResourcePosition resourcePosition) {
				return prop.equals(OWL.INVERSEOF); // only show empty group for owl:inverseOf
			}
		};
	}

	@Override
	public Set<IRI> getMatchedProperties() {
		return Sets.union(inverseOfMatcher.getMatchedProperties(), Sets.newHashSet(RDF.TYPE, OWL.INVERSEOF));
	}

	@Override
	public Map<String, ResourceViewSection> consumeStatements(Project<?> project,
			ResourcePosition resourcePosition, Resource resource, Model statements,
			Set<Statement> processedStatements, Resource workingGraph,
			Map<Resource, Map<String, Value>> resource2attributes,
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, Model propertyModel) {

		boolean currentProject = false;
		if (resourcePosition instanceof LocalResourcePosition) {
			currentProject = ((LocalResourcePosition) resourcePosition).getProject().equals(project);
		}

		SetView<Resource> typingProps = Sets.union(
				propertyModel.filter(null, RDFS.SUBPROPERTYOF, RDF.TYPE).subjects(),
				Collections.singleton(RDF.TYPE));

		boolean symmetric = false;
		boolean symmetricExplicit = true;

		boolean functional = false;
		boolean functionalExplicit = true;

		boolean inverseFunctional = false;
		boolean inverseFunctionalExplicit = true;

		boolean transitive = false;
		boolean transitiveExplicit = true;

		Map<Value, List<Statement>> propTypes2stmts = statements.stream()
				.filter(s -> typingProps.contains(s.getPredicate()))
				.collect(groupingBy(Statement::getObject));

		if (propTypes2stmts.containsKey(OWL.SYMMETRICPROPERTY)) {
			List<Statement> relevantStmts = propTypes2stmts.get(OWL.SYMMETRICPROPERTY);
			processedStatements.addAll(relevantStmts);
			symmetric = true;
			symmetricExplicit = currentProject
					&& relevantStmts.stream().map(Statement::getContext).anyMatch(workingGraph::equals);
		}

		if (propTypes2stmts.containsKey(OWL.FUNCTIONALPROPERTY)) {
			List<Statement> relevantStmts = propTypes2stmts.get(OWL.FUNCTIONALPROPERTY);
			processedStatements.addAll(relevantStmts);
			functional = true;
			functionalExplicit = currentProject
					&& relevantStmts.stream().map(Statement::getContext).anyMatch(workingGraph::equals);
		}

		if (propTypes2stmts.containsKey(OWL.INVERSEFUNCTIONALPROPERTY)) {
			List<Statement> relevantStmts = propTypes2stmts.get(OWL.INVERSEFUNCTIONALPROPERTY);
			processedStatements.addAll(relevantStmts);
			inverseFunctional = true;
			inverseFunctionalExplicit = currentProject
					&& relevantStmts.stream().map(Statement::getContext).anyMatch(workingGraph::equals);
		}

		if (propTypes2stmts.containsKey(OWL.TRANSITIVEPROPERTY)) {
			List<Statement> relevantStmts = propTypes2stmts.get(OWL.TRANSITIVEPROPERTY);
			processedStatements.addAll(relevantStmts);
			transitive = true;
			transitiveExplicit = currentProject
					&& relevantStmts.stream().map(Statement::getContext).anyMatch(workingGraph::equals);
		}

		Map<String, ResourceViewSection> nestedConsumer = inverseOfMatcher.consumeStatements(project,
				resourcePosition, resource, statements, processedStatements, workingGraph,
				resource2attributes, predicate2resourceCreShow, propertyModel);

		PredicateObjectsListSection inverseOf = (PredicateObjectsListSection) nestedConsumer.get("inverseOf");

		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();
		rv.put("facets",
				new PropertyFacetsSection(symmetric, symmetricExplicit, functional, functionalExplicit,
						inverseFunctional, inverseFunctionalExplicit, transitive, transitiveExplicit,
						inverseOf));

		return rv;
	}

}
