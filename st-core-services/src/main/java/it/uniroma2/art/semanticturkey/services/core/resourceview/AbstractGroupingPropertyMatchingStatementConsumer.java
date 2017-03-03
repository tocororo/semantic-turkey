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
import java.util.stream.Collectors;

import javax.print.attribute.standard.JobOriginatingUserName;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.owlart.rdf4jimpl.model.ARTURIResourceRDF4JImpl;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.vocabulary.STVocabUtilities;

public class AbstractGroupingPropertyMatchingStatementConsumer extends AbstractStatementConsumer {

	private CustomFormManager customFormManager;
	private String sectionName;
	private Set<IRI> matchedProperties;

	public AbstractGroupingPropertyMatchingStatementConsumer(CustomFormManager customFormManager,
			String sectionName, Set<IRI> matchedProperties) {
		this.customFormManager = customFormManager;
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
			Map<Resource, Map<String, Value>> resource2attributes,
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, Model propertyModel) {

		boolean currentProject = false;
		if (resourcePosition instanceof LocalResourcePosition) {
			currentProject = ((LocalResourcePosition) resourcePosition).getProject().equals(project);
		}

		Set<Statement> newlyProcessedStatements = new HashSet<>();

		Model pendingStatements = new LinkedHashModel(statements);
		pendingStatements.removeAll(processedStatements);
		IRI lastPredicate = null;

		Map<IRI, AnnotatedValue<IRI>> propMap = new HashMap<>();
		Multimap<IRI, AnnotatedValue<?>> valueMultiMap = HashMultimap.create();
		
		PredicateObjectsList predObjsList = new PredicateObjectsList(propMap, valueMultiMap);
		
		for (IRI predicate : pendingStatements.predicates()) {			
			if (STVocabUtilities.isHiddenResource(new ARTURIResourceRDF4JImpl(predicate),
					project.getNewOntologyManager())) {
				continue;
			}
		
			Set<IRI> rootProperties = matchedProperties.stream().filter(p->propertyModel.contains(predicate, RDFS.SUBPROPERTYOF, p)).collect(toSet());
				
			if (rootProperties.isEmpty()) continue;
				
			AnnotatedValue<IRI> annotatedPredicate = new AnnotatedValue<IRI>(predicate);

			annotatedPredicate.setAttribute("role", RDFResourceRolesEnum.property.toString());
			addRole(annotatedPredicate, resource2attributes);
			addShowOrRenderXLabelOrCRE(annotatedPredicate, resource2attributes, predicate2resourceCreShow,
					null, statements);
			annotatedPredicate.setAttribute("hasCustomRange",
					customFormManager.existsCustomFormGraphForResource(predicate.stringValue()));
			annotatedPredicate.setAttribute("rootProperties",
					rootProperties.stream().map(Value::stringValue).collect(Collectors.joining(",")));
			propMap.put(predicate, annotatedPredicate);
			
			
			newlyProcessedStatements.addAll(pendingStatements.filter(resource, predicate, null));
			
			Map<Value, List<Statement>> statementsByObject = pendingStatements
					.filter(resource, predicate, null).stream().collect(groupingBy(Statement::getObject));
			
			for (Map.Entry<Value, List<Statement>> entry : statementsByObject.entrySet()) {
				Value object = entry.getKey();
				Set<Resource> graphs = entry.getValue().stream().map(Statement::getContext)
						.collect(toSet());

				AnnotatedValue<?> annotatedObject = new AnnotatedValue<Value>(object);
				annotatedObject.setAttribute("explicit", currentProject && graphs.contains(workingGraph));

				if (object instanceof Resource) {
					addRole((AnnotatedValue<Resource>) annotatedObject, resource2attributes);
					addShowOrRenderXLabelOrCRE((AnnotatedValue<Resource>) annotatedObject,
							resource2attributes, predicate2resourceCreShow, predicate, statements);
				}
				annotatedObject.setAttribute("graphs", computeGraphs(graphs));

				valueMultiMap.put(predicate, annotatedObject);
			}			
		}

		processedStatements.addAll(newlyProcessedStatements);
		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();
		rv.put(sectionName, new PredicateObjectsListSection(predObjsList));

		return rv;
	}
}
