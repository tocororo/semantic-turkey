package it.uniroma2.art.semanticturkey.services.core.resourceview;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractPropertyMatchingStatementConsumer.BehaviorOptions.RenderingEngineBehavior;
import it.uniroma2.art.semanticturkey.vocabulary.STVocabUtilities;

public class AbstractPropertyMatchingStatementConsumer extends AbstractStatementConsumer {

	public static class BehaviorOptions {

		public enum CollectionBehavior {
			IGNORE, ALWAYS_ASSUME_COLLECTION
		}

		public enum RootPropertiesBehavior {
			SHOW, SHOW_IF_INFORMATIVE, HIDE
		}

		public enum SubpropertiesBehavior {
			INCLUDE, EXCLUDE
		}

		public enum RenderingEngineBehavior {
			INCLUDE, EXCLUDE
		}

		private RootPropertiesBehavior rootPropertiesBehavior;
		private CollectionBehavior collectionBehavior;
		private SubpropertiesBehavior subpropertiesBehavior;
		private RenderingEngineBehavior renderingEngineBehavior;

		public BehaviorOptions(RootPropertiesBehavior rootPropertiesBehavior,
				CollectionBehavior collectionBehavior, SubpropertiesBehavior subpropertiesBehavior,
				RenderingEngineBehavior renderingEngineBehavior) {
			this.rootPropertiesBehavior = rootPropertiesBehavior;
			this.collectionBehavior = collectionBehavior;
			this.subpropertiesBehavior = subpropertiesBehavior;
			this.renderingEngineBehavior = renderingEngineBehavior;
		}

		public BehaviorOptions() {
			this(RootPropertiesBehavior.HIDE, BehaviorOptions.CollectionBehavior.IGNORE,
					SubpropertiesBehavior.INCLUDE, RenderingEngineBehavior.INCLUDE);
		}

		public BehaviorOptions setRootPropertiesBehavior(RootPropertiesBehavior rootPropertiesBehavior) {
			this.rootPropertiesBehavior = rootPropertiesBehavior;
			return this;
		}

		public RootPropertiesBehavior getRootPropertiesBehavior() {
			return rootPropertiesBehavior;
		}

		public BehaviorOptions setCollectionBehavior(CollectionBehavior collectionBehavior) {
			this.collectionBehavior = collectionBehavior;
			return this;
		}

		public CollectionBehavior getCollectionBehavior() {
			return collectionBehavior;
		}

		public BehaviorOptions setSubpropertiesBehavior(SubpropertiesBehavior subpropertiesBehavior) {
			this.subpropertiesBehavior = subpropertiesBehavior;
			return this;
		}

		public SubpropertiesBehavior getSubpropertiesBehavior() {
			return subpropertiesBehavior;
		}

		public BehaviorOptions setRenderingEngineBehavior(RenderingEngineBehavior renderingEngineBehavior) {
			this.renderingEngineBehavior = renderingEngineBehavior;
			return this;
		}

		public RenderingEngineBehavior getRenderingEngineBehavior() {
			return renderingEngineBehavior;
		}
	}

	private CustomFormManager customFormManager;
	private String sectionName;
	private Set<IRI> matchedProperties;
	private BehaviorOptions behaviorOptions;

	public AbstractPropertyMatchingStatementConsumer(CustomFormManager customFormManager, String sectionName,
			Set<IRI> matchedProperties, BehaviorOptions behaviorOptions) {
		this.customFormManager = customFormManager;
		this.sectionName = sectionName;
		this.matchedProperties = matchedProperties;
		this.behaviorOptions = behaviorOptions;
	}

	public AbstractPropertyMatchingStatementConsumer(CustomFormManager customFormManager, String sectionName,
			Set<IRI> matchedProperties) {
		this(customFormManager, sectionName, matchedProperties, new BehaviorOptions());
	}

	public String getSectionName() {
		return this.sectionName;
	}

	@Override
	public Set<IRI> getMatchedProperties() {
		return matchedProperties;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, ResourceViewSection> consumeStatements(Project project, RepositoryConnection repoConn,
			ResourcePosition resourcePosition, Resource resource, Model statements,
			Set<Statement> processedStatements, Resource workingGraph,
			Map<Resource, Map<String, Value>> resource2attributes,
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, Model propertyModel) {

		boolean currentProject = false;
		if (resourcePosition instanceof LocalResourcePosition) {
			currentProject = ((LocalResourcePosition) resourcePosition).getProject().equals(project);
		}

		Map<IRI, AnnotatedValue<IRI>> propMap = new LinkedHashMap<>();
		Multimap<IRI, AnnotatedValue<?>> valueMultiMap = LinkedHashMultimap.create();

		Collection<IRI> relevantProperties;
		LinkedHashModel pendingDirectKnowledge;

		if (matchedProperties.isEmpty()) {
			pendingDirectKnowledge = new LinkedHashModel(statements.filter(resource, null, null).stream()
					.filter(stmt -> !processedStatements.contains(stmt)).collect(toList()));
			relevantProperties = pendingDirectKnowledge.predicates();
		} else {
			if (behaviorOptions.getSubpropertiesBehavior() == BehaviorOptions.SubpropertiesBehavior.EXCLUDE) {
				relevantProperties = matchedProperties;
			} else {
				relevantProperties = computeRelavantProperties(propertyModel, statements,
						processedStatements);
			}
			pendingDirectKnowledge = new LinkedHashModel(statements.filter(resource, null, null).stream()
					.filter(stmt -> !processedStatements.contains(stmt)
							&& relevantProperties.contains(stmt.getPredicate()))
					.collect(toList()));
		}

		for (IRI predicate : relevantProperties) {
			if (STVocabUtilities.isHiddenResource(predicate, project.getOntologyManager())) {
				continue;
			}

			Map<Value, List<Statement>> statementsByObject = pendingDirectKnowledge
					.filter(resource, predicate, null).stream().collect(groupingBy(Statement::getObject));

			List<AnnotatedValue<?>> predicateValues = new ArrayList<AnnotatedValue<?>>(
					statementsByObject.keySet().size());

			for (Map.Entry<Value, List<Statement>> entry : statementsByObject.entrySet()) {
				Value object = entry.getKey();
				Set<Resource> graphs = entry.getValue().stream().map(Statement::getContext).collect(toSet());

				AnnotatedValue<?> annotatedObject = null;

				if (object instanceof Resource) {
					if (behaviorOptions
							.getCollectionBehavior() == BehaviorOptions.CollectionBehavior.ALWAYS_ASSUME_COLLECTION) {
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

							if (RDF.NIL.equals(top))
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
									addNature((AnnotatedValue<Resource>) annotatedMember,
											resource2attributes);
									addShowViaDedicatedOrGenericRendering(
											(AnnotatedValue<Resource>) annotatedMember, resource2attributes,
											predicate2resourceCreShow, null, statements, behaviorOptions
													.getRenderingEngineBehavior() == RenderingEngineBehavior.INCLUDE);
									addQName((AnnotatedValue<Resource>) annotatedMember, resource2attributes);
								}
								annotatedMember.setAttribute("graphs", computeGraphs(graphs1));
								annotatedMember.setAttribute("tripleScope",
										computeTripleScope(graphs1, workingGraph).toString());
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
					addNature((AnnotatedValue<Resource>) annotatedObject, resource2attributes);
					addShowViaDedicatedOrGenericRendering((AnnotatedValue<Resource>) annotatedObject,
							resource2attributes, predicate2resourceCreShow, predicate, statements,
							behaviorOptions.getRenderingEngineBehavior() == RenderingEngineBehavior.INCLUDE);
					addQName((AnnotatedValue<Resource>) annotatedObject, resource2attributes);
				}

				if (annotatedObject == null) {
					annotatedObject = new AnnotatedValue<>(object);
				}

				annotatedObject.setAttribute("explicit", currentProject && graphs.contains(workingGraph));

				annotatedObject.setAttribute("graphs", computeGraphs(graphs));
				annotatedObject.setAttribute("tripleScope",
						computeTripleScope(graphs, workingGraph).toString());

				predicateValues.add(annotatedObject);

				processedStatements.addAll(entry.getValue());
			}

			sortPredicateValues(predicateValues, statements, resource);

			valueMultiMap.putAll(predicate, predicateValues);

			if (!valueMultiMap.containsKey(predicate)
					&& !shouldRetainEmptyGroup(predicate, resource, resourcePosition)) {
				continue; // Skip irrelevant empty outer group
			}

			AnnotatedValue<IRI> annotatedPredicate = new AnnotatedValue<IRI>(predicate);

			addNature(annotatedPredicate, resource2attributes);
			if ("".equals(annotatedPredicate.getAttributes().get("nature"))) {
				annotatedPredicate.setAttribute("role", RDFResourceRole.property.toString());
			}
			addShowViaDedicatedOrGenericRendering(annotatedPredicate, resource2attributes,
					predicate2resourceCreShow, null, statements, true);
			annotatedPredicate.setAttribute("hasCustomRange",
					customFormManager.existsCustomFormGraphForResource(project, predicate));

			if (behaviorOptions.getRootPropertiesBehavior() == BehaviorOptions.RootPropertiesBehavior.SHOW
					|| (behaviorOptions
							.getRootPropertiesBehavior() == BehaviorOptions.RootPropertiesBehavior.SHOW_IF_INFORMATIVE
							&& matchedProperties.size() > 1)) {
				Set<IRI> rootProperties = matchedProperties.stream()
						.filter(p -> propertyModel.contains(predicate, RDFS.SUBPROPERTYOF, p))
						.collect(toSet());
				annotatedPredicate.setAttribute("rootProperties",
						rootProperties.stream().map(Value::stringValue).collect(Collectors.joining(",")));
			}

			propMap.put(predicate, annotatedPredicate);

		}

		PredicateObjectsList predicateObjectsList = new PredicateObjectsList(propMap, valueMultiMap);
		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();

		RDFResourceRole resourceRole = STServiceAdapter.getRoleFromNature(java.util.Optional
				.ofNullable(resource2attributes.getOrDefault(resource, Collections.emptyMap()).get("nature"))
				.map(Value::stringValue).orElse(""));

		if (!valueMultiMap.isEmpty() || shouldRetainEmptyResult(sectionName, resourceRole)) {
			rv.put(sectionName, new PredicateObjectsListSection(predicateObjectsList));
		}

		return rv;
	}

	protected List<IRI> computeRelavantProperties(Model propertyModel, Model statements,
			Set<Statement> processedStatements) {
		return Stream
				.concat(matchedProperties.stream()
						.flatMap(prop -> propertyModel.filter(null, RDFS.SUBPROPERTYOF, prop).stream())
						.map(stmt -> (IRI) stmt.getSubject()), matchedProperties.stream())
				.distinct().collect(toList());
	}

	protected void sortPredicateValues(List<AnnotatedValue<?>> values, Model statements,
			Resource subjectResource) {
		// do nothing
	}

	protected boolean shouldRetainEmptyGroup(IRI prop, Resource resource, ResourcePosition resourcePosition) {
		return false;
	}

	protected boolean shouldRetainEmptyResult(String sectionName, RDFResourceRole resourceRole) {
		return true;
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
