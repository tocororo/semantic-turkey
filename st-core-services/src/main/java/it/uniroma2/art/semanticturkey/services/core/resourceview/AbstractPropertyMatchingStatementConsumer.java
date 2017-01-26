package it.uniroma2.art.semanticturkey.services.core.resourceview;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import it.uniroma2.art.owlart.rdf4jimpl.model.ARTURIResourceRDF4JImpl;
import it.uniroma2.art.owlart.vocabulary.RDF;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.vocabulary.STVocabUtilities;

public class AbstractPropertyMatchingStatementConsumer extends AbstractStatementConsumer {

	public enum CollectionBehavior {
		IGNORE, ALWAYS_ASSUME_COLLECTION
	};

	private CustomRangeProvider customRangeProvider;
	private String sectionName;
	private Set<IRI> matchedProperties;
	private CollectionBehavior collectionBehavior;

	public AbstractPropertyMatchingStatementConsumer(CustomRangeProvider customRangeProvider,
			String sectionName, Set<IRI> matchedProperties, CollectionBehavior collectionBehavior) {
		this.customRangeProvider = customRangeProvider;
		this.sectionName = sectionName;
		this.matchedProperties = matchedProperties;
		this.collectionBehavior = collectionBehavior;
	}

	public AbstractPropertyMatchingStatementConsumer(CustomRangeProvider customRangeProvider,
			String sectionName, Set<IRI> matchedProperties) {
		this(customRangeProvider, sectionName, matchedProperties, CollectionBehavior.IGNORE);
	}

	@Override
	public Set<IRI> getMatchedProperties() {
		return matchedProperties;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, ResourceViewSection> consumeStatements(Project<?> project,
			ResourcePosition resourcePosition, Resource resource, Model statements,
			Set<Statement> processedStatements, Resource workingGraph,
			Map<Resource, Map<String, Value>> resource2attributes, Model propertyModel) {

		boolean currentProject = false;
		if (resourcePosition instanceof LocalResourcePosition) {
			currentProject = ((LocalResourcePosition) resourcePosition).getProject().equals(project);
		}

		Map<IRI, AnnotatedValue<IRI>> propMap = new HashMap<>();
		Multimap<IRI, AnnotatedValue<?>> valueMultiMap = HashMultimap.create();

		Set<IRI> relevantProperties;
		LinkedHashModel pendingDirectKnowledge;

		if (matchedProperties.isEmpty()) {
			pendingDirectKnowledge = new LinkedHashModel(statements.filter(resource, null, null).stream()
					.filter(stmt -> !processedStatements.contains(stmt)).collect(toList()));
			relevantProperties = pendingDirectKnowledge.predicates();
		} else {
			relevantProperties = Sets.union(matchedProperties.stream()
					.flatMap(prop -> propertyModel.filter(null, RDFS.SUBPROPERTYOF, prop).stream())
					.map(stmt -> (IRI) stmt.getSubject()).collect(toSet()), matchedProperties);
			pendingDirectKnowledge = new LinkedHashModel(statements.filter(resource, null, null).stream()
					.filter(stmt -> !processedStatements.contains(stmt)
							&& relevantProperties.contains(stmt.getPredicate()))
					.collect(toList()));
		}

		for (IRI predicate : relevantProperties) {
			if (STVocabUtilities.isHiddenResource(new ARTURIResourceRDF4JImpl(predicate),
					project.getNewOntologyManager())) {
				continue;
			}

			Map<Value, List<Statement>> statementsByObject = pendingDirectKnowledge
					.filter(resource, predicate, null).stream().collect(groupingBy(Statement::getObject));

			for (Map.Entry<Value, List<Statement>> entry : statementsByObject.entrySet()) {
				Value object = entry.getKey();
				Set<Resource> graphs = entry.getValue().stream().map(Statement::getContext).collect(toSet());

				AnnotatedValue<?> annotatedObject = null;

				if (object instanceof Resource) {
					if (collectionBehavior == CollectionBehavior.ALWAYS_ASSUME_COLLECTION) {
						AnnotatedResourceWithMembers<Resource, Value> annotatedObjectWithMembers = new AnnotatedResourceWithMembers<>(
								(Resource) object);
						annotatedObject = annotatedObjectWithMembers;

						Set<Resource> alreadExpandedCollections = new HashSet<>();
						Queue<NodeContext> frontier = new LinkedList<>();
						frontier.add(new NodeContext((Resource) object, graphs));

						while (!frontier.isEmpty()) {
							NodeContext topContext = frontier.poll();
							Resource top = topContext.getResource();
							Set<Resource> cumulativeGraphs = topContext.getCumulativeGraphs();

							if (RDF.Res.NIL.equals(top))
								continue;

							if (alreadExpandedCollections.contains(top))
								continue;

							alreadExpandedCollections.add(top);

							Map<Value, Set<Resource>> firstElement2graphs = statements
									.filter(top, org.eclipse.rdf4j.model.vocabulary.RDF.FIRST, null).stream()
									.collect(groupingBy(Statement::getObject,
											mapping(Statement::getContext, toSet())));

							for (Entry<Value, Set<Resource>> firstElementAndGraphs : firstElement2graphs
									.entrySet()) {
								Value firstElement = firstElementAndGraphs.getKey();

								Set<Resource> graphs1 = new HashSet<>();
								graphs1.addAll(firstElementAndGraphs.getValue());
								graphs1.addAll(cumulativeGraphs);

								AnnotatedValue<? extends Value> annotatedMember = new AnnotatedValue<Value>(
										firstElement);

								if (firstElement instanceof Resource) {
									addRole((AnnotatedValue<Resource>) annotatedMember, resource2attributes);
									addShowOrRenderXLabel((AnnotatedValue<Resource>) annotatedMember,
											resource2attributes, statements);
								}
								annotatedMember.setAttribute("graphs", computeGraphs(graphs1));
								annotatedMember.setAttribute("index", topContext.getIndex() + 1);
								annotatedObjectWithMembers.getMembers()
										.add((AnnotatedValue<Value>) annotatedMember);
							}

							Map<Value, Set<Resource>> nextCollection2graphs = statements
									.filter(top, org.eclipse.rdf4j.model.vocabulary.RDF.REST, null).stream()
									.collect(groupingBy(Statement::getObject,
											mapping(Statement::getContext, toSet())));
							boolean requireCloning = nextCollection2graphs.size() > 1;

							for (Entry<Value, Set<Resource>> nextCollectionAndGraphs : nextCollection2graphs
									.entrySet()) {
								Value nextCollection = nextCollectionAndGraphs.getKey();

								if (!(nextCollection instanceof Resource))
									continue;

								frontier.add(topContext.nextContext((Resource) nextCollection,
										nextCollectionAndGraphs.getValue(), requireCloning));
							}
						}
					} else {
						annotatedObject = new AnnotatedValue<>(object);
					}
					addRole((AnnotatedValue<Resource>) annotatedObject, resource2attributes);
					addShowOrRenderXLabel((AnnotatedValue<Resource>) annotatedObject, resource2attributes,
							statements);
				}

				if (annotatedObject == null) {
					annotatedObject = new AnnotatedValue<>(object);
				}

				annotatedObject.setAttribute("explicit", currentProject && graphs.contains(workingGraph));

				annotatedObject.setAttribute("graphs", computeGraphs(graphs));

				valueMultiMap.put(predicate, annotatedObject);

				processedStatements.addAll(entry.getValue());
			}
			
			if (valueMultiMap.isEmpty() && !shouldRetainEmptyGroup(predicate, resource, resourcePosition)) {
				continue; // Skip irrelevant empty outer group
			}
			
			AnnotatedValue<IRI> annotatedPredicate = new AnnotatedValue<IRI>(predicate);

			annotatedPredicate.setAttribute("role", RDFResourceRolesEnum.property.toString());
			addRole(annotatedPredicate, resource2attributes);
			addShowOrRenderXLabel(annotatedPredicate, resource2attributes, statements);
			annotatedPredicate.setAttribute("hasCustomRange",
					customRangeProvider.existsCustomRangeEntryGraphForProperty(predicate.stringValue()));

			propMap.put(predicate, annotatedPredicate);

		}

		PredicateObjectsList predicateObjectsList = new PredicateObjectsList(propMap, valueMultiMap);
		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();
		rv.put(sectionName, new PredicateObjectsListSection(predicateObjectsList));

		return rv;
	}

	protected boolean shouldRetainEmptyGroup(IRI prop, Resource resource, ResourcePosition resourcePosition) {
		return false;
	}
}

class NodeContext {
	private Resource resource;
	private Set<Resource> cumulativeGraphs;
	private int index;

	public NodeContext(Resource resource, Set<Resource> cumulativeGraphs) {
		this(resource, cumulativeGraphs, 0);
	}

	public NodeContext(Resource resource, Set<Resource> cumulativeGraphs, int index) {
		this.resource = resource;
		this.cumulativeGraphs = new HashSet<>(cumulativeGraphs);
		this.index = index;
	}

	public NodeContext nextContext(Resource resource, Set<Resource> newGraphs, boolean requireCloning) {
		NodeContext nc = this;

		if (requireCloning) {
			nc = new NodeContext(resource, new HashSet<>(cumulativeGraphs), index);
		}

		nc.index++;
		nc.resource = resource;
		nc.cumulativeGraphs.addAll(newGraphs);

		return nc;
	}

	public int getIndex() {
		return index;
	}

	public Resource getResource() {
		return resource;
	}

	public Set<Resource> getCumulativeGraphs() {
		return cumulativeGraphs;
	}
}
