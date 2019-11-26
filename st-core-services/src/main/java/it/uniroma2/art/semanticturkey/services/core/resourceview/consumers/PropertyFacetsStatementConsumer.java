package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsListSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsSection.FacetStructure;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;

public class PropertyFacetsStatementConsumer extends AbstractStatementConsumer {

	private static List<Pair<IRI, String>> facetClassAndNameList = Arrays.asList(
			new ImmutablePair<>(OWL.SYMMETRICPROPERTY, "symmetric"),
			new ImmutablePair<>(OWL2Fragment.ASYMMETRICPROPERTY, "asymmetric"),
			new ImmutablePair<>(OWL.FUNCTIONALPROPERTY, "functional"),
			new ImmutablePair<>(OWL.INVERSEFUNCTIONALPROPERTY, "inverseFunctional"),
			new ImmutablePair<>(OWL2Fragment.REFLEXIVEPROPERTY, "reflexive"),
			new ImmutablePair<>(OWL2Fragment.IRREFLEXIVEPROPERTY, "irreflexive"),
			new ImmutablePair<>(OWL.TRANSITIVEPROPERTY, "transitive"));

	private AbstractPropertyMatchingStatementConsumer inverseOfMatcher;

	public PropertyFacetsStatementConsumer(CustomFormManager customFormManager) {
		inverseOfMatcher = new AbstractPropertyMatchingStatementConsumer(customFormManager, "inverseOf",
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
	public Map<String, ResourceViewSection> consumeStatements(Project project, RepositoryConnection repoConn,
			ResourcePosition resourcePosition, Resource resource, Model statements,
			Set<Statement> processedStatements, Resource workingGraph,
			Map<Resource, Map<String, Value>> resource2attributes,
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, Model propertyModel) {

		boolean currentProject;
		if (resourcePosition instanceof LocalResourcePosition) {
			currentProject = ((LocalResourcePosition) resourcePosition).getProject().equals(project);
		} else {
			currentProject = false;
		}

		SetView<Resource> typingProps = Sets.union(
				propertyModel.filter(null, RDFS.SUBPROPERTYOF, RDF.TYPE).subjects(),
				Collections.singleton(RDF.TYPE));

		Map<Value, List<Statement>> propTypes2stmts = statements.stream()
				.filter(s -> typingProps.contains(s.getPredicate()))
				.collect(groupingBy(Statement::getObject));

		Map<String, FacetStructure> facets = new LinkedHashMap<>();

		String propertyNature = resource2attributes.getOrDefault(resource, Collections.emptyMap())
				.getOrDefault("nature", SimpleValueFactory.getInstance().createLiteral("")).stringValue();
		RDFResourceRole propertyRole = STServiceAdapter.getRoleFromNature(propertyNature);
		Optional<IRI> graphFromSubjectNature = STServiceAdapter.getGraphFromNature(propertyNature);
		boolean propertyExplicitness = currentProject
				&& graphFromSubjectNature.filter(g ->
					workingGraph.equals(g) || VALIDATION.isRemoveGraphFor(g, workingGraph) || VALIDATION.isAddGraphFor(g, workingGraph)).isPresent();

		for (Pair<IRI, String> facetClassAndName : facetClassAndNameList) {
			IRI facetClass = facetClassAndName.getLeft();
			String facetName = facetClassAndName.getRight();

			boolean value = propTypes2stmts.containsKey(facetClass);
			List<Statement> relevantStmts = propTypes2stmts.getOrDefault(facetClass, Collections.emptyList());
			Set<Resource> graphs = relevantStmts.stream().map(Statement::getContext).collect(toSet());

			if (graphs.isEmpty()) {
				Resource syntheticGraph = graphFromSubjectNature.map(Resource.class::cast).orElseGet(
						() -> currentProject ? workingGraph : NatureRecognitionOrchestrator.INFERENCE_GRAPH);
				if (VALIDATION.isAddGraph(syntheticGraph)) {
					syntheticGraph = VALIDATION.unmangleAddGraph((IRI) syntheticGraph);
				} else if (VALIDATION.isRemoveGraph(syntheticGraph)) {
					syntheticGraph = VALIDATION.unmangleRemoveGraph((IRI) syntheticGraph);
				}
				graphs = Collections.singleton(syntheticGraph);
			}

			boolean explicit = propertyExplicitness && (!value || graphs.contains(workingGraph));

			/*
			 * A facet is reported if it is true, or (despite being fale) it can be applied to the given type
			 * of property. "symmetric" can be applied to both object and data property, while the other
			 * facets can be applied to object properties only, and by extension to RDF properties (which by
			 * side-effect becomes object properties)
			 */

			if (value
					|| (OWL.FUNCTIONALPROPERTY.equals(facetClass)
							&& propertyRole != RDFResourceRole.annotationProperty
							&& propertyRole != RDFResourceRole.ontologyProperty)
					|| (propertyRole != RDFResourceRole.datatypeProperty
							&& propertyRole != RDFResourceRole.annotationProperty
							&& propertyRole != RDFResourceRole.ontologyProperty)) {
				facets.put(facetName,
						new FacetStructure(value, explicit, computeTripleScope(graphs, workingGraph)));
			}

		}

		Map<String, ResourceViewSection> nestedConsumer = inverseOfMatcher.consumeStatements(project, null,
				resourcePosition, resource, statements, processedStatements, workingGraph,
				resource2attributes, predicate2resourceCreShow, propertyModel);

		PredicateObjectsListSection inverseOf = (PredicateObjectsListSection) nestedConsumer.get("inverseOf");

		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();
		rv.put("facets", new PropertyFacetsSection(facets, inverseOf));

		return rv;
	}

}
