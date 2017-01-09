package it.uniroma2.art.semanticturkey.services.core.resourceview;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

import it.uniroma2.art.owlart.rdf4jimpl.model.ARTURIResourceRDF4JImpl;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.vocabulary.STVocabUtilities;

public class AbstractGroupingPropertyMatchingStatementConsumer extends AbstractStatementConsumer {

	private CustomRangeProvider customRangeProvider;
	private String sectionName;
	private Set<IRI> matchedProperties;

	public AbstractGroupingPropertyMatchingStatementConsumer(CustomRangeProvider customRangeProvider,
			String sectionName, Set<IRI> matchedProperties) {
		this.customRangeProvider = customRangeProvider;
		this.sectionName = sectionName;
		this.matchedProperties = matchedProperties;
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

		Set<Statement> newlyProcessedStatements = new HashSet<>();

		Map<IRI, AnnotatedValue<IRI>> outerPropMap = new LinkedHashMap<>();
		Map<IRI, PredicateObjectsList> outerValueMultiMap = new HashMap<>();

		for (IRI superProp : matchedProperties) {
			Map<IRI, AnnotatedValue<IRI>> propMap = new HashMap<>();
			Multimap<IRI, AnnotatedValue<?>> valueMultiMap = HashMultimap.create();

			LinkedHashModel relevantDirectKnowledge = new LinkedHashModel(
					statements.filter(resource, null, null).stream()
							.filter(stmt -> !processedStatements.contains(stmt)
									&& (stmt.getPredicate().equals(superProp) || propertyModel
											.contains(stmt.getPredicate(), RDFS.SUBPROPERTYOF, superProp)))
					.collect(toList()));
			newlyProcessedStatements.addAll(relevantDirectKnowledge);
			
			for (IRI predicate : relevantDirectKnowledge.predicates()) {
				if (STVocabUtilities.isHiddenResource(new ARTURIResourceRDF4JImpl(predicate),
						project.getOntologyManager())) {
					continue;
				}

				AnnotatedValue<IRI> annotatedPredicate = new AnnotatedValue<IRI>(predicate);

				annotatedPredicate.setAttribute("role", RDFResourceRolesEnum.property.toString());
				addRole(annotatedPredicate, resource2attributes);
				addShowOrRenderXLabel(annotatedPredicate, resource2attributes, statements);
				annotatedPredicate.setAttribute("hasCustomRange",
						customRangeProvider.existsCustomRangeEntryGraphForProperty(predicate.stringValue()));

				propMap.put(predicate, annotatedPredicate);

				Map<Value, List<Statement>> statementsByObject = relevantDirectKnowledge
						.filter(resource, predicate, null).stream().collect(groupingBy(Statement::getObject));

				for (Map.Entry<Value, List<Statement>> entry : statementsByObject.entrySet()) {
					Value object = entry.getKey();
					Set<Resource> graphs = entry.getValue().stream().map(Statement::getContext).collect(toSet());

					AnnotatedValue<?> annotatedObject = new AnnotatedValue<Value>(object);
					annotatedObject.setAttribute("explicit", currentProject && graphs.contains(workingGraph));

					if (object instanceof Resource) {
						addRole((AnnotatedValue<Resource>) annotatedObject, resource2attributes);
						addShowOrRenderXLabel((AnnotatedValue<Resource>) annotatedObject, resource2attributes,
								statements);
					}
					annotatedObject.setAttribute("graphs", computeGraphs(graphs));

					valueMultiMap.put(predicate, annotatedObject);
				}
			}
			
			if (valueMultiMap.isEmpty() && !shouldRetainEmptyOuterGroup(superProp, resource, resourcePosition)) {
				continue; // Skip irrelevant empty outer group
			}
			
			PredicateObjectsList predObjsList = new PredicateObjectsList(propMap, valueMultiMap);
			
			AnnotatedValue<IRI> annotatedSuperProp = new AnnotatedValue<IRI>(superProp);

			annotatedSuperProp.setAttribute("role", RDFResourceRolesEnum.property.toString());
			addRole(annotatedSuperProp, resource2attributes);
			addShowOrRenderXLabel(annotatedSuperProp, resource2attributes, statements);
			annotatedSuperProp.setAttribute("hasCustomRange",
					customRangeProvider.existsCustomRangeEntryGraphForProperty(superProp.stringValue()));

			outerPropMap.put(superProp, annotatedSuperProp);
			outerValueMultiMap.put(superProp, predObjsList);
		}

		processedStatements.addAll(newlyProcessedStatements);

		PredicateValueList<PredicateObjectsList> outerPredicateValueList = new PredicateValueList<>(outerPropMap, outerValueMultiMap);
		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();
		rv.put(sectionName, new PredicateValueListSection<PredicateObjectsList>(outerPredicateValueList));

		return rv;
	}

	protected boolean shouldRetainEmptyOuterGroup(IRI superProp, Resource resource,
			ResourcePosition resourcePosition) {
		return true;
	}
}
